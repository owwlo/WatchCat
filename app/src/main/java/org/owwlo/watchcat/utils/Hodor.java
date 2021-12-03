package org.owwlo.watchcat.utils;

import java.util.HashMap;
import java.util.Map;

public class Hodor {
    public static final int TTL_MS = 5 * 1000;

    public int maxViewer;
    public Map<String, Entry> dict = new HashMap<>();

    public static class Entry {
        public long expire;
        public String accessKey;

        public Entry(String key) {
            expire = System.currentTimeMillis() + TTL_MS;
            accessKey = key;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expire;
        }

        public void makePermanent() {
            expire = Long.MAX_VALUE;
        }
    }

    public Hodor(int maxViewer) {
        this.maxViewer = maxViewer;
    }

    private synchronized void cleanUpExpired() {
        for (String key : dict.keySet()) {
            Entry val = dict.get(key);
            if (val.isExpired()) {
                dict.remove(key);
            }
        }
    }

    public synchronized boolean register(String key) {
        cleanUpExpired();

        // The order of the following lines has to be this.
        // Currently, camera+encoding are done in the same thread.
        // >1 threads cannot be used simultaneously to read from camera.
        if (dict.size() + 1 > maxViewer) return false;
        if (dict.containsKey(key)) return true;

        dict.put(key, new Entry(key));
        return true;
    }

    public synchronized boolean isAllow(String key) {
        cleanUpExpired();
        return dict.containsKey(key);
    }

    public synchronized void makePermanent(String key) {
        Entry entry = new Entry(key);
        entry.makePermanent();
        dict.put(key, entry);
    }

    public synchronized void remove(String key) {
        dict.remove(key);
    }
}
