package org.owwlo.watchcat;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import org.greenrobot.eventbus.EventBus;

public class WatchCatApp extends Application {
    public static Context sContext = null;
    public static Handler handler = new Handler();

    public static void postOnMainThread(Runnable r) {
        handler.post(r);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        EventBus.builder()
                .throwSubscriberException(BuildConfig.DEBUG)
                .installDefaultEventBus();
    }

}
