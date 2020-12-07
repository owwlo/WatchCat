package org.owwlo.watchcat.utils;

import android.util.Log;

import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.services.CameraDaemon;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private final static String TAG = WebServer.class.getCanonicalName();

    public WebServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        final Map parms = session.getParameters();
        final Method method = session.getMethod();
        Log.d(TAG, "uri: " + session.getUri());
        final List<String> uri = Arrays.asList(session.getUri().substring(1).split("/"));
        Iterator<String> iter = uri.iterator();

        if (iter.hasNext() && iter.next().equals("control")) {
            if (iter.hasNext()) {
                String next = iter.next();
                if (next.equals("get_info")) {
                    CameraInfo info = new CameraInfo();
                    info.setWidth(1920);
                    info.setHeight(1080);
                    info.setStreamingPort(Constants.STREAMING_PORT);
                    info.setEnabled(CameraDaemon.getRunningMode() == CameraDaemon.RUNNING_MODE.STREAMING);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", JsonUtils.toJson(info));
                } else if (next.equals("get_preview")) {
                    byte[] previewData = CameraDaemon.getPreviewData();
                    ByteArrayInputStream bs = new ByteArrayInputStream(previewData);
                    return newChunkedResponse(Response.Status.OK, NanoHTTPD.mimeTypes().get("jpeg"), bs);
                }
            }
        }
        return newFixedLengthResponse("not handled");
    }
}
