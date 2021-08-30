/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.owwlo.watchcat.libstreaming;

import android.content.ContentValues;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Set;

/**
 * This class parses URIs received by the RTSP server and configures a Session accordingly.
 * <p>
 * This file is originally from libstreaming project: https://github.com/fyhertz/libstreaming
 * Slightly modified to sever the need of WatchCat.
 */
public class UriParser {
    public final static String TAG = UriParser.class.getSimpleName();

    /**
     * Configures a Session according to the given URI.
     *
     * @param uri The URI
     * @return A Session configured according to the URI
     * @throws IllegalStateException
     * @throws IOException
     */
    public static Session parse(String uri) throws IllegalStateException, IOException {
        SessionBuilder builder = SessionBuilder.getInstance().clone();

        String query = URI.create(uri).getQuery();
        String[] queryParams = query == null ? new String[0] : query.split("&");
        ContentValues params = new ContentValues();
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            String value = "";
            try {
                value = keyValue[1];
            } catch (ArrayIndexOutOfBoundsException e) {
            }

            params.put(
                    URLEncoder.encode(keyValue[0], "UTF-8"), // Name
                    URLEncoder.encode(value, "UTF-8")  // Value
            );
        }

        if (params.size() > 0) {
            Set<String> paramKeys = params.keySet();
            // Those parameters must be parsed first or else they won't necessarily be taken into account
            for (String paramName : paramKeys) {
                String paramValue = params.getAsString(paramName);
                Log.d(TAG, "request param will be ignored: " + paramName + "=" + paramValue);
            }
        }

        Session session = builder.build();
        return session;

    }

}
