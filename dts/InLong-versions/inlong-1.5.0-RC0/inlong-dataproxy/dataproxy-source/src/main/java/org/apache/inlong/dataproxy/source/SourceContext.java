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

package org.apache.inlong.dataproxy.source;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Context;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.config.RemoteConfigManager;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.IdTopicConfigHolder;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.netty.channel.group.ChannelGroup;

/**
 * 
 * SinkContext
 */
public class SourceContext {

    public static final Logger LOG = LoggerFactory.getLogger(SourceContext.class);

    public static final String KEY_MAX_THREADS = "maxThreads";
    public static final String KEY_PROCESSINTERVAL = "processInterval";
    public static final String KEY_RELOADINTERVAL = "reloadInterval";
    public static final String CONNECTIONS = "connections";
    public static final String INLONG_HOST_IP = "inlongHostIp";
    public static final String INLONG_HOST_PORT = "inlongHostPort";

    protected AbstractSource source;
    protected ChannelGroup allChannels;
    protected String proxyClusterId;
    protected String sourceId;
    protected String sourceDataId;
    // config
    protected String hostIp;
    protected int hostPort;
    protected int maxThreads = 32;
    protected int maxConnections = Integer.MAX_VALUE;
    protected int maxMsgLength;
    // metric
    protected IdTopicConfigHolder idHolder;
    protected DataProxyMetricItemSet metricItemSet;
    // reload
    protected Context parentContext;
    protected long reloadInterval;
    protected Timer reloadTimer;
    // isRejectService
    protected boolean isRejectService = false;

    /**
     * Constructor
     * 
     * @param source
     * @param allChannels
     * @param context
     */
    public SourceContext(AbstractSource source, ChannelGroup allChannels, Context context) {
        this.source = source;
        this.allChannels = allChannels;
        this.proxyClusterId = CommonPropertiesHolder.get()
                .getOrDefault(RemoteConfigManager.KEY_PROXY_CLUSTER_NAME, "unknown");
        this.sourceId = source.getName();
        // metric
        this.metricItemSet = new DataProxyMetricItemSet(sourceId);
        MetricRegister.register(metricItemSet);
        // config
        this.maxConnections = context.getInteger(CONNECTIONS, 5000);
        this.maxThreads = context.getInteger(KEY_MAX_THREADS, 32);
        this.maxMsgLength = context.getInteger(ConfigConstants.MAX_MSG_LENGTH, 1024 * 64);
        Preconditions.checkArgument(
                (maxMsgLength >= 4 && maxMsgLength <= ConfigConstants.MSG_MAX_LENGTH_BYTES),
                "maxMsgLength must be >= 4 and <= " + ConfigConstants.MSG_MAX_LENGTH_BYTES);
        // port
        this.hostIp = this.getHostIp(context);
        this.hostPort = this.getHostPort(context);
        this.sourceDataId = String.valueOf(hostPort);
        // id topic
        this.idHolder = new IdTopicConfigHolder();
        this.idHolder.configure(context);
        //
        this.parentContext = context;
        this.reloadInterval = context.getLong(KEY_RELOADINTERVAL, 60000L);
    }

    /**
     * getHostIp
     * 
     * @param  context
     * @return
     */
    private String getHostIp(Context context) {
        Map<String, String> envMap = System.getenv();
        if (envMap.containsKey(INLONG_HOST_IP)) {
            String hostIp = envMap.get(INLONG_HOST_IP);
            return hostIp;
        }
        return context.getString(INLONG_HOST_IP);
    }

    /**
     * getHostPort
     * 
     * @param  context
     * @return
     */
    private int getHostPort(Context context) {
        Map<String, String> envMap = System.getenv();
        if (envMap.containsKey(INLONG_HOST_PORT)) {
            String strPort = envMap.get(INLONG_HOST_PORT);
            return NumberUtils.toInt(strPort, 0);
        }
        return context.getInteger(INLONG_HOST_IP, 0);
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
        try {
            this.idHolder.reload();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * get sourceDataId
     * 
     * @return the sourceDataId
     */
    public String getSourceDataId() {
        return sourceDataId;
    }

    /**
     * set sourceDataId
     * 
     * @param sourceDataId the sourceDataId to set
     */
    public void setSourceDataId(String sourceDataId) {
        this.sourceDataId = sourceDataId;
    }

    /**
     * get source
     * 
     * @return the source
     */
    public AbstractSource getSource() {
        return source;
    }

    /**
     * get allChannels
     * 
     * @return the allChannels
     */
    public ChannelGroup getAllChannels() {
        return allChannels;
    }

    /**
     * get proxyClusterId
     * 
     * @return the proxyClusterId
     */
    public String getProxyClusterId() {
        return proxyClusterId;
    }

    /**
     * get sourceId
     * 
     * @return the sourceId
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * get maxConnections
     * 
     * @return the maxConnections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * get maxMsgLength
     * 
     * @return the maxMsgLength
     */
    public int getMaxMsgLength() {
        return maxMsgLength;
    }

    /**
     * get idHolder
     * 
     * @return the idHolder
     */
    public IdTopicConfigHolder getIdHolder() {
        return idHolder;
    }

    /**
     * get metricItemSet
     * 
     * @return the metricItemSet
     */
    public DataProxyMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }

    /**
     * get parentContext
     * 
     * @return the parentContext
     */
    public Context getParentContext() {
        return parentContext;
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
     * get maxThreads
     * 
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * isRejectService
     * 
     * @return
     */
    public boolean isRejectService() {
        return isRejectService;
    }

    /**
     * setRejectService
     * 
     * @param isRejectService
     */
    public void setRejectService(boolean isRejectService) {
        this.isRejectService = isRejectService;
    }

}
