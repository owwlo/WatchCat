package org.owwlo.watchcat.utils;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import java.nio.charset.StandardCharsets;

public class StringDetailedRequest extends StringRequest {
    private String requestBody = null;

    public StringDetailedRequest(int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    public void setBody(String body) {
        requestBody = body;
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=utf-8";
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return requestBody == null ? null : requestBody.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String responseString = "";
        if (response != null) {
            responseString = String.valueOf(response.statusCode);
            // can get more details such as response.headers
        }
        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
    }

}
