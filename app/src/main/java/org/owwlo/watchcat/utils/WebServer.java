package org.owwlo.watchcat.utils;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.owwlo.watchcat.model.AuthResult;
import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.model.GeneralNetworkResponse;
import org.owwlo.watchcat.model.Viewer;
import org.owwlo.watchcat.model.ViewerPasscode;
import org.owwlo.watchcat.services.CameraDaemon;
import org.owwlo.watchcat.services.ServiceDaemon;
import org.owwlo.watchcat.utils.EventBus.IncomingAuthorizationCancelEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private final static String TAG = WebServer.class.getCanonicalName();

    private final static String kURL_CONTROL = "control";
    private final static String kURL_GET_INFO = "get_info";
    private final static String kURL_GET_PREVIEW = "get_preview";
    private final static String kURL_CLIENT = "client";
    private final static String kURL_REMOVE = "remove";
    private final static String kURL_UPDATE_STATUS = "update_status";
    private final static String kURL_AUTH_REQUEST = "auth_request";
    private final static String kURL_AUTH = "auth";

    private final static String kMIME_JSON = "application/json; charset=utf-8";
    private final static String kMIME_JPEG = NanoHTTPD.mimeTypes().get("jpeg");

    private ServiceDaemon mainService;
    private AuthManager authManager;

    public WebServer(int port, ServiceDaemon serviceDaemon) {
        super(port);
        mainService = serviceDaemon;
        authManager = mainService.getAuthManager();
    }

    private class HTTPSessionHelper {
        private IHTTPSession session;
        private Map<String, String> parsedBody = null;

        public HTTPSessionHelper(IHTTPSession session) {
            this.session = session;
        }

        public String getPostRawData() {
            if (parsedBody == null) {
                parsedBody = new HashMap<>();
                try {
                    session.parseBody(parsedBody);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ResponseException e) {
                    e.printStackTrace();
                }
            }
            return parsedBody.get("postData");
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        final String remoteIp = session.getRemoteIpAddress();
        Log.d(TAG, remoteIp + " requested: " + session.getUri());
        final List<String> uri = Arrays.asList(session.getUri().substring(1).split("/"));
        Iterator<String> iter = uri.iterator();

        final String actionDomain = iter.hasNext() ? iter.next() : "";
        if (actionDomain.equals(kURL_CONTROL)) {
            if (iter.hasNext()) {
                String next = iter.next();
                if (next.equals(kURL_GET_INFO)) {
                    final CameraInfo info = mainService.getCameraInfo();
                    return newFixedLengthResponse(Response.Status.OK, kMIME_JSON, JsonUtils.toJson(info));
                } else if (next.equals(kURL_GET_PREVIEW)) {
                    byte[] previewData = CameraDaemon.getPreviewData();
                    ByteArrayInputStream bs = new ByteArrayInputStream(previewData);
                    return newChunkedResponse(Response.Status.OK, kMIME_JPEG, bs);
                }
            }
        } else if (actionDomain.equals(kURL_CLIENT)) {
            final String action = iter.hasNext() ? iter.next() : "";
            if (action.equals(kURL_REMOVE)) {
                mainService.getCameraManager().updateClientStatus(remoteIp, ServiceDaemon.RUNNING_MODE.SHUTTING_DOWN, null);
                return newFixedLengthResponse(Response.Status.OK, kMIME_JSON, JsonUtils.toJson(new GeneralNetworkResponse(true)));
            } else if (action.equals(kURL_UPDATE_STATUS)) {
                HTTPSessionHelper postSession = new HTTPSessionHelper(session);
                String postBody = postSession.getPostRawData();
                Log.d(TAG, "payload: " + postBody);
                CameraInfo info = JsonUtils.parseJson(postBody, CameraInfo.class);
                mainService.getCameraManager().updateClientStatus(remoteIp, info.getRunningMode(), info);
                return newFixedLengthResponse(Response.Status.OK, kMIME_JSON, JsonUtils.toJson(new GeneralNetworkResponse(true)));
            } else if (action.equals(kURL_AUTH_REQUEST)) {
                HTTPSessionHelper postSession = new HTTPSessionHelper(session);
                String postBody = postSession.getPostRawData();
                Log.d(TAG, "payload: " + postBody);

                Viewer viewer = JsonUtils.parseJson(postBody, Viewer.class);

                AuthResult result = new AuthResult(AuthResult.kRESULT_DENIED);
                if (authManager.isAccessGranted(viewer.getId())) {
                    result.setResult(AuthResult.kRESULT_GRANTED);
                } else {
                    result.setResult(AuthResult.kRESULT_NEW_AUTH);
                    EventBus.getDefault().post(authManager.newAuth(viewer.getId(), viewer.getName()));
                }
                return newFixedLengthResponse(Response.Status.OK, kMIME_JSON, JsonUtils.toJson(result));
            } else if (action.equals(kURL_AUTH)) {
                HTTPSessionHelper postSession = new HTTPSessionHelper(session);
                String postBody = postSession.getPostRawData();
                Log.d(TAG, "payload: " + postBody);

                AuthResult result = new AuthResult(AuthResult.kRESULT_DENIED);
                ViewerPasscode passcode = JsonUtils.parseJson(postBody, ViewerPasscode.class);
                Viewer viewer = authManager.auth(passcode.getId(), passcode.getPasscode());
                if (viewer != null) {
                    result.setResult(AuthResult.kRESULT_GRANTED);
                    EventBus.getDefault().post(new IncomingAuthorizationCancelEvent(passcode.getId()));
                }
                return newFixedLengthResponse(Response.Status.OK, kMIME_JSON, JsonUtils.toJson(result));
            }
        }
        return newFixedLengthResponse("not handled");
    }
}
