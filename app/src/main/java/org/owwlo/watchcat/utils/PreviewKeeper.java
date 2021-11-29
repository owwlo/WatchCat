package org.owwlo.watchcat.utils;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;

public class PreviewKeeper {
    private static class Holder {
        private static final PreviewKeeper instance = new PreviewKeeper();
    }

    private long lastPreviewTime = 0;
    private Camera.Parameters cameraParameters = null;
    private byte[] previewData = null;
    private boolean needToFlip = false;

    private PreviewKeeper() {
    }

    public boolean previewAvailable() {
        return lastPreviewTime != 0 && cameraParameters != null && previewData != null;
    }

    public byte[] getLastPreviewImageInBytes() {
        if (previewData == null) return null;

        byte[] workingBytes = previewData.clone();
        final int width = cameraParameters.getPreviewSize().width;
        final int height = cameraParameters.getPreviewSize().height;

        if (needToFlip) {
            NativeHelper.nv21Flip(workingBytes, width, height);
        }

        YuvImage yuv = new YuvImage(workingBytes, cameraParameters.getPreviewFormat(), width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 80, out);

        return out.toByteArray();
    }


    public synchronized void tryUpdatePreview(byte[] previewBytes, Camera camera, boolean flip, boolean forceToTake) {
        long now = System.currentTimeMillis();
        if (forceToTake || (now - lastPreviewTime > Constants.PREVIEW_UPDATE_INTERVAL_MS)) {
            previewData = previewBytes;
            cameraParameters = camera.getParameters();
            needToFlip = flip;
            lastPreviewTime = now;
        }
    }

    public long getLastPreviewTime() {
        return lastPreviewTime;
    }

    public static PreviewKeeper getInstance() {
        return Holder.instance;
    }
}
