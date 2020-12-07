package org.owwlo.watchcat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

// This class was from https://github.com/material-components/material-components-android project.
// Original class name was ImageRequester
public class NetworkImageLoader {
    private static Context ctx;
    private static NetworkImageLoader instance;
    private final RequestQueue requestQueue;
    private final ImageLoader imageLoader;
    private final int maxByteSize;

    private NetworkImageLoader(Context context) {
        ctx = context;
        this.requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        this.requestQueue.start();
        this.maxByteSize = calculateMaxByteSize();
        this.imageLoader =
                new ImageLoader(
                        requestQueue,
                        new ImageLoader.ImageCache() {
                            @Override
                            public Bitmap getBitmap(String url) {
                                return null;
                            }
                            @Override
                            public void putBitmap(String url, Bitmap bitmap) {
                            }
                        });
        instance = this;
    }

    private static int calculateMaxByteSize() {
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        final int screenBytes = displayMetrics.widthPixels * displayMetrics.heightPixels * 4;
        return screenBytes * 3;
    }

    public static synchronized NetworkImageLoader getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkImageLoader(context);
        }
        return instance;
    }

    public void setImageFromUrl(NetworkImageView networkImageView, String url) {
        networkImageView.setImageUrl(url, imageLoader);
    }
}
