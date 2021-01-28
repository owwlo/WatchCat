package org.owwlo.watchcat.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.owwlo.watchcat.libstreaming.RtspServer;
import org.owwlo.watchcat.libstreaming.Session;
import org.owwlo.watchcat.libstreaming.SessionBuilder;
import org.owwlo.watchcat.libstreaming.gl.SurfaceView;
import org.owwlo.watchcat.libstreaming.video.VideoQuality;
import org.owwlo.watchcat.utils.Constants;
import org.owwlo.watchcat.utils.Toaster;
import org.owwlo.watchcat.utils.Utils;

public class CameraDaemon extends Service implements Session.Callback {
    private final static String TAG = CameraDaemon.class.getCanonicalName();

    private static ServiceDaemon.RUNNING_MODE mMode = ServiceDaemon.RUNNING_MODE.STANDING_BY;

    private static CameraDaemon sInstance = null;
    private Session mCurrentSession = null;
    private String mAccessPassword = "";
    private int mStreamingPort = Constants.DEFAULT_STREAMING_PORT;
    private static byte[] previewData = new byte[0];
    private long previewTimestamp = 0;

    // Binding starts
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public CameraDaemon getService() {
            return CameraDaemon.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    // Binding ends

    public static CameraDaemon getInstance() {
        return sInstance;
    }


    public CameraDaemon() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO dynamically select 720p or 1080p
//        MediaFormat mMediaFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
//        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
//        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
//        MediaCodec mMediaCodec = null;
//        try {
//            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mStreamingPort = Utils.getAvailablePort(Constants.DEFAULT_STREAMING_PORT);

        Toaster.debug.info(this, "CameraDaemon started: " + Utils.getLocalIPAddress() + ":" + mStreamingPort);

        // TODO add audio support
        SessionBuilder.getInstance()
                .setCallback(this)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(1920, 1080, 30, 1500000));
        sInstance = this;
    }

    public void startPreviewing(SurfaceView surfaceView, boolean flip) {
        stopPreviewing();
        mCurrentSession = SessionBuilder.getInstance().setSurfaceView(surfaceView).setPreviewFlip(flip).build();
        mCurrentSession.startPreview();
    }

    public void stopPreviewing() {
        if (mCurrentSession != null) {
            genPreviewIfNeeded();
            mCurrentSession.stopPreview();
            mCurrentSession.stop();
            mCurrentSession.release();
            mCurrentSession = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        Log.e(TAG, error);
    }

    public static byte[] getPreviewData() {
        return previewData;
    }

    public long getPreviewTimestamp() {
        return previewTimestamp;
    }

    private void genPreviewIfNeeded() {
        if (mCurrentSession == null) return;
        byte[] jpgBytes = mCurrentSession.getLastPreviewImage();
        if (jpgBytes != null) {
            previewData = jpgBytes;
            previewTimestamp = System.currentTimeMillis();
        }
    }

    public int getStreamingPort() {
        return mStreamingPort;
    }

    public void startStream() {
        Intent intent = new Intent(this, RtspServer.class);
        intent.putExtra(Constants.RtspServerConstants.INTENT_PORT, getStreamingPort());
        startService(intent);
        updateRunningMode(ServiceDaemon.RUNNING_MODE.STREAMING);
    }

    public void stopStream() {
        stopService(new Intent(this, RtspServer.class));
        updateRunningMode(ServiceDaemon.RUNNING_MODE.STANDING_BY);
    }

    private void updateRunningMode(ServiceDaemon.RUNNING_MODE newMode) {
        mMode = newMode;
        ServiceDaemon.getInstance().getCameraManager().broadcastMyInfo();
    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        Log.d(TAG, "Bitrate: " + bitrate);
    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        if (e != null) {
            logError(e.getMessage());
        }

    }

    @Override
    public void onPreviewStarted() {
        Log.d(TAG, "Preview started.");

    }

    @Override
    public void onSessionConfigured() {
        Log.d(TAG, "Preview configured.");
    }

    @Override
    public void onSessionStarted() {
        Log.d(TAG, "Session started.");

    }

    @Override
    public void onSessionStopped() {
        Log.d(TAG, "Session stopped.");

    }

    public static ServiceDaemon.RUNNING_MODE getRunningMode() {
        return mMode;
    }
}
