package com.biliclock.deskclock;

import android.app.Application;
import android.content.Context;

/** Holds the application context for the tiny networking helper. */
public final class App extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        // Parsing two root certificates is cheap, but do it before the first fetch
        // so API 23 can validate Open-Meteo's Let's Encrypt chain.
        Tls.socketFactory(this);
    }

    public static Context context() {
        return context;
    }
}
