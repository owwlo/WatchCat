package org.owwlo.watchcat.utils;

import android.content.Context;
import android.content.ContextWrapper;

import com.google.android.exoplayer2.util.Log;

import org.owwlo.watchcat.services.CameraDaemon;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.regex.Pattern;

public class Utils {
    private final static String TAG = CameraDaemon.class.getCanonicalName();

    public static Context sContext = null;

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                    "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     * @return True if the input parameter is a valid IPv4 address.
     */
    public static boolean isIPv4Address(String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    /**
     * Get local Ip address.
     */
    public static InetAddress getLocalIPAddress() {
        Enumeration<NetworkInterface> enumeration = null;
        try {
            enumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                NetworkInterface nif = enumeration.nextElement();
                Enumeration<InetAddress> inetAddresses = nif.getInetAddresses();
                if (inetAddresses != null) {
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (!inetAddress.isLoopbackAddress() && isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                    }
                }
            }
        }
        return null;
    }


    public static int getAvailablePort() {
        return getAvailablePort(0);
    }

    public static int getAvailablePort(final int defaultPort) {
        ServerSocket socket;
        int port = 0;
        try {
            socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            Log.d(TAG, "failed to get an available port.");
            e.printStackTrace();
            return 0;
        }
        return port;
    }


    public static String getCameraStreamingURI(String ip, int port) {
        return "rtsp://" + ip + ":" + port + "/";
    }

    public static String getCameraPreviewURI(String ip, int port) {
        return "http://" + ip + ":" + port + "/control/get_preview";
    }

    public static String getCameraInfoURI(final String ip, final int port) {
        return "http://" + ip + ":" + port + "/control/get_info";
    }

    public static String getClientUpdateURI(final String ip, final int port) {
        return "http://" + ip + ":" + port + "/client/update_status";
    }

    public static String getClientShuttingDownURI(final String ip, final int port) {
        return "http://" + ip + ":" + port + "/client/remove";
    }

    public static File getPreviewPath() {
        ContextWrapper contextWrapper = new ContextWrapper(sContext);
        return new File(contextWrapper.getExternalFilesDir(null), Constants.PREVIEW_FILENAME);
    }

    public static <T extends Iterable<String>> String join(String separator, T input) {
        Iterator itr = input.iterator();
        StringBuilder sb = new StringBuilder();
        while (itr.hasNext()) {
            sb.append(itr.next());
            if (itr.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }
}
