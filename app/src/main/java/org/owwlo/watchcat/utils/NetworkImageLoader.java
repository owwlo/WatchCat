package org.owwlo.watchcat.utils;

import android.content.Context;
import android.graphics.Bitmap;
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
                            private LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(maxByteSize) {
                                @Override
                                public int sizeOf(String url, Bitmap bitmap) {
                                    return bitmap.getByteCount();
                                }
                            };

                            @Override
                            public Bitmap getBitmap(String url) {
                                return lruCache.get(url);
                            }

                            @Override
                            public void putBitmap(String url, Bitmap bitmap) {
                                Toaster.debug.info("Loaded new image from: " + url);
                                lruCache.put(url, bitmap);
                            }
                        });
        instance = this;
    }

    private static int calculateMaxByteSize() {
        // TODO find a better place for 1920x1080
        final int screenBytes = 1920 * 1080 * 4;
        return screenBytes * 5;
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
