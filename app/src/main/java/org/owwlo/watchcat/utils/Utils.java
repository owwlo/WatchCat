package org.owwlo.watchcat.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import org.owwlo.watchcat.services.CameraDaemon;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
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
     *
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

    public static int findAvaiablePort() {
        ServerSocket socket;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            Log.e(TAG,"Couldn't assign port to your service.");
            e.printStackTrace();
            return 0;
        }
    }

    public static String getCameraStreamingURI(String ip, int port)
    {
        return "rtsp://"  + ip + ":" + port + "/";
    }

    public static String getCameraPreviewURI(String ip, int port)
    {
        return "http://"  + ip + ":" + port + "/control/get_preview";
    }

    public static File getPreviewPath()
    {
        ContextWrapper contextWrapper = new ContextWrapper(sContext);
        return new File(contextWrapper.getExternalFilesDir(null), Constants.PREVIEW_FILENAME);
    }
}
