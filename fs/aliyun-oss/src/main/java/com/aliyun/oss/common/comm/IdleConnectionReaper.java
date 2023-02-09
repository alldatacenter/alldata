/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.comm;

import static com.aliyun.oss.common.utils.LogUtils.getLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.HttpClientConnectionManager;

/**
 * A daemon thread used to periodically check connection pools for idle
 * connections.
 */
public final class IdleConnectionReaper extends Thread {
    private static final int REAP_INTERVAL_MILLISECONDS = 5 * 1000;
    private static final ArrayList<HttpClientConnectionManager> connectionManagers = new ArrayList<HttpClientConnectionManager>();

    private static IdleConnectionReaper instance;

    private static long idleConnectionTime = 60 * 1000;

    private volatile boolean shuttingDown;

    private IdleConnectionReaper() {
        super("idle_connection_reaper");
        setDaemon(true);
    }

    public static synchronized boolean registerConnectionManager(HttpClientConnectionManager connectionManager) {
        if (instance == null) {
            instance = new IdleConnectionReaper();
            instance.start();
        }
        return connectionManagers.add(connectionManager);
    }

    public static synchronized boolean removeConnectionManager(HttpClientConnectionManager connectionManager) {
        boolean b = connectionManagers.remove(connectionManager);
        if (connectionManagers.isEmpty())
            shutdown();
        return b;
    }

    private void markShuttingDown() {
        shuttingDown = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        while (true) {
            if (shuttingDown) {
                getLog().debug("Shutting down reaper thread.");
                return;
            }

            try {
                Thread.sleep(REAP_INTERVAL_MILLISECONDS);
            } catch (InterruptedException e) {
            }

            try {
                List<HttpClientConnectionManager> connectionManagers = null;
                synchronized (IdleConnectionReaper.class) {
                    connectionManagers = (List<HttpClientConnectionManager>) IdleConnectionReaper.connectionManagers
                            .clone();
                }
                for (HttpClientConnectionManager connectionManager : connectionManagers) {
                    try {
                        connectionManager.closeExpiredConnections();
                        connectionManager.closeIdleConnections(idleConnectionTime, TimeUnit.MILLISECONDS);
                    } catch (Exception ex) {
                        getLog().warn("Unable to close idle connections", ex);
                    }
                }
            } catch (Throwable t) {
                getLog().debug("Reaper thread: ", t);
            }
        }
    }

    public static synchronized boolean shutdown() {
        if (instance != null) {
            instance.markShuttingDown();
            instance.interrupt();
            connectionManagers.clear();
            instance = null;
            return true;
        }
        return false;
    }

    public static synchronized int size() {
        return connectionManagers.size();
    }

    public static synchronized void setIdleConnectionTime(long idletime) {
        idleConnectionTime = idletime;
    }

}
