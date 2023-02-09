/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.http;

import java.util.concurrent.TimeUnit;
import org.apache.http.conn.HttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//用于监控空闲的连接池连接
public final class IdleConnectionMonitorThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(IdleConnectionMonitorThread.class);
    private final HttpClientConnectionManager connMgr;
    private volatile boolean shutdown;

    private static final int MONITOR_INTERVAL_MS = 2000;
    private static final int IDLE_ALIVE_MS = 5000;

    public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
        super();
        this.connMgr = connMgr;
        this.shutdown = false;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(MONITOR_INTERVAL_MS);
                    // 关闭无效的连接
                    connMgr.closeExpiredConnections();
                    // 关闭空闲时间超过IDLE_ALIVE_MS的连接
                    connMgr.closeIdleConnections(IDLE_ALIVE_MS, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException e) {
            log.error("interrupt exception occured:", e);
        } finally {
            connMgr.shutdown();
        }
    }

    // 关闭后台连接
    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }

}
