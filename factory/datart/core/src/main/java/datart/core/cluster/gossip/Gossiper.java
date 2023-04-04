/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.core.cluster.gossip;

import javax.management.timer.Timer;
import java.util.Date;
import java.util.Objects;

public class Gossiper extends Timer {

    protected String host;

    protected int port;

    protected String jsonData;

    protected final GossiperManager manager;

    protected long timeout;

    public Gossiper(GossiperManager manager, String host, int port, long timeout) {
        this.host = host;
        this.port = port;
        this.manager = manager;
        this.timeout = timeout;
    }

    public Gossiper(Gossiper gossiper) {
        this.host = gossiper.host;
        this.port = gossiper.port;
        this.timeout = gossiper.timeout;
        this.manager = gossiper.manager;
        this.jsonData = gossiper.jsonData;
    }

    @Override
    public synchronized void start() {
        startTimer();
        super.start();
    }

    public void startTimer() {
        addNotificationListener(manager, null, this);
    }

    public void resetHeartbeat() {
        removeAllNotifications();
        addNotification("", "", null, new Date(System.currentTimeMillis() + timeout));
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public GossiperManager getManager() {
        return manager;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gossiper that = (Gossiper) o;
        return port == that.port && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}