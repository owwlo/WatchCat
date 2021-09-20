package org.owwlo.watchcat.utils;

import android.content.Context;

public class SharedPreferences {
    private static final String KEY_FIRST_STREAMING = "first_streaming";

    private android.content.SharedPreferences sp;

    public SharedPreferences(Context ctx) {
        sp = ctx.getSharedPreferences(
                ctx.getPackageName(), Context.MODE_PRIVATE);
    }

    public boolean getFirstStreaming() {
        return sp.getBoolean(KEY_FIRST_STREAMING, true);
    }

    public void setFirstStreaming(boolean b) {
        android.content.SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_FIRST_STREAMING, b);
        editor.commit();
    }
}
