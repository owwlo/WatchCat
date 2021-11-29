package org.owwlo.watchcat.utils;

import android.content.Context;
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

    public static void info(Context context, final String msg) {
        WatchCatApp.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toasty.info(context, msg, Toast.LENGTH_SHORT, false).show();
            }
        });
    }

    public static void warn(Context context, final String msg) {
        WatchCatApp.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toasty.warning(context, msg, Toast.LENGTH_SHORT, false).show();
            }
        });
    }

    public static void error(Context context, final String msg) {
        WatchCatApp.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toasty.error(context, msg, Toast.LENGTH_SHORT, false).show();
            }
        });
    }
}
