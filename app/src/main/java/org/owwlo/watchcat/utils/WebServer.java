package org.owwlo.watchcat.utils;

import android.util.Log;

import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.model.GeneralNetworkResponse;
import org.owwlo.watchcat.services.CameraDaemon;
import org.owwlo.watchcat.services.ServiceDaemon;

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

    private ServiceDaemon mServiceDaemon;

    public WebServer(int port, ServiceDaemon serviceDaemon) {
        super(port);
        mServiceDaemon = serviceDaemon;
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
        if (actionDomain.equals("control")) {
            if (iter.hasNext()) {
                String next = iter.next();
                if (next.equals("get_info")) {
                    CameraInfo info = mServiceDaemon.getCameraInfo();
                    return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.mimeTypes().get("json"), JsonUtils.toJson(info));
                } else if (next.equals("get_preview")) {
                    byte[] previewData = CameraDaemon.getPreviewData();
                    ByteArrayInputStream bs = new ByteArrayInputStream(previewData);
                    return newChunkedResponse(Response.Status.OK, NanoHTTPD.mimeTypes().get("jpeg"), bs);
                }
            }
        } else if (actionDomain.equals("client")) {
            final String action = iter.hasNext() ? iter.next() : "";
            if (action.equals("remove")) {
                mServiceDaemon.getCameraManager().updateClientStatus(remoteIp, ServiceDaemon.RUNNING_MODE.SHUTTING_DOWN, null);
                return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.mimeTypes().get("json"), JsonUtils.toJson(new GeneralNetworkResponse(true)));
            } else if (action.equals("update_status")) {
                HTTPSessionHelper postSession = new HTTPSessionHelper(session);
                String postBody = postSession.getPostRawData();
                Log.d(TAG, "payload: " + postBody);
                CameraInfo info = JsonUtils.parseJson(postBody, CameraInfo.class);
                mServiceDaemon.getCameraManager().updateClientStatus(remoteIp, info.getRunningMode(), info);
                return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.mimeTypes().get("json"), JsonUtils.toJson(new GeneralNetworkResponse(true)));
            }
        }
        return newFixedLengthResponse("not handled");
    }
}
