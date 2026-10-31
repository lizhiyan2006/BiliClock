package com.biliclock.deskclock;

import android.content.Context;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Android 6 predates Let's Encrypt's ISRG roots, which Open-Meteo uses.
 * Combine the platform trust store with the two ISRG roots shipped in
 * res/raw so the default weather backend also works on API 23.
 */
public final class Tls {
    private static volatile boolean initialized;
    private static volatile SSLSocketFactory socketFactory;

    private Tls() {
    }

    public static SSLSocketFactory socketFactory(Context context) {
        if (!initialized) {
            synchronized (Tls.class) {
                if (!initialized) {
                    try {
                        socketFactory = build(context.getApplicationContext());
                    } catch (Throwable ignored) {
                        socketFactory = null;
                    }
                    initialized = true;
                }
            }
        }
        return socketFactory;
    }

    private static SSLSocketFactory build(Context context) throws Exception {
        X509TrustManager system = systemManager();
        X509TrustManager extra = customManager(context);
        CompositeTrustManager composite = new CompositeTrustManager(system, extra);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{composite}, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private static X509TrustManager systemManager() throws Exception {
        TrustManagerFactory factory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return firstX509(factory.getTrustManagers());
    }

    private static X509TrustManager customManager(Context context) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        loadCertificates(context, cf, keyStore, R.raw.isrg_root_x1, "isrg-x1");
        loadCertificates(context, cf, keyStore, R.raw.isrg_root_x2, "isrg-x2");
        // Bilibili's avatar CDN currently chains to GlobalSign Root R46, which
        // post-dates Android 6's system trust store.
        loadCertificates(context, cf, keyStore, R.raw.globalsign_root_r46, "globalsign-r46");
        TrustManagerFactory factory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        return firstX509(factory.getTrustManagers());
    }

    private static void loadCertificates(Context context, CertificateFactory cf,
                                         KeyStore keyStore, int resId, String prefix) throws Exception {
        try (InputStream in = context.getResources().openRawResource(resId)) {
            Collection<? extends Certificate> certificates = cf.generateCertificates(in);
            int index = 0;
            for (Certificate certificate : certificates) {
                keyStore.setCertificateEntry(prefix + "-" + index++, certificate);
            }
        }
    }

    private static X509TrustManager firstX509(TrustManager[] managers) {
        if (managers != null) {
            for (TrustManager manager : managers) {
                if (manager instanceof X509TrustManager) {
                    return (X509TrustManager) manager;
                }
            }
        }
        return null;
    }

    private static final class CompositeTrustManager implements X509TrustManager {
        private final X509TrustManager system;
        private final X509TrustManager extra;

        CompositeTrustManager(X509TrustManager system, X509TrustManager extra) {
            this.system = system;
            this.extra = extra;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            if (system != null) {
                system.checkClientTrusted(chain, authType);
            } else if (extra != null) {
                extra.checkClientTrusted(chain, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            if (system != null) {
                try {
                    system.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException ignored) {
                }
            }
            if (extra != null) {
                extra.checkServerTrusted(chain, authType);
                return;
            }
            throw new CertificateException("没有可用的证书校验器");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> result = new ArrayList<>();
            if (system != null) {
                for (X509Certificate cert : system.getAcceptedIssuers()) {
                    result.add(cert);
                }
            }
            if (extra != null) {
                for (X509Certificate cert : extra.getAcceptedIssuers()) {
                    result.add(cert);
                }
            }
            return result.toArray(new X509Certificate[0]);
        }
    }
}
