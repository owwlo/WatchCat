package org.owwlo.watchcat.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.rafakob.nsdhelper.NsdHelper;
import com.rafakob.nsdhelper.NsdListener;
import com.rafakob.nsdhelper.NsdService;
import com.rafakob.nsdhelper.NsdType;

import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.utils.Constants;
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

    private static ServiceDaemon sInstance = null;

    public enum RUNNING_MODE {
        STREAMING, STANDING_BY, SHUTTING_DOWN
    }

    public static class RemoteCameraManager {
        private final static String TAG = RemoteCameraManager.class.getCanonicalName();

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

        public interface RemoteCameraEventListener {
            void onCameraAdded(String ip, CameraInfo info);

            void onCameraRemoved(String ip);

            void onStateUpdated(String ip, CameraInfo newInfo);
        }

        private Handler handler = new Handler();
        private Runnable mAliveChecker = new Runnable() {
            @Override
            public void run() {
                synchronized (mCameras) {
                    for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, Utils.getCameraInfoURI(entry.getKey()),
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

        public RemoteCameraManager(Context context) {
            mRequestQueue = Volley.newRequestQueue(context);
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
                    if (oldInfo.isEnabled() != info.isEnabled()) {
                        mCameras.put(ip, info);
                        notifyStateUpdated(ip, info);
                    }
                }
            }
        }

        public void broadcastShuttingDown() {
            synchronized (mCameras) {
                for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                    StringRequest request = new StringRequest(Request.Method.POST, Utils.getClientShuttingDownURI(entry.getKey()),
                            response -> {
                            }, error -> {
                        Log.d(TAG, "error occurs from: " + entry.getKey() + " error: " + error);
                    });
                    mRequestQueue.add(request);
                }
            }
        }

        public void broadcastMyInfo() {
            CameraDaemon cameraDaemon = CameraDaemon.getInstance();
            if (cameraDaemon != null) {
                CameraInfo info = cameraDaemon.getCameraInfo();
                final String infoPayload = JsonUtils.toJson(info);

                Log.d(TAG, infoPayload);
                synchronized (mCameras) {
                    for (Map.Entry<String, CameraInfo> entry : mCameras.entrySet()) {
                        StringDetailedRequest request = new StringDetailedRequest(Request.Method.POST, Utils.getClientUpdateURI(entry.getKey()),
                                response -> {
                                }, error -> {
                            Log.d(TAG, "error occurs from: " + entry.getKey() + " error: " + error);
                        });
                        Log.d(TAG, "sending: " + infoPayload + " to " + entry.getKey());
                        request.setBody(infoPayload);
                        mRequestQueue.add(request);
                    }
                }
            }
        }
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

    @Override
    public void onNsdRegistered(NsdService nsdService) {
        mServer = new WebServer(Constants.CONTROL_PORT, this);
        startWebServer();
        Log.d(TAG, "started service at: " + nsdService.getPort());
    }

    @Override
    public void onNsdDiscoveryFinished() {
        nsdHelper.startDiscovery(NsdType.HTTP);
    }

    @Override
    public void onNsdServiceFound(NsdService nsdService) {
    }

    @Override
    public void onNsdServiceResolved(NsdService nsdService) {
        String serviceName = nsdService.getName();
        final String ip = nsdService.getHostIp();
        if (mLocalIpAddress == ip) return;
        if (serviceName.indexOf("org.owwlo.watchcat.camera.") == 0 && ip != null) {
            // TODO protocol version check
            String type = nsdService.getType();
            Log.d(TAG, "resolved: " + type + " " + ip);

            StringRequest stringRequest = new StringRequest(Request.Method.GET, Utils.getCameraInfoURI(ip),
                    response -> {
                        Log.d(TAG, ip + " response: " + response);
                        CameraInfo info = JSON.parseObject(response, CameraInfo.class);
                        mCameraManager.handleCameraInfo(ip, info);
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

        Utils.sContext = this;

        mHttpRequestQueue = Volley.newRequestQueue(this);
        mCameraManager = new RemoteCameraManager(this);

        int webServicePort = Utils.findAvaiablePort();

        fetchLocalIp();

        nsdHelper = new NsdHelper(this, this);
        nsdHelper.setAutoResolveEnabled(true);
        nsdHelper.setDiscoveryTimeout(Constants.NSD_TIMEOUT_SECS);
        nsdHelper.registerService("org.owwlo.watchcat.camera." + Constants.WATCHCAT_API_VER, NsdType.HTTP);
        nsdHelper.startDiscovery(NsdType.HTTP);
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
