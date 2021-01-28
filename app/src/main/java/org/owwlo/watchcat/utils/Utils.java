package org.owwlo.watchcat.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import com.google.android.exoplayer2.util.Log;

import org.owwlo.watchcat.services.CameraDaemon;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
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


    public static class Urls {
        private final String ip;
        private final int controlPort;
        private final int streamingPort;

        private Urls(final String ip, final int controlPort, final int streamingPort) {
            this.ip = ip;
            this.controlPort = controlPort;
            this.streamingPort = streamingPort;
        }

        public static Urls getTarget(final String ip, final int controlPort, final int streamingPort) {
            return new Urls(ip, controlPort, streamingPort);
        }

        public static Urls getControlTarget(final String ip, final int controlPort) {
            return new Urls(ip, controlPort, 0);
        }

        public String getCameraStreamingURI(final String accessId) {
            return "rtsp://" + Constants.DEFAULT_RTSP_AUTH_USER + ":" + accessId + "@" + ip + ":" + streamingPort + "/";
        }

        public String getCameraPreviewURI(final long timestamp) {
            return "http://" + ip + ":" + controlPort + "/control/get_preview/" + timestamp;
        }

        public String getCameraInfoURI() {
            return "http://" + ip + ":" + controlPort + "/control/get_info";
        }

        public String getClientUpdateURI() {
            return "http://" + ip + ":" + controlPort + "/client/update_status";
        }

        public String getClientShuttingDownURI() {
            return "http://" + ip + ":" + controlPort + "/client/remove";
        }

        public String getAuthAttemptURI() {
            return "http://" + ip + ":" + controlPort + "/client/auth_request";
        }

        public String getPasscodeAuthURI() {
            return "http://" + ip + ":" + controlPort + "/client/auth";
        }
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

    public static String getHostname() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String hostName;
                try {
                    InetAddress netHost = InetAddress.getLocalHost();
                    hostName = netHost.getHostName();
                } catch (UnknownHostException ex) {
                    Log.d(TAG, "failed to get hostname for current device.");
                    hostName = null;
                }
                return hostName;
            }
        };
        task.execute();
        try {
            return task.get();
        } catch (Exception e) {
            return "";
        }
    }

    private static String getDeviceName() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null != bluetoothAdapter) {
            return bluetoothAdapter.getName();
        }
        final List<String> nameKeys = Arrays.asList("bluetooth_name", "device_name", "lock_screen_owner_info");
        for (final String key : nameKeys) {
            String pending = Settings.System.getString(sContext.getContentResolver(), key);
            if (pending != null) return pending;
            pending = Settings.Secure.getString(sContext.getContentResolver(), key);
            if (pending != null) return pending;
        }
        return "Unknown Device";
    }

    public static String getDeviceId() {
        return "[" + Build.MANUFACTURER + "]["
                + Build.MODEL + "]" + " " + getDeviceName();
    }

    public static class RandomStringGenerator {
        public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        public static final String lower = upper.toLowerCase(Locale.ROOT);
        public static final String digits = "0123456789";
        public static final String alphaNum = upper + lower + digits;
        private final Random random;
        private final char[] symbols;
        private final char[] buf;

        private static RandomStringGenerator sKeyInstance = null;
        private static RandomStringGenerator sPasscodeInstance = null;

        public static RandomStringGenerator getKeyInstance() {
            if (sKeyInstance == null) {
                sKeyInstance = new RandomStringGenerator(Constants.VIEWER_ID_LENGTH, alphaNum);
            }
            return sKeyInstance;
        }

        public static RandomStringGenerator getPasscodeInstance() {
            if (sPasscodeInstance == null) {
                sPasscodeInstance = new RandomStringGenerator(Constants.VIEWER_PASSCODE_LENGTH, digits);
            }
            return sPasscodeInstance;
        }

        public String nextString() {
            for (int idx = 0; idx < buf.length; ++idx)
                buf[idx] = symbols[random.nextInt(symbols.length)];
            return new String(buf);
        }

        public RandomStringGenerator(int length, Random random, String symbols) {
            if (length < 1) throw new IllegalArgumentException();
            if (symbols.length() < 2) throw new IllegalArgumentException();
            this.random = Objects.requireNonNull(random);
            this.symbols = symbols.toCharArray();
            this.buf = new char[length];
        }

        public RandomStringGenerator(int length, String charPool) {
            this(length, new SecureRandom(), charPool);
        }
    }

}
