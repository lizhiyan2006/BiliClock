param(
    [string]$SdkPath = "",
    [string]$JdkPath = "",
    [string]$BuildToolsVersion = "35.0.0",
    [string]$Platform = "android-35"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$app = Join-Path $root "app"
$build = Join-Path $root "build"
$dist = Join-Path $root "dist"
$gen = Join-Path $build "gen"
$classes = Join-Path $build "classes"
$dex = Join-Path $build "dex"

if (-not $JdkPath) {
    $candidate = Join-Path $root ".toolchain\jdk\jdk-17.0.16+8"
    if (Test-Path $candidate) { $JdkPath = $candidate }
    elseif ($env:JAVA_HOME) { $JdkPath = $env:JAVA_HOME }
}
if (-not $SdkPath) {
    if ($env:ANDROID_HOME) { $SdkPath = $env:ANDROID_HOME }
    elseif ($env:ANDROID_SDK_ROOT) { $SdkPath = $env:ANDROID_SDK_ROOT }
    else { $SdkPath = Join-Path $root ".toolchain\sdk" }
}

$java = Join-Path $JdkPath "bin\java.exe"
$javac = Join-Path $JdkPath "bin\javac.exe"
$jar = Join-Path $JdkPath "bin\jar.exe"
$keytool = Join-Path $JdkPath "bin\keytool.exe"
$buildTools = Join-Path $SdkPath "build-tools\$BuildToolsVersion"
$aapt2 = Join-Path $buildTools "aapt2.exe"
$d8 = Join-Path $buildTools "d8.bat"
$zipalign = Join-Path $buildTools "zipalign.exe"
$apksigner = Join-Path $buildTools "apksigner.bat"
$androidJar = Join-Path $SdkPath "platforms\$Platform\android.jar"

foreach ($required in @($java, $javac, $jar, $keytool, $aapt2, $d8, $zipalign, $apksigner, $androidJar)) {
    if (-not (Test-Path $required)) {
        throw "Missing build dependency: $required`nInstall Android SDK build-tools $BuildToolsVersion and platform $Platform."
    }
}

$env:JAVA_HOME = $JdkPath
$env:PATH = (Join-Path $JdkPath "bin") + ";" + $buildTools + ";" + $env:PATH

if (Test-Path $build) { Remove-Item -Recurse -Force $build }
if (-not (Test-Path $dist)) { New-Item -ItemType Directory -Path $dist | Out-Null }
New-Item -ItemType Directory -Force -Path $gen, $classes, $dex | Out-Null

Write-Host "==> Compiling resources" -ForegroundColor Cyan
$compiled = Join-Path $build "resources.zip"
& $aapt2 compile --dir (Join-Path $app "src\main\res") -o $compiled
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

# aapt2 invoked outside Gradle still needs the legacy package attribute. The
# checked-in manifest deliberately uses a Gradle namespace instead, so inject
# the package into a generated copy for this command-line build.
$manifestOut = Join-Path $build "AndroidManifest.xml"
$manifestText = Get-Content -Raw -Encoding UTF8 (Join-Path $app "src\main\AndroidManifest.xml")
$manifestText = $manifestText -replace '<manifest\s+xmlns:android', '<manifest package="com.biliclock.deskclock" xmlns:android'
[System.IO.File]::WriteAllText($manifestOut, $manifestText, (New-Object System.Text.UTF8Encoding($false)))

$unsigned = Join-Path $build "unsigned.apk"
& $aapt2 link -o $unsigned `
    -I $androidJar `
    --manifest $manifestOut `
    -A (Join-Path $app "src\main\assets") `
    -R $compiled `
    --java $gen `
    --min-sdk-version 23 `
    --target-sdk-version 34 `
    --version-code 5 `
    --version-name "1.3.1" `
    --auto-add-overlay `
    -0 ttf
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "==> Compiling Java" -ForegroundColor Cyan
$sources = Get-ChildItem -Path (Join-Path $app "src\main\java"), $gen -Recurse -Filter *.java |
    ForEach-Object { $_.FullName }
$sourceList = Join-Path $build "sources.txt"
[System.IO.File]::WriteAllLines($sourceList, [string[]]$sources, (New-Object System.Text.UTF8Encoding($false)))
& $javac -nowarn -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $androidJar `
    -d $classes "@$sourceList"
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "==> Dexing" -ForegroundColor Cyan
$classFiles = Get-ChildItem -Path $classes -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& $d8 --release --min-api 23 --lib $androidJar --output $dex @classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "==> Packaging" -ForegroundColor Cyan
& $jar uf $unsigned -C $dex "classes.dex"
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

$aligned = Join-Path $build "aligned.apk"
& $zipalign -f -p 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

$keystore = Join-Path $root "keystore\debug.keystore"
if (-not (Test-Path $keystore)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $keystore) | Out-Null
    & $keytool -genkeypair -v -keystore $keystore -storepass android -alias androiddebugkey `
        -keypass android -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}

$output = Join-Path $dist "BiliClock-1.3.1-debug.apk"
& $apksigner sign --ks $keystore --ks-pass pass:android --key-pass pass:android `
    --out $output $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }
& $apksigner verify --verbose $output
if ($LASTEXITCODE -ne 0) { throw "apksigner verify failed" }

Write-Host "Built: $output" -ForegroundColor Green
Get-Item $output | Select-Object FullName, Length, LastWriteTime
exit 0

