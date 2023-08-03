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

import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.admin.ProxyServiceMBean;
import org.apache.inlong.dataproxy.channel.FailoverChannelProcessor;
import org.apache.inlong.dataproxy.config.CommonConfigHolder;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.metrics.stats.MonitorIndex;
import org.apache.inlong.dataproxy.metrics.stats.MonitorStats;
import org.apache.inlong.dataproxy.source.httpMsg.HttpMessageHandler;
import org.apache.inlong.dataproxy.utils.AddressUtils;
import org.apache.inlong.dataproxy.utils.ConfStringUtils;
import org.apache.inlong.dataproxy.utils.FailoverChannelProcessorHolder;
import org.apache.inlong.sdk.commons.admin.AdminServiceRegister;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.FlumeException;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * source base class
 */
public abstract class BaseSource
        extends
            AbstractSource
        implements
            ConfigUpdateCallback,
            ProxyServiceMBean,
            EventDrivenSource,
            Configurable {

    private static final Logger logger = LoggerFactory.getLogger(BaseSource.class);

    protected Context context;
    // whether source reject service
    protected volatile boolean isRejectService = false;
    // source service host
    protected String srcHost;
    // source serviced port
    protected int srcPort;
    protected String strPort;
    // message factory name
    protected String msgFactoryName;
    // message handler name
    protected String messageHandlerName;
    // source default append attribute
    protected String defAttr = "";
    // allowed max message length
    protected int maxMsgLength;
    // whether compress message
    protected boolean isCompressed;
    // whether filter empty message
    protected boolean filterEmptyMsg;
    // whether custom channel processor
    protected boolean customProcessor;
    // max netty worker threads
    protected int maxWorkerThreads;
    // max netty accept threads
    protected int maxAcceptThreads;
    // max read idle time
    protected long maxReadIdleTimeMs;
    // max connection count
    protected int maxConnections;
    // reuse address
    protected boolean reuseAddress;
    // connect backlog
    protected int conBacklog;
    // connect linger
    protected int conLinger = -1;
    // netty parameters
    protected EventLoopGroup acceptorGroup;
    protected EventLoopGroup workerGroup;
    protected ChannelGroup allChannels;
    protected ChannelFuture channelFuture;
    // receive buffer size
    protected int maxRcvBufferSize;
    // send buffer size
    protected int maxSendBufferSize;
    // file metric statistic
    private MonitorIndex monitorIndex = null;
    private MonitorStats monitorStats = null;
    // metric set
    private DataProxyMetricItemSet metricItemSet;

    public BaseSource() {
        super();
        allChannels = new DefaultChannelGroup("DefaultChannelGroup", GlobalEventExecutor.INSTANCE);
    }

    @Override
    public void configure(Context context) {
        logger.info("{} start to configure context:{}.", this.getName(), context.toString());
        this.context = context;
        this.srcHost = getHostIp(context);
        this.srcPort = getHostPort(context);
        this.strPort = String.valueOf(this.srcPort);
        // get message factory
        String tmpVal = context.getString(SourceConstants.SRCCXT_MSG_FACTORY_NAME,
                ServerMessageFactory.class.getName()).trim();
        Preconditions.checkArgument(StringUtils.isNotBlank(tmpVal),
                SourceConstants.SRCCXT_MSG_FACTORY_NAME + " config is blank");
        this.msgFactoryName = tmpVal.trim();
        // get message handler
        tmpVal = context.getString(SourceConstants.SRCCXT_MESSAGE_HANDLER_NAME);
        if (StringUtils.isBlank(tmpVal)) {
            tmpVal = SourceConstants.SRC_PROTOCOL_TYPE_HTTP.equalsIgnoreCase(getProtocolName())
                    ? HttpMessageHandler.class.getName()
                    : ServerMessageHandler.class.getName();
        }
        Preconditions.checkArgument(StringUtils.isNotBlank(tmpVal),
                SourceConstants.SRCCXT_MESSAGE_HANDLER_NAME + " config is blank");
        this.messageHandlerName = tmpVal;
        // get default attributes
        tmpVal = context.getString(SourceConstants.SRCCXT_DEF_ATTR);
        if (StringUtils.isNotBlank(tmpVal)) {
            this.defAttr = tmpVal.trim();
        }
        // get allowed max message length
        this.maxMsgLength = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_MAX_MSG_LENGTH, SourceConstants.VAL_DEF_MAX_MSG_LENGTH);
        Preconditions.checkArgument((this.maxMsgLength >= SourceConstants.VAL_MIN_MAX_MSG_LENGTH
                && this.maxMsgLength <= SourceConstants.VAL_MAX_MAX_MSG_LENGTH),
                SourceConstants.SRCCXT_MAX_MSG_LENGTH + " must be in ["
                        + SourceConstants.VAL_MIN_MAX_MSG_LENGTH + ", "
                        + SourceConstants.VAL_MAX_MAX_MSG_LENGTH + "]");
        // get whether compress message
        this.isCompressed = context.getBoolean(SourceConstants.SRCCXT_MSG_COMPRESSED,
                SourceConstants.VAL_DEF_MSG_COMPRESSED);
        // get whether filter empty message
        this.filterEmptyMsg = context.getBoolean(SourceConstants.SRCCXT_FILTER_EMPTY_MSG,
                SourceConstants.VAL_DEF_FILTER_EMPTY_MSG);
        // get whether custom channel processor
        this.customProcessor = context.getBoolean(SourceConstants.SRCCXT_CUSTOM_CHANNEL_PROCESSOR,
                SourceConstants.VAL_DEF_CUSTOM_CH_PROCESSOR);
        // get max accept threads
        this.maxAcceptThreads = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_MAX_ACCEPT_THREADS, SourceConstants.VAL_DEF_NET_ACCEPT_THREADS);
        Preconditions.checkArgument((this.maxAcceptThreads >= SourceConstants.VAL_MIN_ACCEPT_THREADS
                && this.maxAcceptThreads <= SourceConstants.VAL_MAX_ACCEPT_THREADS),
                SourceConstants.SRCCXT_MAX_ACCEPT_THREADS + " must be in ["
                        + SourceConstants.VAL_MIN_ACCEPT_THREADS + ", "
                        + SourceConstants.VAL_MAX_ACCEPT_THREADS + "]");
        // get max worker threads
        this.maxWorkerThreads = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_MAX_WORKER_THREADS, SourceConstants.VAL_DEF_WORKER_THREADS);
        Preconditions.checkArgument((this.maxWorkerThreads >= SourceConstants.VAL_MIN_WORKER_THREADS),
                SourceConstants.SRCCXT_MAX_WORKER_THREADS + " must be >= "
                        + SourceConstants.VAL_MIN_WORKER_THREADS);
        // get max read idle time
        this.maxReadIdleTimeMs = ConfStringUtils.getLongValue(context,
                SourceConstants.SRCCXT_MAX_READ_IDLE_TIME_MS, SourceConstants.VAL_DEF_READ_IDLE_TIME_MS);
        Preconditions.checkArgument((this.maxReadIdleTimeMs >= SourceConstants.VAL_MIN_READ_IDLE_TIME_MS
                && this.maxReadIdleTimeMs <= SourceConstants.VAL_MAX_READ_IDLE_TIME_MS),
                SourceConstants.SRCCXT_MAX_READ_IDLE_TIME_MS + " must be in ["
                        + SourceConstants.VAL_MIN_READ_IDLE_TIME_MS + ", "
                        + SourceConstants.VAL_MAX_READ_IDLE_TIME_MS + "]");
        // get max connect count
        this.maxConnections = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_MAX_CONNECTION_CNT, SourceConstants.VAL_DEF_MAX_CONNECTION_CNT);
        Preconditions.checkArgument(this.maxConnections >= SourceConstants.VAL_MIN_CONNECTION_CNT,
                SourceConstants.SRCCXT_MAX_CONNECTION_CNT + " must be >= "
                        + SourceConstants.VAL_MIN_CONNECTION_CNT);
        // get connect backlog
        this.conBacklog = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_CONN_BACKLOG, SourceConstants.VAL_DEF_CONN_BACKLOG);
        Preconditions.checkArgument(this.conBacklog >= SourceConstants.VAL_MIN_CONN_BACKLOG,
                SourceConstants.SRCCXT_CONN_BACKLOG + " must be >= "
                        + SourceConstants.VAL_MIN_CONN_BACKLOG);
        // get connect linger
        Integer tmpValue = context.getInteger(SourceConstants.SRCCXT_CONN_LINGER);
        if (tmpValue != null && tmpValue >= 0) {
            this.conLinger = tmpValue;
        }
        // get whether reuse address
        this.reuseAddress = context.getBoolean(SourceConstants.SRCCXT_REUSE_ADDRESS,
                SourceConstants.VAL_DEF_REUSE_ADDRESS);

        // get whether custom channel processor
        this.customProcessor = context.getBoolean(SourceConstants.SRCCXT_CUSTOM_CHANNEL_PROCESSOR,
                SourceConstants.VAL_DEF_CUSTOM_CH_PROCESSOR);
        // get max receive buffer size
        this.maxRcvBufferSize = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_RECEIVE_BUFFER_SIZE, SourceConstants.VAL_DEF_RECEIVE_BUFFER_SIZE);
        Preconditions.checkArgument(this.maxRcvBufferSize >= SourceConstants.VAL_MIN_RECEIVE_BUFFER_SIZE,
                SourceConstants.SRCCXT_RECEIVE_BUFFER_SIZE + " must be >= "
                        + SourceConstants.VAL_MIN_RECEIVE_BUFFER_SIZE);
        // get max send buffer size
        this.maxSendBufferSize = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_SEND_BUFFER_SIZE, SourceConstants.VAL_DEF_SEND_BUFFER_SIZE);
        Preconditions.checkArgument(this.maxSendBufferSize >= SourceConstants.VAL_MIN_SEND_BUFFER_SIZE,
                SourceConstants.SRCCXT_SEND_BUFFER_SIZE + " must be >= "
                        + SourceConstants.VAL_MIN_SEND_BUFFER_SIZE);
    }

    @Override
    public synchronized void start() {
        if (customProcessor) {
            ChannelSelector selector = getChannelProcessor().getSelector();
            FailoverChannelProcessor newProcessor = new FailoverChannelProcessor(selector);
            newProcessor.configure(this.context);
            setChannelProcessor(newProcessor);
            FailoverChannelProcessorHolder.setChannelProcessor(newProcessor);
        }
        super.start();
        // initial metric item set
        this.metricItemSet = new DataProxyMetricItemSet(
                CommonConfigHolder.getInstance().getClusterName(), getName(), String.valueOf(srcPort));
        MetricRegister.register(metricItemSet);
        // init monitor logic
        if (CommonConfigHolder.getInstance().isEnableFileMetric()) {
            this.monitorIndex = new MonitorIndex(CommonConfigHolder.getInstance().getFileMetricSourceOutName(),
                    CommonConfigHolder.getInstance().getFileMetricStatInvlSec() * 1000L,
                    CommonConfigHolder.getInstance().getFileMetricStatCacheCnt());
            this.monitorIndex.start();
            this.monitorStats = new MonitorStats(
                    CommonConfigHolder.getInstance().getFileMetricEventOutName()
                            + AttrConstants.SEP_HASHTAG + this.getProtocolName(),
                    CommonConfigHolder.getInstance().getFileMetricStatInvlSec() * 1000L,
                    CommonConfigHolder.getInstance().getFileMetricStatCacheCnt());
            this.monitorStats.start();
        }
        startSource();
        // register
        AdminServiceRegister.register(ProxyServiceMBean.MBEAN_TYPE, this.getName(), this);
    }

    @Override
    public synchronized void stop() {
        logger.info("[STOP {} SOURCE]{} stopping...", this.getProtocolName(), this.getName());
        // close channels
        if (!allChannels.isEmpty()) {
            try {
                allChannels.close().awaitUninterruptibly();
            } catch (Exception e) {
                logger.warn("Close {} netty channels throw exception", this.getName(), e);
            } finally {
                allChannels.clear();
            }
        }
        // close channel future
        if (channelFuture != null) {
            try {
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                logger.warn("Close {} channel future throw exception", this.getName(), e);
            }
        }
        // stop super class
        super.stop();
        // stop workers
        if (this.acceptorGroup != null) {
            this.acceptorGroup.shutdownGracefully();
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
        }
        // stop file statistic index
        if (CommonConfigHolder.getInstance().isEnableFileMetric()) {
            if (monitorIndex != null) {
                monitorIndex.stop();
            }
            if (monitorStats != null) {
                monitorStats.stop();
            }
        }
        logger.info("[STOP {} SOURCE]{} stopped", this.getProtocolName(), this.getName());
    }

    @Override
    public void update() {
        // check current all links
        if (ConfigManager.getInstance().needChkIllegalIP()) {
            int cnt = 0;
            Channel channel;
            String strRemoteIP;
            long startTime = System.currentTimeMillis();
            Iterator<Channel> iterator = allChannels.iterator();
            while (iterator.hasNext()) {
                channel = iterator.next();
                strRemoteIP = AddressUtils.getChannelRemoteIP(channel);
                if (strRemoteIP == null) {
                    continue;
                }
                if (ConfigManager.getInstance().isIllegalIP(strRemoteIP)) {
                    channel.disconnect();
                    channel.close();
                    allChannels.remove(channel);
                    cnt++;
                    logger.error(strRemoteIP + " is Illegal IP, so disconnect it !");
                }
            }
            logger.info("Source {} channel check, disconnects {} Illegal channels, waist {} ms",
                    getName(), cnt, (System.currentTimeMillis() - startTime));
        }
    }

    /**
     * get metricItemSet
     *
     * @return the metricItemSet
     */
    public DataProxyMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }

    public Context getContext() {
        return context;
    }

    public String getSrcHost() {
        return srcHost;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public String getStrPort() {
        return strPort;
    }

    public String getDefAttr() {
        return defAttr;
    }

    public int getMaxMsgLength() {
        return maxMsgLength;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public boolean isFilterEmptyMsg() {
        return filterEmptyMsg;
    }

    public boolean isCustomProcessor() {
        return customProcessor;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public ChannelGroup getAllChannels() {
        return allChannels;
    }

    public long getMaxReadIdleTimeMs() {
        return maxReadIdleTimeMs;
    }

    public String getMessageHandlerName() {
        return messageHandlerName;
    }

    public int getMaxWorkerThreads() {
        return maxWorkerThreads;
    }

    public void fileMetricIncSumStats(String eventKey) {
        if (CommonConfigHolder.getInstance().isEnableFileMetric()) {
            monitorStats.incSumStats(eventKey);
        }
    }

    public void fileMetricIncDetailStats(String eventKey) {
        if (CommonConfigHolder.getInstance().isEnableFileMetric()) {
            monitorStats.incDetailStats(eventKey);
        }
    }

    public void fileMetricAddSuccCnt(String key, int cnt, int packCnt, long packSize) {
        if (CommonConfigHolder.getInstance().isEnableFileMetric()) {
            monitorIndex.addSuccStats(key, cnt, packCnt, packSize);
        }
    }

    public void fileMetricAddFailCnt(String key, int failCnt) {
        if (CommonConfigHolder.getInstance().isEnableFileMetric()) {
            monitorIndex.addFailStats(key, failCnt);
        }
    }
    /**
     * addMetric
     *
     * @param result
     * @param size
     * @param event
     */
    public void addMetric(boolean result, long size, Event event) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, CommonConfigHolder.getInstance().getClusterName());
        dimensions.put(DataProxyMetricItem.KEY_SOURCE_ID, getName());
        dimensions.put(DataProxyMetricItem.KEY_SOURCE_DATA_ID, getStrPort());
        DataProxyMetricItem.fillInlongId(event, dimensions);
        DataProxyMetricItem.fillAuditFormatTime(event, dimensions);
        DataProxyMetricItem metricItem = metricItemSet.findMetricItem(dimensions);
        if (result) {
            metricItem.readSuccessCount.incrementAndGet();
            metricItem.readSuccessSize.addAndGet(size);
            AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_READ_SUCCESS, event);
        } else {
            metricItem.readFailCount.incrementAndGet();
            metricItem.readFailSize.addAndGet(size);
        }
    }

    /**
     * channel factory
     *
     * @return
     */
    public ChannelInitializer getChannelInitializerFactory() {
        ChannelInitializer fac = null;
        logger.info(this.getName() + " load msgFactory=" + msgFactoryName);
        try {
            Class<? extends ChannelInitializer> clazz =
                    (Class<? extends ChannelInitializer>) Class.forName(msgFactoryName);
            Constructor ctor = clazz.getConstructor(BaseSource.class);
            logger.info("Using channel processor:{}", getChannelProcessor().getClass().getName());
            fac = (ChannelInitializer) ctor.newInstance(this);
        } catch (Exception e) {
            logger.error("{} start error, fail to construct ChannelPipelineFactory with name {}",
                    this.getName(), msgFactoryName, e);
            stop();
            throw new FlumeException(e.getMessage());
        }
        return fac;
    }

    public abstract String getProtocolName();

    public abstract void startSource();

    /**
     * stopService
     */
    @Override
    public void stopService() {
        this.isRejectService = true;
    }

    /**
     * recoverService
     */
    @Override
    public void recoverService() {
        this.isRejectService = false;
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
     * getHostIp
     *
     * @param context
     * @return
     */
    private String getHostIp(Context context) {
        String result = null;
        // first get host ip from dataProxy.conf
        String tmpVal = context.getString(SourceConstants.SRCCXT_CONFIG_HOST);
        if (StringUtils.isNotBlank(tmpVal)) {
            tmpVal = tmpVal.trim();
            Preconditions.checkArgument(ConfStringUtils.isValidIp(tmpVal),
                    SourceConstants.SRCCXT_CONFIG_HOST + "(" + tmpVal + ") config in conf not valid");
            result = tmpVal;
        }
        // second get host ip from system env
        Map<String, String> envMap = System.getenv();
        if (envMap.containsKey(SourceConstants.SYSENV_HOST_IP)) {
            tmpVal = envMap.get(SourceConstants.SYSENV_HOST_IP);
            Preconditions.checkArgument(ConfStringUtils.isValidIp(tmpVal),
                    SourceConstants.SYSENV_HOST_IP + "(" + tmpVal + ") config in system env not valid");
            result = tmpVal.trim();
        }
        if (StringUtils.isBlank(result)) {
            result = SourceConstants.VAL_DEF_HOST_VALUE;
        }
        return result;
    }

    /**
     * getHostPort
     *
     * @param context
     * @return
     */
    private int getHostPort(Context context) {
        Integer result = null;
        // first get host port from dataProxy.conf
        String tmpVal = context.getString(SourceConstants.SRCCXT_CONFIG_PORT);
        if (StringUtils.isNotBlank(tmpVal)) {
            tmpVal = tmpVal.trim();
            try {
                result = Integer.parseInt(tmpVal);
            } catch (Throwable e) {
                throw new IllegalArgumentException(
                        SourceConstants.SYSENV_HOST_PORT + "(" + tmpVal + ") config in conf not integer");
            }
        }
        if (result != null) {
            Preconditions.checkArgument(ConfStringUtils.isValidPort(result),
                    SourceConstants.SRCCXT_CONFIG_PORT + "(" + result + ") config in conf not valid");
        }
        // second get host port from system env
        Map<String, String> envMap = System.getenv();
        if (envMap.containsKey(SourceConstants.SYSENV_HOST_PORT)) {
            tmpVal = envMap.get(SourceConstants.SYSENV_HOST_PORT);
            if (StringUtils.isNotBlank(tmpVal)) {
                tmpVal = tmpVal.trim();
                try {
                    result = Integer.parseInt(tmpVal);
                } catch (Throwable e) {
                    throw new IllegalArgumentException(
                            SourceConstants.SYSENV_HOST_PORT + "(" + tmpVal + ") config in system env not integer");
                }
                Preconditions.checkArgument(ConfStringUtils.isValidPort(result),
                        SourceConstants.SYSENV_HOST_PORT + "(" + tmpVal + ") config in system env not valid");
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("Required parameter " +
                    SourceConstants.SRCCXT_CONFIG_PORT + " must exist and may not be null");
        }
        return result;
    }

}
