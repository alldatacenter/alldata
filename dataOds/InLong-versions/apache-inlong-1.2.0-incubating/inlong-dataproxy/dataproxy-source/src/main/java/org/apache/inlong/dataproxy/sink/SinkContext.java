/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.sink;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * SinkContext
 */
public class SinkContext {

    public static final Logger LOG = LoggerFactory.getLogger(SinkContext.class);

    public static final String KEY_MAX_THREADS = "maxThreads";
    public static final String KEY_PROCESS_INTERVAL = "processInterval";
    public static final String KEY_RELOAD_INTERVAL = "reloadInterval";

    protected final String clusterId;
    protected final String sinkName;
    protected final Context sinkContext;

    protected final Channel channel;

    protected final int maxThreads;
    protected final long processInterval;
    protected final long reloadInterval;

    protected final DataProxyMetricItemSet metricItemSet;
    protected Timer reloadTimer;

    /**
     * Constructor
     */
    public SinkContext(String sinkName, Context context, Channel channel) {
        this.sinkName = sinkName;
        this.sinkContext = context;
        this.channel = channel;
        this.clusterId = context.getString(CommonPropertiesHolder.KEY_PROXY_CLUSTER_NAME);
        this.maxThreads = sinkContext.getInteger(KEY_MAX_THREADS, 10);
        this.processInterval = sinkContext.getInteger(KEY_PROCESS_INTERVAL, 100);
        this.reloadInterval = sinkContext.getLong(KEY_RELOAD_INTERVAL, 60000L);
        //
        this.metricItemSet = new DataProxyMetricItemSet(sinkName);
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
                reload();
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
     * get clusterId
     *
     * @return the clusterId
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * get sinkName
     *
     * @return the sinkName
     */
    public String getSinkName() {
        return sinkName;
    }

    /**
     * get sinkContext
     *
     * @return the sinkContext
     */
    public Context getSinkContext() {
        return sinkContext;
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
     * get metricItemSet
     *
     * @return the metricItemSet
     */
    public DataProxyMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }
}
