package org.owwlo.watchcat.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alibaba.fastjson.JSON;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owwlo.watchcat.model.AppDatabase;
import org.owwlo.watchcat.model.AuthResult;
import org.owwlo.watchcat.model.Camera;
import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.model.Viewer;
import org.owwlo.watchcat.model.ViewerPasscode;
import org.owwlo.watchcat.nsdhelper.NsdHelper;
import org.owwlo.watchcat.nsdhelper.NsdListener;
import org.owwlo.watchcat.nsdhelper.NsdService;
import org.owwlo.watchcat.nsdhelper.NsdType;
import org.owwlo.watchcat.utils.AuthManager;
import org.owwlo.watchcat.utils.Constants;
import org.owwlo.watchcat.utils.EventBus.OutgoingAuthorizationRequestEvent;
import org.owwlo.watchcat.utils.EventBus.OutgoingAuthorizationResultEvent;
import org.owwlo.watchcat.utils.EventBus.PinInputDoneEvent;
import org.owwlo.watchcat.utils.JsonUtils;
import org.owwlo.watchcat.utils.StringDetailedRequest;
import org.owwlo.watchcat.utils.Utils;
import org.owwlo.watchcat.utils.WebServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceDaemon extends IntentService implements NsdListener {
    private final static String TAG = ServiceDaemon.class.getCanonicalName();
    private WebServer mServer = null;
    private NsdHelper nsdHelper = null;
    private RequestQueue mHttpRequestQueue;
    private RemoteCameraManager mCameraManager = null;
    private String mLocalIpAddress = "";
    private int mControlPort = 0;
    private AppDatabase database = null;
    private AuthManager authManager = null;

    private static ServiceDaemon sInstance = null;

    public enum RUNNING_MODE {
        STREAMING, STANDING_BY, SHUTTING_DOWN
    }

    public interface RemoteCameraEventListener {
        void onCameraAdded(String ip, CameraInfo info);

        void onCameraRemoved(String ip);

        void onStateUpdated(String ip, CameraInfo newInfo);
    }

    public class RemoteCameraManager {
        private final String TAG = RemoteCameraManager.class.getCanonicalName();

        public void updateClientStatus(String remoteIp, RUNNING_MODE mode, CameraInfo info) {
            switch (mode) {
                case SHUTTING_DOWN:
                    removeCameraInfo(remoteIp);
                    break;
                case STREAMING:
                case STANDING_BY:
                    handleCameraInfo(remoteIp, info);
                    break;
                default:
            }
        }

        private Handler handler = new Handler();
        private Runnable mAliveChecker = new Runnable() {
            @Override
            public void run() {
                synchronized (mCameras) {
                    for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, Utils.getCameraInfoURI(entry.getKey(), entry.getValue().getControlPort()),
                                response -> {
                                }, error -> {
                            Log.d(TAG, "error occurs, removing the following ip from the Camera pool: " + entry.getKey());
                            removeCameraInfo(entry.getKey());
                        });
                        mRequestQueue.add(stringRequest);
                    }
                }
                handler.postDelayed(this, Constants.NSD_CHECKALIVE_INTERVAL_SECS);
            }
        };

        private RequestQueue mRequestQueue;

        public RemoteCameraManager(Context context, RequestQueue requestQueue) {
            mRequestQueue = requestQueue;
            handler.post(mAliveChecker);
        }

        private Map<String, CameraInfo> mCameras = new HashMap<>();
        private Set<RemoteCameraEventListener> mListeners = new HashSet<>();

        public void registerListener(RemoteCameraEventListener listener) {
            synchronized (mCameras) {
                mListeners.add(listener);
                for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                    listener.onCameraAdded(entry.getKey(), entry.getValue());
                }
            }
        }

        public void removeListener(RemoteCameraEventListener listener) {
            mListeners.remove(listener);
        }

        void notifyAdd(String ip, CameraInfo info) {
            for (RemoteCameraEventListener listener : mListeners) {
                listener.onCameraAdded(ip, info);
            }
        }

        void notifyRemove(String ip) {
            for (RemoteCameraEventListener listener : mListeners) {
                listener.onCameraRemoved(ip);
            }
        }

        void notifyStateUpdated(String ip, CameraInfo newInfo) {
            for (RemoteCameraEventListener listener : mListeners) {
                listener.onStateUpdated(ip, newInfo);
            }
        }

        public void removeCameraInfo(String ip) {
            synchronized (mCameras) {
                if (mCameras.containsKey(ip)) {
                    mCameras.remove(ip);
                    notifyRemove(ip);
                }
            }
        }

        public void handleCameraInfo(String ip, CameraInfo info) {
            synchronized (mCameras) {
                if (!mCameras.containsKey(ip)) {
                    mCameras.put(ip, info);
                    notifyAdd(ip, info);
                } else {
                    CameraInfo oldInfo = mCameras.get(ip);
                    if (!oldInfo.equals(info)) {
                        mCameras.put(ip, info);
                        notifyStateUpdated(ip, info);
                    }
                }
            }
        }

        public void broadcastShuttingDown() {
            synchronized (mCameras) {
                for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                    StringRequest request = new StringRequest(Request.Method.POST, Utils.getClientShuttingDownURI(entry.getKey(), entry.getValue().getControlPort()),
                            response -> {
                            }, error -> {
                        Log.d(TAG, "error occurs from: " + entry.getKey() + " error: " + error);
                    });
                    mRequestQueue.add(request);
                }
            }
        }

        public void sendMyInfo(final String targetIp, final CameraInfo targetInfo) {
            CameraDaemon cameraDaemon = CameraDaemon.getInstance();
            if (cameraDaemon == null) return;
            CameraInfo info = getCameraInfo();
            final String infoPayload = JsonUtils.toJson(info);

            StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST, Utils.getClientUpdateURI(targetIp, targetInfo.getControlPort()),
                    response -> {
                    }, error -> {
                Log.d(TAG, "error occurs from: " + targetIp + " error: " + error);
            });
            Log.d(TAG, "sending: " + infoPayload + " to " + targetIp);
            request.setBody(infoPayload);
            mRequestQueue.add(request);
        }

        public void broadcastMyInfo() {
            CameraDaemon cameraDaemon = CameraDaemon.getInstance();
            if (cameraDaemon != null) {
                CameraInfo info = getCameraInfo();
                final String infoPayload = JsonUtils.toJson(info);

                Log.d(TAG, infoPayload);
                synchronized (mCameras) {
                    for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                        sendMyInfo(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    public CameraInfo getCameraInfo() {
        CameraInfo info = new CameraInfo();
        info.setControlPort(ServiceDaemon.getInstance().getControlPort());

        CameraDaemon cameraDaemon = CameraDaemon.getInstance();
        if (cameraDaemon != null) {
            info.setStreamingPort(cameraDaemon.getStreamingPort());
            info.setEnabled(cameraDaemon.getRunningMode() == ServiceDaemon.RUNNING_MODE.STREAMING);

            // TODO support dynamic resolution
            info.setWidth(1920);
            info.setHeight(1080);
        }
        return info;
    }

    public RemoteCameraManager getCameraManager() {
        return mCameraManager;
    }

    // Binding starts
    private final IBinder binder = new ServiceDaemon.LocalBinder();

    public class LocalBinder extends Binder {
        public ServiceDaemon getService() {
            return ServiceDaemon.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    // Binding ends

    public ServiceDaemon() {
        super("ServiceDaemon");
        sInstance = this;
    }

    public static ServiceDaemon getInstance() {
        return sInstance;
    }

    public int getControlPort() {
        return mControlPort;
    }

    @Override
    public void onNsdRegistered(NsdService nsdService) {
        if (mServer == null || !mServer.isAlive()) {
            mControlPort = nsdService.getPort();
            mServer = new WebServer(mControlPort, this);
            startWebServer();
            Log.d(TAG, "started service at: " + mControlPort);
        }
    }

    @Override
    public void onNsdDiscoveryFinished() {
        nsdHelper.startDiscovery(NsdType.HTTP);
    }

    @Override
    public void onNsdServiceFound(NsdService nsdService) {
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(OutgoingAuthorizationRequestEvent event) {
        final Camera camera = event.getCamera();
        final Viewer myself = authManager.getMyself();
        StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST, Utils.getAuthAttemptURI(camera.getIp(), camera.getControlPort()),
                response -> {
                    AuthResult result = JSON.parseObject(response, AuthResult.class);
                    EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, result.getResult()));
                }, error -> {
            EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, AuthResult.kRESULT_DENIED));
        });
        request.setBody(JsonUtils.toJson(myself));
        mHttpRequestQueue.add(request);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(PinInputDoneEvent event) {
        final Camera camera = event.getCamera();
        final String passcode = event.getPasscode();
        final Viewer myself = authManager.getMyself();
        final ViewerPasscode vp = new ViewerPasscode(myself.getId(), passcode);
        StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST, Utils.getPasscodeAuthURI(camera.getIp(), camera.getControlPort()),
                response -> {
                    AuthResult result = JSON.parseObject(response, AuthResult.class);
                    EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, result.getResult()));
                }, error -> {
            EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, AuthResult.kRESULT_DENIED));
        });
        request.setBody(JsonUtils.toJson(vp));
        mHttpRequestQueue.add(request);
    }

    @Override
    public void onNsdServiceResolved(NsdService nsdService) {
        final String serviceName = nsdService.getName();
        final String ip = nsdService.getHostIp();
        if (mLocalIpAddress.equals(ip)) return;
        if (serviceName.indexOf("org.owwlo.watchcat.camera.") == 0 && ip != null) {
            // TODO protocol version check
            String type = nsdService.getType();
            Log.d(TAG, "resolved[REMOTE]: " + type + " " + ip + ":" + nsdService.getPort());

            StringRequest stringRequest = new StringRequest(Request.Method.GET, Utils.getCameraInfoURI(ip, nsdService.getPort()),
                    response -> {
                        Log.d(TAG, ip + " response: " + response);
                        CameraInfo info = JSON.parseObject(response, CameraInfo.class);
                        mCameraManager.handleCameraInfo(ip, info);
                        mCameraManager.sendMyInfo(ip, info);
                    }, error -> Log.d(TAG, error.getLocalizedMessage()));

            mHttpRequestQueue.add(stringRequest);
        }
    }

    @Override
    public void onNsdServiceLost(NsdService nsdService) {
    }

    @Override
    public void onNsdError(String s, int i, String s1) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "watchcat")
                .enableMultiInstanceInvalidation()
                .allowMainThreadQueries()
                .build();
        authManager = new AuthManager(this);

        Utils.sContext = this;

        mHttpRequestQueue = Volley.newRequestQueue(this);
        mCameraManager = new RemoteCameraManager(this, mHttpRequestQueue);

        fetchLocalIp();

        nsdHelper = new NsdHelper(this, this);
        nsdHelper.setAutoResolveEnabled(true);
        nsdHelper.setDiscoveryTimeout(Constants.NSD_TIMEOUT_SECS);
        nsdHelper.registerService("org.owwlo.watchcat.camera." + Constants.WATCHCAT_API_VER, NsdType.HTTP);
        nsdHelper.startDiscovery(NsdType.HTTP);
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    private void fetchLocalIp() {
        mLocalIpAddress = Utils.getLocalIPAddress().getHostAddress();
        Log.d(TAG, "ip: " + mLocalIpAddress);
    }

    private void startWebServer() {
        try {
            mServer.start();
        } catch (IOException e) {
            Log.d(TAG, "unable to start the WebServer");
            e.printStackTrace();
        }
    }

    private void stopWebServer() {
        if (mServer != null && mServer.isAlive()) {
            mServer.stop();
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        nsdHelper.unregisterService();
        nsdHelper.stopDiscovery();
        stopWebServer();
        super.onDestroy();
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String dataString = intent.getDataString();
        Log.d(this.getPackageCodePath(), "dataString: " + dataString);
    }

}
