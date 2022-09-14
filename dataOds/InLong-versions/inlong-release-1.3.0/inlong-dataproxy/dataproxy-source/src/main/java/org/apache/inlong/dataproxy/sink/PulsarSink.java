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

import static org.apache.inlong.dataproxy.consts.ConfigConstants.MAX_MONITOR_CNT;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import io.netty.handler.codec.TooLongFrameException;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.dataproxy.base.HighPriorityThreadFactory;
import org.apache.inlong.dataproxy.base.OrderEvent;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.sink.pulsar.CreatePulsarClientCallBack;
import org.apache.inlong.dataproxy.sink.pulsar.PulsarClientService;
import org.apache.inlong.dataproxy.sink.pulsar.SendMessageCallBack;
import org.apache.inlong.dataproxy.sink.pulsar.SinkTask;
import org.apache.inlong.dataproxy.utils.FailoverChannelProcessorHolder;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.apache.inlong.dataproxy.utils.NetworkUtils;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.PulsarClientException.AlreadyClosedException;
import org.apache.pulsar.client.api.PulsarClientException.NotFoundException;
import org.apache.pulsar.client.api.PulsarClientException.ProducerQueueIsFullError;
import org.apache.pulsar.client.api.PulsarClientException.TopicTerminatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Use pulsarSink need adding such config, if these ara not config in dataproxy-pulsar.conf,
 * PulsarSink will use default value.
 * <p/>
 *
 * Prefix of Pulsar sink config in flume.conf like this XXX.sinks.XXX.property and properties are may these
 * configurations:
 * <p/>
 * <code>type</code> (*): value must be 'org.apache.inlong.dataproxy.sink.PulsarSink'
 * <p/>
 * <code>pulsar_server_url_list</code> (*): value is pulsar broker url, like 'pulsar://127.0.0.1:6650'
 * <p/>
 * <code>send_timeout_MILL</code>: send message timeout, unit is millisecond, default is 30000 (mean 30s)
 * <p/>
 * <code>stat_interval_sec</code>: stat info will be made period time, unit is second, default is 60s
 * <p/>
 * <code>thread-num</code>: sink thread num, default is 8
 * <p/>
 * <code>client-id-cache</code>: whether the client uses cache, default is true
 * <p/>
 * <code>max_pending_messages</code>: default is 10000
 * <p/>
 * <code>max_batching_messages</code>: default is 1000
 * <p/>
 * <code>enable_batch</code>: default is true
 * <p/>
 * <code>block_if_queue_full</code>: default is true
 */
public class PulsarSink extends AbstractSink implements Configurable, SendMessageCallBack, CreatePulsarClientCallBack {

    private static final Logger logger = LoggerFactory.getLogger(PulsarSink.class);
    /**
     * log tools
     */
    private static final LogCounter logPrinterB = new LogCounter(10, 100000, 60 * 1000);
    private static final LogCounter logPrinterC = new LogCounter(10, 100000, 60 * 1000);
    private static final String SEPARATOR = "#";
    private static final Long PRINT_INTERVAL = 30L;

    private static final PulsarPerformanceTask PULSAR_PERFORMANCE_TASK = new PulsarPerformanceTask();
    private static final LoadingCache<String, Long> AGENT_ID_CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(4 * 8).initialCapacity(500).expireAfterAccess(30, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Long>() {
                @Nonnull
                @Override
                public Long load(@Nonnull String key) {
                    return System.currentTimeMillis();
                }
            });
    /*
     * properties for header info
     */
    private static final String TOPIC = "topic";
    /*
     * for stat
     */
    private static final AtomicLong TOTAL_PULSAR_SUCC_SEND_CNT = new AtomicLong(0);
    private static final AtomicLong TOTAL_PULSAR_SUCC_SEND_SIZE = new AtomicLong(0);
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors
            .newScheduledThreadPool(1, new HighPriorityThreadFactory("pulsarPerformance-Printer-thread"));

    static {
        SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(PULSAR_PERFORMANCE_TASK, 0L,
                PRINT_INTERVAL, TimeUnit.SECONDS);
        logger.info("success to start pulsar performance task");
    }

    private final AtomicLong currentInFlightCount = new AtomicLong(0);
    private final AtomicLong currentSuccessSendCnt = new AtomicLong(0);
    private final AtomicLong lastSuccessSendCnt = new AtomicLong(0);
    private final AtomicInteger processIndex = new AtomicInteger(0);

    private RateLimiter diskRateLimiter;
    private long t1 = System.currentTimeMillis();
    private int maxMonitorCnt = 300000;
    /*
     * Control whether the SinkRunner thread can read data from the Channel
     */
    private volatile boolean canTake = false;
    private SinkCounter sinkCounter;
    /*
     * message queue and retry
     */
    private int eventQueueSize = 10000;
    private int badEventQueueSize = 10000;
    private int maxRetrySendCnt = 16;
    /*
     * send thread pool
     */
    private SinkTask[] sinkThreadPool;
    private int sinkThreadPoolSize;
    private PulsarClientService pulsarClientService;
    /*
     * statistic info log
     */
    private MonitorIndex monitorIndex;
    private MonitorIndexExt monitorIndexExt;

    /*
     *  metric
     */
    private Map<String, String> dimensions;
    private DataProxyMetricItemSet metricItemSet;
    private ConfigManager configManager;
    private Map<String, String> topicProperties;
    private Map<String, String> pulsarCluster;
    private MQClusterConfig pulsarConfig;

    public PulsarSink() {
        super();
        logger.debug("new instance of PulsarSink!");
    }

    /**
     * configure
     */
    @Override
    public void configure(Context context) {
        logger.info("PulsarSink started and context = {}", context.toString());
        maxMonitorCnt = context.getInteger(MAX_MONITOR_CNT, 300000);

        configManager = ConfigManager.getInstance();
        topicProperties = configManager.getTopicProperties();
        pulsarCluster = configManager.getMqClusterUrl2Token();
        pulsarConfig = configManager.getMqClusterConfig(); //pulsar common config
        Map<String, String> commonProperties = configManager.getCommonProperties();
        sinkThreadPoolSize = pulsarConfig.getThreadNum();
        if (sinkThreadPoolSize <= 0) {
            sinkThreadPoolSize = 1;
        }
        pulsarClientService = new PulsarClientService(pulsarConfig, sinkThreadPoolSize);

        configManager.getTopicConfig().addUpdateCallback(new ConfigUpdateCallback() {
            @Override
            public void update() {
                if (pulsarClientService != null) {
                    diffSetPublish(pulsarClientService, new HashSet<>(topicProperties.values()),
                            new HashSet<>(configManager.getTopicProperties().values()));
                }
            }
        });
        configManager.getMqClusterHolder().addUpdateCallback(new ConfigUpdateCallback() {
            @Override
            public void update() {
                if (pulsarClientService != null) {
                    diffUpdatePulsarClient(pulsarClientService, pulsarCluster, configManager.getMqClusterUrl2Token());
                }
            }
        });
        maxRetrySendCnt = pulsarConfig.getMaxRetryCnt();
        badEventQueueSize = pulsarConfig.getBadEventQueueSize();
        Preconditions.checkArgument(pulsarConfig.getThreadNum() > 0, "threadNum must be > 0");
        sinkThreadPool = new SinkTask[sinkThreadPoolSize];
        eventQueueSize = pulsarConfig.getEventQueueSize();
        if (pulsarConfig.getDiskIoRatePerSec() != 0) {
            diskRateLimiter = RateLimiter.create(pulsarConfig.getDiskIoRatePerSec());
        }

        if (sinkCounter == null) {
            sinkCounter = new SinkCounter(getName());
        }
    }

    private void initTopicSet(PulsarClientService pulsarClientService, Set<String> topicSet) {
        long startTime = System.currentTimeMillis();
        if (topicSet != null) {
            for (String topic : topicSet) {
                pulsarClientService.initTopicProducer(topic);
            }
        }
        logger.info(getName() + " initTopicSet cost: " + (System.currentTimeMillis() - startTime) + "ms");
        logger.info(getName() + " producer is ready for topics: " + pulsarClientService.getProducerInfoMap().keySet());
    }

    /**
     * When topic.properties is re-enabled, the producer update is triggered
     */
    public void diffSetPublish(PulsarClientService pulsarClientService, Set<String> originalSet, Set<String> endSet) {
        boolean changed = false;
        for (String s : endSet) {
            if (!originalSet.contains(s)) {
                changed = true;
                try {
                    pulsarClientService.initTopicProducer(s);
                } catch (Exception e) {
                    logger.error("get producer failed: ", e);
                }
            }
        }
        if (changed) {
            logger.info("topics.properties has changed, trigger diff publish for {}", getName());
            topicProperties = configManager.getTopicProperties();
        }
    }

    /**
     * When pulsarURLList change, close and restart
     */
    public void diffUpdatePulsarClient(PulsarClientService pulsarClientService, Map<String, String> originalCluster,
            Map<String, String> endCluster) {
        MapDifference<String, String> mapDifference = Maps.difference(originalCluster, endCluster);
        if (mapDifference.areEqual()) {
            return;
        }

        logger.info("pulsarConfig has changed, close unused url clients and start new url clients");
        Map<String, String> needToClose = new HashMap<>(mapDifference.entriesOnlyOnLeft());
        Map<String, String> needToStart = new HashMap<>(mapDifference.entriesOnlyOnRight());
        Map<String, MapDifference.ValueDifference<String>> differentToken = mapDifference.entriesDiffering();
        for (String url : differentToken.keySet()) {
            needToClose.put(url, originalCluster.get(url));
            needToStart.put(url, endCluster.get(url));//token changed
        }

        pulsarClientService.updatePulsarClients(this, needToClose, needToStart,
                new HashSet<>(topicProperties.values()));

        pulsarCluster = configManager.getMqClusterUrl2Token();
    }

    @Override
    public void start() {
        logger.info("[{}] pulsar sink starting...", getName());
        // register metrics
        this.dimensions = new HashMap<>();
        this.dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, "DataProxy");
        this.dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getName());

        sinkCounter.start();
        pulsarClientService.initCreateConnection(this);

        int statIntervalSec = pulsarConfig.getStatIntervalSec();
        Preconditions.checkArgument(statIntervalSec >= 0, "statIntervalSec must be >= 0");
        if (statIntervalSec > 0) {
            // switch for lots of metrics
            monitorIndex = new MonitorIndex("Pulsar_Sink", statIntervalSec, maxMonitorCnt);
            monitorIndexExt = new MonitorIndexExt("Pulsar_Sink_monitors#" + this.getName(),
                    statIntervalSec, maxMonitorCnt);
        }

        super.start();

        try {
            initTopicSet(pulsarClientService, new HashSet<String>(topicProperties.values()));
        } catch (Exception e) {
            logger.info("pulsar sink start publish topic fail.", e);
        }

        for (int i = 0; i < sinkThreadPoolSize; i++) {
            sinkThreadPool[i] = new SinkTask(pulsarClientService, this,
                    eventQueueSize / sinkThreadPoolSize,
                    badEventQueueSize / sinkThreadPoolSize, i, true);
            sinkThreadPool[i].setName(getName() + "_pulsar_sink_sender-" + i);
            sinkThreadPool[i].start();
        }

        this.metricItemSet = new DataProxyMetricItemSet(this.getName());
        MetricRegister.register(metricItemSet);
        this.canTake = true;
        logger.info("[{}] Pulsar sink started", getName());
    }

    @Override
    public void stop() {
        logger.info("pulsar sink stopping");
        this.canTake = false;
        int waitCount = 0;
        while (isAllSendFinished() && waitCount++ < 10) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("Stop thread has been interrupt!");
                break;
            }
        }
        if (pulsarConfig.getStatIntervalSec() > 0) {
            try {
                monitorIndex.shutDown();
            } catch (Exception e) {
                logger.warn("stat runner interrupted");
            }
        }
        if (pulsarClientService != null) {
            pulsarClientService.close();
        }
        if (sinkThreadPool != null) {
            for (SinkTask thread : sinkThreadPool) {
                if (thread != null) {
                    thread.close();
                    thread.interrupt();
                }
            }
            sinkThreadPool = null;
        }

        super.stop();
        if (!SCHEDULED_EXECUTOR_SERVICE.isShutdown()) {
            SCHEDULED_EXECUTOR_SERVICE.shutdown();
        }
        sinkCounter.stop();
        logger.debug("pulsar sink stopped. Metrics:{}", sinkCounter);
    }

    private boolean isAllSendFinished() {
        for (int i = 0; i < sinkThreadPoolSize; i++) {
            if (!sinkThreadPool[i].isAllSendFinished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Status process() throws EventDeliveryException {
        if (!this.canTake) {
            return Status.BACKOFF;
        }

        Status status = Status.READY;
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            Event event = channel.take();
            if (event != null) {
                if (diskRateLimiter != null) {
                    diskRateLimiter.acquire(event.getBody().length);
                }
                if (!processEvent(new EventStat(event))) {
                    logger.info("[{}] Channel --> Queue(has no enough space,current code point) "
                            + "--> pulsar,Check if pulsar server or network is ok.(if this situation "
                            + "last long time it will cause memoryChannel full and fileChannel write.)", getName());
                    tx.rollback();
                    // metric
                    dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, event.getHeaders().getOrDefault(TOPIC, ""));
                    DataProxyMetricItem metricItem = this.metricItemSet.findMetricItem(dimensions);
                    metricItem.readFailCount.incrementAndGet();
                    metricItem.readFailSize.addAndGet(event.getBody().length);
                } else {
                    tx.commit();
                    // metric
                    dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, event.getHeaders().getOrDefault(TOPIC, ""));
                    DataProxyMetricItem metricItem = this.metricItemSet.findMetricItem(dimensions);
                    metricItem.readSuccessCount.incrementAndGet();
                    metricItem.readFailSize.addAndGet(event.getBody().length);
                }
            } else {
                status = Status.BACKOFF;
                tx.commit();
            }
        } catch (Throwable t) {
            logger.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                logger.error("pulsar sink transaction rollback exception", e);

            }
        } finally {
            tx.close();
        }
        return status;
    }

    private void editStatistic(final Event event, String keyPostfix, boolean isOrder) {
        String topic = "";
        String streamId = "";
        String nodeIp;
        if (event != null) {
            if (event.getHeaders().containsKey(TOPIC)) {
                topic = event.getHeaders().get(TOPIC);
            }
            if (event.getHeaders().containsKey(AttributeConstants.STREAM_ID)) {
                streamId = event.getHeaders().get(AttributeConstants.STREAM_ID);
            } else if (event.getHeaders().containsKey(AttributeConstants.INAME)) {
                streamId = event.getHeaders().get(AttributeConstants.INAME);
            }

            // Compatible agent
            if (event.getHeaders().containsKey("ip")) {
                event.getHeaders().put(ConfigConstants.REMOTE_IP_KEY, event.getHeaders().get("ip"));
                event.getHeaders().remove("ip");
            }

            // Compatible agent
            if (event.getHeaders().containsKey("time")) {
                event.getHeaders().put(AttributeConstants.DATA_TIME, event.getHeaders().get("time"));
                event.getHeaders().remove("time");
            }

            if (event.getHeaders().containsKey(ConfigConstants.REMOTE_IP_KEY)) {
                nodeIp = event.getHeaders().get(ConfigConstants.REMOTE_IP_KEY);
                if (event.getHeaders().containsKey(ConfigConstants.REMOTE_IDC_KEY)) {
                    if (nodeIp != null) {
                        nodeIp = nodeIp.split(":")[0];
                    }

                    long msgCounterL = 1L;
                    // msg counter
                    if (event.getHeaders().containsKey(ConfigConstants.MSG_COUNTER_KEY)) {
                        msgCounterL = Integer.parseInt(event.getHeaders().get(ConfigConstants.MSG_COUNTER_KEY));
                    }

                    String orderType = "non-order";
                    if (isOrder) {
                        orderType = "order";
                    }
                    StringBuilder newBase = new StringBuilder();
                    newBase.append(this.getName()).append(SEPARATOR).append(topic).append(SEPARATOR)
                            .append(streamId).append(SEPARATOR).append(nodeIp)
                            .append(SEPARATOR).append(NetworkUtils.getLocalIp())
                            .append(SEPARATOR).append(orderType).append(SEPARATOR)
                            .append(event.getHeaders().get(ConfigConstants.PKG_TIME_KEY));

                    long messageSize = event.getBody().length;
                    if (event.getHeaders().get(ConfigConstants.TOTAL_LEN) != null) {
                        messageSize = Long.parseLong(event.getHeaders().get(ConfigConstants.TOTAL_LEN));
                    }

                    if (keyPostfix != null && !keyPostfix.equals("")) {
                        monitorIndex.addAndGet(new String(newBase), 0, 0, 0, (int) msgCounterL);
                        if (logPrinterB.shouldPrint()) {
                            logger.warn("error cannot send event, {} event size is {}", topic, messageSize);
                        }
                    } else {
                        monitorIndex.addAndGet(new String(newBase), (int) msgCounterL, 1, messageSize, 0);
                    }
                }
            }
        }
    }

    @Override
    public void handleCreateClientSuccess(String url) {
        logger.info("createConnection success for url = {}", url);
        sinkCounter.incrementConnectionCreatedCount();
    }

    @Override
    public void handleCreateClientException(String url) {
        logger.info("createConnection has exception for url = {}", url);
        sinkCounter.incrementConnectionFailedCount();
    }

    @Override
    public void handleMessageSendSuccess(String topic, Object result, EventStat eventStat) {
        /*
         * Statistics pulsar performance
         */
        TOTAL_PULSAR_SUCC_SEND_CNT.incrementAndGet();
        TOTAL_PULSAR_SUCC_SEND_SIZE.addAndGet(eventStat.getEvent().getBody().length);
        /*
         *add to sinkCounter
         */
        sinkCounter.incrementEventDrainSuccessCount();
        currentInFlightCount.decrementAndGet();
        currentSuccessSendCnt.incrementAndGet();
        long nowCnt = currentSuccessSendCnt.get();
        long oldCnt = lastSuccessSendCnt.get();
        long logEveryNEvents = pulsarConfig.getLogEveryNEvents();
        Preconditions.checkArgument(logEveryNEvents > 0, "logEveryNEvents must be > 0");

        if (nowCnt % logEveryNEvents == 0 && nowCnt != lastSuccessSendCnt.get()) {
            lastSuccessSendCnt.set(nowCnt);
            long t2 = System.currentTimeMillis();
            logger.info("Pulsar sink {}, succ put {} events to pulsar in the past {} millsec",
                    getName(), (nowCnt - oldCnt), (t2 - t1));
            t1 = t2;
        }
        Map<String, String> dimensions = getNewDimension(DataProxyMetricItem.KEY_SINK_DATA_ID, topic);
        DataProxyMetricItem metricItem = this.metricItemSet.findMetricItem(dimensions);
        metricItem.sendSuccessCount.incrementAndGet();
        metricItem.sendSuccessSize.addAndGet(eventStat.getEvent().getBody().length);
        metricItem.sendCount.incrementAndGet();
        metricItem.sendSize.addAndGet(eventStat.getEvent().getBody().length);
        monitorIndexExt.incrementAndGet("PULSAR_SINK_SUCCESS");
        editStatistic(eventStat.getEvent(), null, eventStat.isOrderMessage());

    }

    @Override
    public void handleMessageSendException(String topic, EventStat eventStat, Object e) {
        monitorIndexExt.incrementAndGet("PULSAR_SINK_EXP");
        boolean needRetry = true;
        if (e instanceof NotFoundException) {
            logger.error("NotFoundException for topic " + topic + ", message will be discard!", e);
            needRetry = false;
        } else if (e instanceof TooLongFrameException) {
            logger.error("TooLongFrameException, send failed for " + getName(), e);
        } else if (e instanceof ProducerQueueIsFullError) {
            logger.error("ProducerQueueIsFullError, send failed for " + getName(), e);
        } else if (!(e instanceof AlreadyClosedException
                || e instanceof PulsarClientException.NotConnectedException
                || e instanceof TopicTerminatedException)) {
            if (logPrinterB.shouldPrint()) {
                logger.error("send failed for " + getName(), e);
            }
            if (eventStat.getRetryCnt() == 0) {
                editStatistic(eventStat.getEvent(), "failure", eventStat.isOrderMessage());
            }
        }
        Map<String, String> dimensions = getNewDimension(DataProxyMetricItem.KEY_SINK_DATA_ID, topic);
        DataProxyMetricItem metricItem = this.metricItemSet.findMetricItem(dimensions);
        metricItem.sendFailCount.incrementAndGet();
        metricItem.sendFailSize.addAndGet(eventStat.getEvent().getBody().length);
        eventStat.incRetryCnt();
        if (!eventStat.isOrderMessage() && needRetry) {
            processResendEvent(eventStat);
        }
    }

    private Map<String, String> getNewDimension(String otherKey, String value) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, "DataProxy");
        dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getName());
        dimensions.put(otherKey, value);
        return dimensions;
    }

    private boolean processEvent(EventStat eventStat) {
        if (eventStat == null || eventStat.getEvent() == null) {
            return true;
        }

        boolean result = true;
        Event event = eventStat.getEvent();
        if (MessageUtils.isSyncSendForOrder(event) && (event instanceof OrderEvent)) {
            String partitionKey = event.getHeaders().get(AttributeConstants.MESSAGE_PARTITION_KEY);
            SinkTask sinkTask =
                    sinkThreadPool[Math.abs(partitionKey.hashCode()) % sinkThreadPoolSize];
            result = sinkTask.processEvent(eventStat);
        } else {
            int num = 0;
            do {
                int index = processIndex.getAndIncrement();
                SinkTask sinkTask = sinkThreadPool[index % sinkThreadPoolSize];
                if (sinkTask != null) {
                    result = sinkTask.processEvent(eventStat);
                    if (result) {
                        break;
                    }
                }
                num++;
            } while (num < sinkThreadPoolSize);
        }
        return result;
    }

    private void processResendEvent(EventStat eventStat) {
        try {
            if (eventStat == null || eventStat.getEvent() == null) {
                logger.warn("processResendEvent eventStat is null!");
                return;
            }
            if (eventStat.getRetryCnt() == 1) {
                currentInFlightCount.decrementAndGet();
            }
            /*
             * If the failure requires retransmission to pulsar,
             * the sid needs to be removed before retransmission.
             */
            if (pulsarConfig.getClientIdCache()) {
                String clientId = eventStat.getEvent().getHeaders().get(ConfigConstants.SEQUENCE_ID);
                if (clientId != null && AGENT_ID_CACHE.asMap().containsKey(clientId)) {
                    AGENT_ID_CACHE.invalidate(clientId);
                }
            }
            boolean result = false;
            int num = 0;
            do {
                int index = processIndex.getAndIncrement();
                SinkTask sinkTask = sinkThreadPool[index % sinkThreadPoolSize];
                if (sinkTask != null) {
                    result = sinkTask.processReSendEvent(eventStat);
                    if (result) {
                        break;
                    }
                }
                num++;
            } while (num < sinkThreadPoolSize);
            if (!result) {
                FailoverChannelProcessorHolder.getChannelProcessor()
                        .processEvent(eventStat.getEvent());
                if (logPrinterC.shouldPrint()) {
                    logger.error(getName() + " Channel --> pulsar --> ResendQueue(full) "
                            + "-->FailOverChannelProcessor(current code point), "
                            + "Resend queue is full,Check if pulsar server or network is ok.");
                }
            }
        } catch (Throwable throwable) {
            monitorIndexExt.incrementAndGet("PULSAR_SINK_DROPPED");
            if (logPrinterC.shouldPrint()) {
                logger.error(getName() + " Discard msg because put events to both of "
                        + "queue and fileChannel fail", throwable);
            }
        }
    }

    public LoadingCache<String, Long> getAgentIdCache() {
        return AGENT_ID_CACHE;
    }

    public Map<String, String> getTopicsProperties() {
        return topicProperties;
    }

    public SinkCounter getSinkCounter() {
        return sinkCounter;
    }

    public AtomicLong getCurrentInFlightCount() {
        return currentInFlightCount;
    }

    public MQClusterConfig getPulsarConfig() {
        return pulsarConfig;
    }

    public int getMaxRetrySendCnt() {
        return maxRetrySendCnt;
    }

    static class PulsarPerformanceTask implements Runnable {

        @Override
        public void run() {
            try {
                if (TOTAL_PULSAR_SUCC_SEND_SIZE.get() != 0) {
                    logger.info("Total pulsar performance tps :"
                            + TOTAL_PULSAR_SUCC_SEND_CNT.get() / PRINT_INTERVAL
                            + "/s, avg msg size:"
                            + TOTAL_PULSAR_SUCC_SEND_SIZE.get() / TOTAL_PULSAR_SUCC_SEND_CNT.get()
                            + ",print every " + PRINT_INTERVAL + " seconds");
                    // totalPulsarSuccSendCnt represents the number of packets
                    TOTAL_PULSAR_SUCC_SEND_CNT.set(0);
                    TOTAL_PULSAR_SUCC_SEND_SIZE.set(0);
                }

            } catch (Exception e) {
                logger.info("pulsarPerformanceTask error", e);
            }
        }
    }
}
