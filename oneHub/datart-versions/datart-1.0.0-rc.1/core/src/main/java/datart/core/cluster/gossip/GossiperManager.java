package datart.core.cluster.gossip;

import javax.management.Notification;
import javax.management.NotificationListener;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GossiperManager extends Gossiper implements NotificationListener {

    private List<Gossiper> remoteGossipers;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private static final GossiperManager INSTANCE = new GossiperManager();


    private GossiperManager() {
        super(INSTANCE);
    }

    public void startGossiper() {

        runReceiveThread();

    }

    @Override
    public void startTimer() {
        addNotificationListener(new SyncGossiperStatus(), null, this);
        addNotification("sync", "sync heartbeat", null, new Date(System.currentTimeMillis() + timeout));
    }

    public void resetTimer() {
        removeAllNotifications();
        addNotification("sync", "sync heartbeat", null, new Date(System.currentTimeMillis() + timeout));
    }

    private void runReceiveThread() {
        new Thread(() -> {

        }).start();
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {

    }


    public static GossiperManager getInstance() {
        return INSTANCE;
    }

    public List<Gossiper> getRemoteGossipers() {
        return remoteGossipers;
    }

    public void setRemoteGossipers(List<Gossiper> remoteGossipers) {
        this.remoteGossipers = remoteGossipers;
    }

    public AtomicBoolean getShutdown() {
        return shutdown;
    }
}
