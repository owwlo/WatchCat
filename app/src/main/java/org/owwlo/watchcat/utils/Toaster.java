package org.owwlo.watchcat.utils;

import android.util.Log;
import android.widget.Toast;

import org.owwlo.watchcat.BuildConfig;
import org.owwlo.watchcat.WatchCatApp;

import es.dmoral.toasty.Toasty;

public class Toaster {
    public static class debug {
        public static void info(final String msg) {
            if (!BuildConfig.DEBUG) return;
            Log.d(Toaster.class.getSimpleName(), "Toaster INFO: " + msg);
            WatchCatApp.postOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(WatchCatApp.sContext, msg, Toast.LENGTH_SHORT, false).show();
                }
            });
        }

        public static void warn(final String msg) {
            if (!BuildConfig.DEBUG) return;
            Log.d(Toaster.class.getSimpleName(), "Toaster WARN: " + msg);
            WatchCatApp.postOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Toasty.warning(WatchCatApp.sContext, msg, Toast.LENGTH_SHORT, false).show();
                }
            });
        }

        public static void error(final String msg) {
            if (!BuildConfig.DEBUG) return;
            Log.d(Toaster.class.getSimpleName(), "Toaster ERROR: " + msg);
            WatchCatApp.postOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Toasty.error(WatchCatApp.sContext, msg, Toast.LENGTH_SHORT, false).show();
                }
            });
        }
    }

    public static void info(final String msg) {
        WatchCatApp.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toasty.info(WatchCatApp.sContext, msg, Toast.LENGTH_SHORT, false).show();
            }
        });
    }

    public static void warn(final String msg) {
        WatchCatApp.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toasty.warning(WatchCatApp.sContext, msg, Toast.LENGTH_SHORT, false).show();
            }
        });
    }

    public static void error(final String msg) {
        WatchCatApp.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toasty.error(WatchCatApp.sContext, msg, Toast.LENGTH_SHORT, false).show();
            }
        });
    }
}
