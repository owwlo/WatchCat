package org.owwlo.watchcat;

import android.app.Application;

import org.greenrobot.eventbus.EventBus;

public class WatchCatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.builder()
                .throwSubscriberException(BuildConfig.DEBUG)
                .installDefaultEventBus();
    }

}
