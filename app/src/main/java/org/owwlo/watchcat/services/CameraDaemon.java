package org.owwlo.watchcat.services;

import android.app.Service;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;

import org.owwlo.watchcat.libstreaming.RtspServer;
import org.owwlo.watchcat.libstreaming.Session;
import org.owwlo.watchcat.libstreaming.SessionBuilder;
import org.owwlo.watchcat.libstreaming.gl.SurfaceView;
import org.owwlo.watchcat.utils.Constants;
import org.owwlo.watchcat.utils.Toaster;
import org.owwlo.watchcat.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraDaemon extends Service implements Session.Callback {
    private final static String TAG = CameraDaemon.class.getCanonicalName();

    private static ServiceDaemon.RUNNING_MODE mMode = ServiceDaemon.RUNNING_MODE.STANDING_BY;

    private static CameraDaemon sInstance = null;
    private Session mCurrentSession = null;
    private String mAccessPassword = "";
    private int mStreamingPort = Constants.DEFAULT_STREAMING_PORT;

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

    public class CameraParams {
        private final String TAG = CameraParams.class.getCanonicalName();

        final List<Integer> awesomeProfiles = Arrays.asList(CamcorderProfile.QUALITY_1080P, CamcorderProfile.QUALITY_720P, CamcorderProfile.QUALITY_480P, CamcorderProfile.QUALITY_LOW);
        List<Integer> supportedProfiles = new ArrayList<>();
        int selectedProfileIdx = 0;

        private String serializeProfile(CamcorderProfile profile) {
            return new Gson().toJson(profile);
        }

        public CameraParams() {
            for (Integer profile : awesomeProfiles) {
                if (CamcorderProfile.hasProfile(profile)) {
                    supportedProfiles.add(profile);
                }
            }
            for (Integer profile : supportedProfiles) {
                CamcorderProfile p = CamcorderProfile.get(profile);
                Log.d(TAG, "supported profile: " + serializeProfile(p));
            }
        }

        public CamcorderProfile getSelectedProfile() {
            return CamcorderProfile.get(supportedProfiles.get(selectedProfileIdx));
        }

        public int getSelectedProfileIdx() {
            return selectedProfileIdx;
        }

        public void setSelectedProfile(int idx) {
            if (idx < 0 || idx >= supportedProfiles.size()) {
                return;
            }
            selectedProfileIdx = idx;
        }

        public String[] getSupportedProfilesDesc() {
            String[] desc = new String[supportedProfiles.size()];
            for (int i = 0; i < desc.length; i++) {
                CamcorderProfile p = CamcorderProfile.get(supportedProfiles.get(i));
                desc[i] = "" + p.videoFrameWidth + "x" + p.videoFrameHeight + " " + p.videoFrameRate + "fps";
            }
            return desc;
        }
    }

    CameraParams cameraParams = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mStreamingPort = Utils.getAvailablePort(Constants.DEFAULT_STREAMING_PORT);

        Toaster.debug.info( "CameraDaemon started port: " + mStreamingPort);

        cameraParams = new CameraParams();

        // TODO add audio support
        SessionBuilder.getInstance()
                .setCallback(this)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264);

        sInstance = this;
    }

    public CameraParams getCameraParams() {
        return cameraParams;
    }

    public void startPreviewing(SurfaceView surfaceView, boolean flip) {
        stopPreviewing();
        mCurrentSession = SessionBuilder.getInstance().setSurfaceView(surfaceView).setPreviewFlip(flip).build();
        mCurrentSession.startPreview();
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

    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        Log.e(TAG, error);
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
            e.printStackTrace();
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
