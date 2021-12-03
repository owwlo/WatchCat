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

import java.io.IOException;

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
    public static Session parse(String uri, String auth) throws IllegalStateException, IOException {
        SessionBuilder builder = SessionBuilder.getInstance().clone();
        builder.setAuth(auth);
        Session session = builder.build();
        return session;

    }

}
