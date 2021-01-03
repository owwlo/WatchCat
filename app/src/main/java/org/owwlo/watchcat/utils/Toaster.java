package org.owwlo.watchcat.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.owwlo.watchcat.BuildConfig;

import es.dmoral.toasty.Toasty;

public class Toaster {
    public static class debug {
        public static void info(Context context, final String msg) {
            if (!BuildConfig.DEBUG) return;
            Log.d(Toaster.class.getSimpleName(), "Toaster INFO: " + msg);
            Toasty.info(context, msg, Toast.LENGTH_LONG, false).show();
        }

        public static void warn(Context context, final String msg) {
            if (!BuildConfig.DEBUG) return;
            Log.d(Toaster.class.getSimpleName(), "Toaster WARN: " + msg);
            Toasty.warning(context, msg, Toast.LENGTH_LONG, false).show();
        }

        public static void error(Context context, final String msg) {
            if (!BuildConfig.DEBUG) return;
            Log.d(Toaster.class.getSimpleName(), "Toaster ERROR: " + msg);
            Toasty.error(context, msg, Toast.LENGTH_LONG, false).show();
        }
    }

    public static void info(Context context, final String msg) {
        Toasty.info(context, msg, Toast.LENGTH_LONG, false).show();
    }

    public static void warn(Context context, final String msg) {
        Toasty.warning(context, msg, Toast.LENGTH_LONG, false).show();
    }

    public static void error(Context context, final String msg) {
        Toasty.error(context, msg, Toast.LENGTH_LONG, false).show();
    }
}
