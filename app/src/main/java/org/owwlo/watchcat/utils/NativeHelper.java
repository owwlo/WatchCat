package org.owwlo.watchcat.utils;

public class NativeHelper {

    static {
        System.loadLibrary("native-nv21Utils");
    }

    public static native boolean nv21ToYuv420(byte[] buffer, byte[] data, boolean isPlanar, int sliceHeight, int height,
                                              int stride, int width, boolean panesReversed, int size, int yPadding);

    public static native void nv21Flip(byte[] data, int imageWidth, int imageHeight);
}
