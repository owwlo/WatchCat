package org.owwlo.watchcat.utils;

public class Constants {
    public final static int WATCHCAT_API_VER = 1;

    public final static int DEFAULT_STREAMING_PORT = 57526;
    public final static int PREVIEW_DEFAULT_WIDTH_IN_DP = 310;

    public final static int NSD_TIMEOUT_SECS = 10;
    public final static int NSD_CHECKALIVE_INTERVAL_MS = 10 * 1000;

    public static class RtspServerConstants {
        public final static String INTENT_PORT = "INTENT_PORT";
    }

    public final static int VIEWER_ID_LENGTH = 128;
    public final static int VIEWER_PASSCODE_LENGTH = 4;

    public final static long VIEWER_AUTH_EXP_MS = 1000 * 60;

    public final static String URI_TOKEN_PREFIX = "TOKEN_";

    public final static int PREVIEW_UPDATE_INTERVAL_MS = 60 * 1000;
}
