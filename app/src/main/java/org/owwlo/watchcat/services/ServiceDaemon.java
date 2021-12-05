package org.owwlo.watchcat.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

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
import org.owwlo.watchcat.utils.EventBus.VideoPlayerStateChangeEvent;
import org.owwlo.watchcat.utils.JsonUtils;
import org.owwlo.watchcat.utils.PreviewKeeper;
import org.owwlo.watchcat.utils.StringDetailedRequest;
import org.owwlo.watchcat.utils.Utils;
import org.owwlo.watchcat.utils.WebServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceDaemon extends Service implements NsdListener {
    private final static String TAG = ServiceDaemon.class.getCanonicalName();
    private WebServer webServer = null;
    private NsdHelper nsdHelper = null;
    private RequestQueue httpRequestQueue;
    private RemoteCameraManager cameraManager = null;
    private int controlPort = 0;
    private AppDatabase database = null;
    private AuthManager authManager = null;
    private PreviewKeeper previewKeeper = PreviewKeeper.getInstance();

    private static ServiceDaemon sInstance = null;

    private static String selfServiceName = null;

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
        private boolean checkAlive = true;
        private Runnable aliveChecker = new Runnable() {
            @Override
            public void run() {
                if (checkAlive) {
                    synchronized (cameras) {
                        for (Map.Entry<String, CameraInfo> entry : cameras.entrySet()) {
                            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                                    Utils.Urls.getControlTarget(entry.getKey(), entry.getValue().getControlPort()).getCameraInfoURI(),
                                    response -> {
                                    }, error -> {
                                Log.d(TAG, "error occurs, removing the following ip from the Camera pool: " + entry.getKey());
                                removeCameraInfo(entry.getKey());
                            });
                            requestQueue.add(stringRequest);
                        }
                    }
                }
                handler.postDelayed(this, Constants.NSD_CHECKALIVE_INTERVAL_MS);
            }
        };

        public boolean isCheckAlive() {
            return checkAlive;
        }

        public void setCheckAlive(boolean checkAlive) {
            this.checkAlive = checkAlive;
        }

        private RequestQueue requestQueue;

        public RemoteCameraManager(Context context, RequestQueue requestQueue) {
            this.requestQueue = requestQueue;
            handler.post(aliveChecker);
        }

        private Map<String, CameraInfo> cameras = new HashMap<>();
        private Set<RemoteCameraEventListener> listeners = new HashSet<>();

        public void registerListener(RemoteCameraEventListener listener) {
            synchronized (cameras) {
                listeners.add(listener);
                for (Map.Entry<String, CameraInfo> entry : cameras.entrySet()) {
                    listener.onCameraAdded(entry.getKey(), entry.getValue());
                }
            }
        }

        public void removeListener(RemoteCameraEventListener listener) {
            listeners.remove(listener);
        }

        void notifyAdd(String ip, CameraInfo info) {
            for (RemoteCameraEventListener listener : listeners) {
                listener.onCameraAdded(ip, info);
            }
        }

        void notifyRemove(String ip) {
            for (RemoteCameraEventListener listener : listeners) {
                listener.onCameraRemoved(ip);
            }
        }

        void notifyStateUpdated(String ip, CameraInfo newInfo) {
            for (RemoteCameraEventListener listener : listeners) {
                listener.onStateUpdated(ip, newInfo);
            }
        }

        public void removeCameraInfo(String ip) {
            synchronized (cameras) {
                if (cameras.containsKey(ip)) {
                    cameras.remove(ip);
                    notifyRemove(ip);
                }
            }
        }

        public void handleCameraInfo(String ip, CameraInfo info) {
            synchronized (cameras) {
                if (!cameras.containsKey(ip)) {
                    cameras.put(ip, info);
                    notifyAdd(ip, info);
                } else {
                    CameraInfo oldInfo = cameras.get(ip);
                    if (!oldInfo.equals(info)) {
                        cameras.put(ip, info);
                        notifyStateUpdated(ip, info);
                    }
                }
            }
        }

        public void broadcastShuttingDown() {
            synchronized (cameras) {
                for (Map.Entry<String, CameraInfo> entry : cameras.entrySet()) {
                    StringRequest request = new StringRequest(Request.Method.POST,
                            Utils.Urls.getControlTarget(entry.getKey(), entry.getValue().getControlPort()).getClientShuttingDownURI(),
                            response -> {
                            }, error -> {
                        Log.d(TAG, "error occurs from: " + entry.getKey() + " error: " + error);
                    });
                    requestQueue.add(request);
                }
            }
        }

        public void sendMyInfo(final String targetIp, final CameraInfo targetInfo) {
            CameraDaemon cameraDaemon = CameraDaemon.getInstance();
            if (cameraDaemon == null) return;
            CameraInfo info = getCameraInfo();
            final String infoPayload = JsonUtils.toJson(info);

            StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST,
                    Utils.Urls.getControlTarget(targetIp, targetInfo.getControlPort()).getClientUpdateURI(),
                    response -> {
                    }, error -> {
                Log.d(TAG, "error occurs from: " + targetIp + " error: " + error);
            });
            Log.d(TAG, "sending: " + infoPayload + " to " + targetIp);
            request.setBody(infoPayload);
            requestQueue.add(request);
        }

        public void broadcastMyInfo() {
            CameraDaemon cameraDaemon = CameraDaemon.getInstance();
            if (cameraDaemon != null) {
                CameraInfo info = getCameraInfo();
                final String infoPayload = JsonUtils.toJson(info);

                Log.d(TAG, infoPayload);
                synchronized (cameras) {
                    for (Map.Entry<String, CameraInfo> entry : cameras.entrySet()) {
                        sendMyInfo(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    public CameraInfo getCameraInfo() {
        CameraInfo info = new CameraInfo();
        info.setControlPort(ServiceDaemon.getInstance().getControlPort());
        info.setName(Utils.getDeviceId());
        info.setVersion(Utils.getAppVersion());

        CameraDaemon cameraDaemon = CameraDaemon.getInstance();
        if (cameraDaemon != null) {
            info.setStreamingPort(cameraDaemon.getStreamingPort());
            info.setEnabled(cameraDaemon.getRunningMode() == ServiceDaemon.RUNNING_MODE.STREAMING);

            // TODO support dynamic resolution
            info.setWidth(1920);
            info.setHeight(1080);

            info.setThumbnailTimestamp(previewKeeper.getLastPreviewTime());
        }
        return info;
    }

    public RemoteCameraManager getCameraManager() {
        return cameraManager;
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
        super();
        sInstance = this;
    }

    public static ServiceDaemon getInstance() {
        return sInstance;
    }

    public int getControlPort() {
        return controlPort;
    }

    @Override
    public void onNsdRegistered(NsdService nsdService) {
        if (webServer == null || !webServer.isAlive()) {
            controlPort = nsdService.getPort();
            webServer = new WebServer(controlPort, this);
            startWebServer();
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
        StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST,
                camera.getUrls().getAuthAttemptURI(),
                response -> {
                    AuthResult result = JSON.parseObject(response, AuthResult.class);
                    EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, result.getResult()));
                }, error -> {
            EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, AuthResult.kRESULT_DENIED));
        });
        request.setBody(JsonUtils.toJson(myself));
        httpRequestQueue.add(request);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(PinInputDoneEvent event) {
        final Camera camera = event.getCamera();
        final String passcode = event.getPasscode();
        final Viewer myself = authManager.getMyself();
        final ViewerPasscode vp = new ViewerPasscode(myself.getId(), passcode);
        StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST,
                camera.getUrls().getPasscodeAuthURI(),
                response -> {
                    AuthResult result = JSON.parseObject(response, AuthResult.class);
                    EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, result.getResult()));
                }, error -> {
            EventBus.getDefault().post(new OutgoingAuthorizationResultEvent(camera, AuthResult.kRESULT_DENIED));
        });
        request.setBody(JsonUtils.toJson(vp));
        httpRequestQueue.add(request);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(VideoPlayerStateChangeEvent event) {
        if (cameraManager == null || nsdHelper == null) return;
        Log.d(TAG, "Received VideoPlayer latest state: " + event.getState());
        switch (event.getState()) {
            case PLAYING:
                cameraManager.setCheckAlive(false);
                nsdHelper.stopDiscovery();
                break;
            case EXITING:
                cameraManager.setCheckAlive(true);
                nsdHelper.startDiscovery(NsdType.HTTP);
                break;
        }
    }

    @Override
    public void onNsdServiceResolved(NsdService nsdService) {
        final String serviceName = nsdService.getName();
        if (selfServiceName.equals(serviceName)) return;
        if (serviceName.indexOf("org.owwlo.watchcat.camera.") == 0) {
            // TODO protocol version check
            final String ip = nsdService.getHostIp();
            Log.d(TAG, "resolved[REMOTE]: " + serviceName + " " + ip + ":" + nsdService.getPort());
            final String url = Utils.Urls.getControlTarget(ip, nsdService.getPort()).getCameraInfoURI();
            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    url,
                    response -> {
                        Log.d(TAG, ip + " response: " + response);
                        CameraInfo info = JSON.parseObject(response, CameraInfo.class);
                        cameraManager.handleCameraInfo(ip, info);
                        cameraManager.sendMyInfo(ip, info);
                    }, error -> Log.d(TAG, "error requesting " + url + ", err: " + error.toString()));
            httpRequestQueue.add(stringRequest);
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

        httpRequestQueue = Volley.newRequestQueue(this);
        cameraManager = new RemoteCameraManager(this, httpRequestQueue);

        initServiceName();
        initNsdService();
    }

    private void initNsdService() {
        nsdHelper = new NsdHelper(this, this);
        nsdHelper.setAutoResolveEnabled(true);
        nsdHelper.setDiscoveryTimeout(Constants.NSD_TIMEOUT_SECS);
        nsdHelper.registerService(selfServiceName, NsdType.HTTP);
        nsdHelper.startDiscovery(NsdType.HTTP);
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    private void initServiceName() {
        selfServiceName = "org.owwlo.watchcat.camera." + Constants.WATCHCAT_API_VER + "." + System.currentTimeMillis();
    }

    private void startWebServer() {
        try {
            webServer.start();
        } catch (IOException e) {
            Log.d(TAG, "unable to start the WebServer");
            e.printStackTrace();
        }
    }

    private void stopWebServer() {
        if (webServer != null && webServer.isAlive()) {
            webServer.stop();
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
}
