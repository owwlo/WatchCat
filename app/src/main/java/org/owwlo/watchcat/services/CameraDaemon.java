package org.owwlo.watchcat.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.owwlo.watchcat.libstreaming.Session;
import org.owwlo.watchcat.libstreaming.SessionBuilder;
import org.owwlo.watchcat.libstreaming.gl.SurfaceView;

import org.owwlo.watchcat.utils.Constants;
import org.owwlo.watchcat.utils.Utils;
import org.owwlo.watchcat.libstreaming.RtspServer;

import org.owwlo.watchcat.libstreaming.video.VideoQuality;

public class CameraDaemon extends IntentService implements Session.Callback {
    private final static String TAG = CameraDaemon.class.getCanonicalName();

    public enum RUNNING_MODE {
        STREAMING, STANDING_BY
    }

    private static RUNNING_MODE mMode = RUNNING_MODE.STANDING_BY;

    private static CameraDaemon sInstance = null;
    private Session mCurrentSession = null;
    private String mAccessPassword = "";
    // TODO better way to set
    private int mStreamingPort = Constants.STREAMING_PORT;

    public String getAccessPassword() {
        return mAccessPassword;
    }

    public void setAccessPassword(String mAccessPassword) {
        this.mAccessPassword = mAccessPassword;
    }

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
        super("WatchCatDaemon");
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

        Log.d(this.getPackageName(), "ip: " + Utils.getLocalIPAddress());

        // TODO add audio support
        SessionBuilder.getInstance()
                .setCallback(this)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(1920, 1080, 30, 1500000));
        sInstance = this;
    }

    public void startPreviewing(SurfaceView surfaceView, int angle) {
        stopPreviewing();
        mCurrentSession = SessionBuilder.getInstance().setSurfaceView(surfaceView).setPreviewOrientation(angle).build();
        mCurrentSession.startPreview();
    }

    public Bitmap getLastPreviewImage() {
        if (mCurrentSession == null) return null;
        return mCurrentSession.getLastPreviewImage();
    }

    public void updateCurrentSessionRotation(int angle) {
        if (mCurrentSession == null) return;
        mCurrentSession.setPreviewOrientation(angle);
    }

    public void stopPreviewing() {
        if (mCurrentSession != null) {
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

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String dataString = intent.getDataString();
        Log.d(this.getPackageCodePath(), "dataString: " + dataString);
    }


    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        Log.e(TAG, error);
    }

    public void startStream() {
        // TODO better way to get a port
        Intent intent = new Intent(this, RtspServer.class);
        intent.putExtra(Constants.RtspServerConstants.INTENT_USERNAME, ""); // we use password only
        intent.putExtra(Constants.RtspServerConstants.INTENT_PASSWORD, mAccessPassword);
        intent.putExtra(Constants.RtspServerConstants.INTENT_PORT, mStreamingPort);
        startService(intent);

        mMode = RUNNING_MODE.STREAMING;
    }

    public void stopStream() {
        stopService(new Intent(this, RtspServer.class));

        mMode = RUNNING_MODE.STANDING_BY;
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

    public static RUNNING_MODE getRunningMode() {
        return mMode;
    }
}
