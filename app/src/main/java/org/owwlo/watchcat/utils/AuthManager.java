package org.owwlo.watchcat.utils;

import com.google.android.exoplayer2.util.Log;

import org.owwlo.watchcat.model.Viewer;
import org.owwlo.watchcat.model.ViewerDao;
import org.owwlo.watchcat.services.ServiceDaemon;
import org.owwlo.watchcat.utils.EventBus.IncomingAuthorizationRequestEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthManager {
    private final static String TAG = AuthManager.class.getCanonicalName();

    private ServiceDaemon mainService;
    private ViewerDao dao;

    private static class ViewerHolder {
        private Viewer viewer;

        private String passcode;
        private long expiration;

        public ViewerHolder(String id, String name) {
            viewer = Viewer.createClient(id, name);
            passcode = Utils.RandomStringGenerator.getPasscodeInstance().nextString();
            expiration = System.currentTimeMillis() + Constants.VIEWER_AUTH_EXP_MS;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiration;
        }

        public IncomingAuthorizationRequestEvent getAuthEvent() {
            return new IncomingAuthorizationRequestEvent(viewer.getName(), viewer.getId(), passcode);
        }

        public String getPasscode() {
            return passcode;
        }

        public long getExpiration() {
            return expiration;
        }

        public Viewer getViewer() {
            return viewer;
        }
    }

    private Map<String, ViewerHolder> pendingAuth = new HashMap<String, ViewerHolder>();

    public AuthManager(ServiceDaemon service) {
        mainService = service;
        dao = mainService.getDatabase().viewerDao();
        synchronized (dao) {
            if (dao.findMyself() == null) {
                dao.insertAll(Viewer.createSelf(Utils.RandomStringGenerator.getKeyInstance().nextString()));
            }
        }
    }

    public Viewer getMyself() {
        Viewer me = dao.findMyself();
        me.setName(Utils.getDeviceId());
        return me;
    }

    public boolean isAccessGranted(String incomingId) {
        Viewer viewer = dao.findClientById(incomingId);
        return viewer != null;
    }

    public IncomingAuthorizationRequestEvent newAuth(String incomingId, String name) {
        cleanUpExpired();
        if (pendingAuth.containsKey(incomingId)) {
            Log.d(TAG, "found unexpired auth attempt: " + incomingId);
            return pendingAuth.get(incomingId).getAuthEvent();
        }
        ViewerHolder vh = new ViewerHolder(incomingId, name);
        pendingAuth.put(incomingId, vh);
        Log.d(TAG, "new auth attempt: " + incomingId + " / " + name);
        return vh.getAuthEvent();
    }

    public Viewer auth(String incomingId, String passcode) {
        cleanUpExpired();
        if (!pendingAuth.containsKey(incomingId)) return null;
        ViewerHolder vh = pendingAuth.get(incomingId);
        if (vh.getPasscode().equals(passcode)) {
            Viewer viewer = vh.getViewer();
            dao.insertAll(viewer);
            pendingAuth.remove(incomingId);
            return viewer;
        }
        return null;
    }

    public void revokeAccess(String id) {
        dao.delete(Viewer.createFromKey(id));
    }

    private void cleanUpExpired() {
        for (Iterator<ViewerHolder> it = pendingAuth.values().iterator(); it.hasNext(); ) {
            if (it.next().isExpired()) {
                it.remove();
            }
        }
    }
}
