/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb.context;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sdk.dataproxy.pb.metrics.SdkMetricItem;
import org.apache.inlong.sdk.dataproxy.pb.metrics.SdkMetricItemSet;
import org.apache.inlong.sdk.dataproxy.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SinkContext
 */
public class SinkContext {

    public static final Logger LOG = LoggerFactory.getLogger(SinkContext.class);

    public static final String KEY_NODE_ID = "nodeId";
    public static final String KEY_MAX_THREADS = "maxThreads";
    public static final String KEY_PROCESSINTERVAL = "processInterval";
    public static final String KEY_RELOADINTERVAL = "reloadInterval";
    public static final String KEY_AUDITFORMATINTERVAL = "auditFormatInterval";

    protected final String nodeId;
    protected final String nodeIp;
    protected final Context context;

    protected final Channel channel;
    // parameter
    protected final int maxThreads;
    protected final long processInterval;
    protected final long reloadInterval;
    protected long auditFormatInterval = 60000L;
    // metric
    protected final SdkMetricItemSet metricItemSet;
    // reload
    protected Timer reloadTimer;

    /**
     * Constructor
     * 
     * @param context
     * @param channel
     */
    public SinkContext(Context context, Channel channel) {
        this.nodeId = context.getString(KEY_NODE_ID, IpUtils.getLocalAddress());
        this.nodeIp = IpUtils.getLocalAddress();
        this.context = context;
        this.channel = channel;
        this.maxThreads = context.getInteger(KEY_MAX_THREADS, 10);
        this.processInterval = context.getInteger(KEY_PROCESSINTERVAL, 100);
        this.reloadInterval = context.getLong(KEY_RELOADINTERVAL, 60000L);
        this.auditFormatInterval = context.getLong(KEY_AUDITFORMATINTERVAL, 60000L);
        // metric
        this.metricItemSet = new SdkMetricItemSet(nodeId);
        MetricRegister.register(this.metricItemSet);
    }

    /**
     * start
     */
    public void start() {
        try {
            this.reload();
            this.setReloadTimer();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * close
     */
    public void close() {
        try {
            this.reloadTimer.cancel();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * setReloadTimer
     */
    protected void setReloadTimer() {
        reloadTimer = new Timer(true);
        TimerTask task = new TimerTask() {

            public void run() {
                try {
                    reload();
                } catch (Throwable e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        reloadTimer.schedule(task, new Date(System.currentTimeMillis() + reloadInterval), reloadInterval);
    }

    /**
     * reload
     */
    public void reload() {
    }

    /**
     * get nodeId
     * 
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * get context
     * 
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * get channel
     * 
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * get maxThreads
     * 
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * get processInterval
     * 
     * @return the processInterval
     */
    public long getProcessInterval() {
        return processInterval;
    }

    /**
     * get reloadInterval
     * 
     * @return the reloadInterval
     */
    public long getReloadInterval() {
        return reloadInterval;
    }

    /**
     * fillAuditFormatTime
     *
     * @param event
     * @param dimensions
     */
    public void fillAuditFormatTime(Event event, Map<String, String> dimensions) {
        long msgTime = SdkMetricItem.getLogTime(event);
        long auditFormatTime = msgTime - msgTime % auditFormatInterval;
        dimensions.put(SdkMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
    }

    /**
     * getAuditFormatTime
     *
     * @param  msgTime
     * @return
     */
    public long getAuditFormatTime(long msgTime) {
        long auditFormatTime = msgTime - msgTime % auditFormatInterval;
        return auditFormatTime;
    }

    /**
     * get metricItemSet
     * 
     * @return the metricItemSet
     */
    public SdkMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }
}
