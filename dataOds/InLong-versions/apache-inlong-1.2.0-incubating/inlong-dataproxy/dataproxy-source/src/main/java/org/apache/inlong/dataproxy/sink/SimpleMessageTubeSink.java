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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.FlumeException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.flume.source.shaded.guava.RateLimiter;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.utils.Constants;
import org.apache.inlong.dataproxy.utils.NetworkUtils;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentCallback;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corerpc.exception.OverflowException;
import org.apache.pulsar.shade.org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class SimpleMessageTubeSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMessageTubeSink.class);
    private static int MAX_TOPICS_EACH_PRODUCER_HOLD = 200;
    private static final String TUBE_REQUEST_TIMEOUT = "tube-request-timeout";
    private static final String KEY_DISK_IO_RATE_PER_SEC = "disk-io-rate-per-sec";

    private static int BAD_EVENT_QUEUE_SIZE = 10000;

    private static final String SINK_THREAD_NUM = "thread-num";
    private static int EVENT_QUEUE_SIZE = 1000;
    private volatile boolean canTake = false;
    private volatile boolean canSend = false;
    private static int BATCH_SIZE = 10000;
    private static final int defaultRetryCnt = -1;
    private static final int defaultLogEveryNEvents = 100000;
    private static final int defaultSendTimeout = 20000; // in millsec
    private static final int defaultStatIntervalSec = 60;
    private static final int sendNewMetricRetryCount = 3;

    private static String MASTER_HOST_PORT_LIST = "master-host-port-list";
    private static String TOPIC = "topic";
    private static String SEND_TIMEOUT = "send_timeout"; // in millsec
    private static String LOG_EVERY_N_EVENTS = "log-every-n-events";
    private static String RETRY_CNT = "retry-currentSuccSendedCnt";
    private static String STAT_INTERVAL_SEC = "stat-interval-sec"; // in sec
    private static String MAX_TOPICS_EACH_PRODUCER_HOLD_NAME = "max-topic-each-producer-hold";

    private static final String LOG_TOPIC = "proxy-log-topic";
    private static final String STREAMID = "proxy-log-streamid";
    private static final String GROUPID = "proxy-log-groupid";
    private static final String SEND_REMOTE = "send-remote";
    private static final String topicsFilePath = "topics.properties";
    private static final String slaTopicFilePath = "slaTopics.properties";
    private static final String SLA_METRIC_SINK = "sla-metric-sink";

    private static String MAX_SURVIVED_TIME = "max-survived-time";
    private static String MAX_SURVIVED_SIZE = "max-survived-size";
    private static String CLIENT_ID_CACHE = "client-id-cache";

    private int maxSurvivedTime = 3 * 1000 * 30;
    private int maxSurvivedSize = 100000;

    private String proxyLogTopic = "teg_manager";
    private String proxyLogGroupId = "b_teg_manager";
    private String proxyLogStreamId = "proxy_measure_log";
    private boolean sendRemote = false;
    private ConfigManager configManager;
    private Map<String, String> topicProperties;

    public MessageProducer producer;
    public Map<String, MessageProducer> producerMap;

    private LinkedBlockingQueue<EventStat> resendQueue;
    private LinkedBlockingQueue<Event> eventQueue;

    private long diskIORatePerSec;
    private RateLimiter diskRateLimiter;

    public AtomicInteger currentPublishTopicNum = new AtomicInteger(0);
    public TubeMultiSessionFactory sessionFactory;
    private String masterHostAndPortList;
    private Integer logEveryNEvents;
    private Integer sendTimeout;
    private static int retryCnt = defaultRetryCnt;
    private int requestTimeout = 60;
    private int threadNum;
    private Thread[] sinkThreadPool;

    private String metaTopicFilePath = topicsFilePath;
    private long linkMaxAllowedDelayedMsgCount;
    private long sessionWarnDelayedMsgCount;
    private long sessionMaxAllowedDelayedMsgCount;
    private long nettyWriteBufferHighWaterMark;
    private int recoverthreadcount;
    //
    private Map<String, String> dimensions;
    private DataProxyMetricItemSet metricItemSet;

    private static final LoadingCache<String, Long> agentIdCache = CacheBuilder
            .newBuilder().concurrencyLevel(4 * 8).initialCapacity(5000000).expireAfterAccess(30, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Long>() {

                @Override
                public Long load(String key) {
                    return System.currentTimeMillis();
                }
            });

    private IdCacheCleaner idCacheCleaner;
    protected static boolean idCleanerStarted = false;
    protected static final ConcurrentHashMap<String, Long> agentIdMap =
            new ConcurrentHashMap<String, Long>();
    private static ConcurrentHashMap<String, Long> illegalTopicMap =
            new ConcurrentHashMap<String, Long>();

    private boolean clientIdCache = false;
    private boolean isNewCache = true;

    private boolean overflow = false;

    /**
     * diff publish
     *
     * @param originalSet
     * @param endSet
     */
    public void diffSetPublish(Set<String> originalSet, Set<String> endSet) {

        boolean changed = false;
        for (String s : endSet) {
            if (!originalSet.contains(s)) {
                changed = true;
                try {
                    producer = getProducer(s);
                } catch (Exception e) {
                    logger.error("Get producer failed!", e);
                }
            }
        }

        if (changed) {
            logger.info("topics.properties has changed, trigger diff publish for {}", getName());
            topicProperties = configManager.getTopicProperties();
        }
    }

    private MessageProducer getProducer(String topic) throws TubeClientException {
        if (producerMap.containsKey(topic)) {
            return producerMap.get(topic);
        } else {
            synchronized (this) {
                if (!producerMap.containsKey(topic)) {
                    if (producer == null || currentPublishTopicNum.get() >= MAX_TOPICS_EACH_PRODUCER_HOLD) {
                        producer = sessionFactory.createProducer();
                        currentPublishTopicNum.set(0);
                    }
                    // publish topic
                    producer.publish(topic);
                    producerMap.put(topic, producer);
                    currentPublishTopicNum.incrementAndGet();
                }
            }
            return producerMap.get(topic);
        }
    }

    private TubeClientConfig initTubeConfig() throws Exception {
        final TubeClientConfig tubeClientConfig = new TubeClientConfig(NetworkUtils.getLocalIp(),
                this.masterHostAndPortList);
        tubeClientConfig.setLinkMaxAllowedDelayedMsgCount(linkMaxAllowedDelayedMsgCount);
        tubeClientConfig.setSessionWarnDelayedMsgCount(sessionWarnDelayedMsgCount);
        tubeClientConfig.setSessionMaxAllowedDelayedMsgCount(sessionMaxAllowedDelayedMsgCount);
        tubeClientConfig.setNettyWriteBufferHighWaterMark(nettyWriteBufferHighWaterMark);
        tubeClientConfig.setHeartbeatPeriodMs(15000L);
        tubeClientConfig.setRpcTimeoutMs(20000L);

        return tubeClientConfig;
    }

    /**
     * If this function is called successively without calling {@see #destroyConnection()}, only the
     * first call has any effect.
     *
     * @throws FlumeException if an RPC client connection could not be opened
     */
    private void createConnection() throws FlumeException {
//        synchronized (tubeSessionLock) {
        // if already connected, just skip
        if (sessionFactory != null) {
            return;
        }

        try {
            TubeClientConfig conf = initTubeConfig();
            //sessionFactory = new TubeMutilMessageSessionFactory(conf);
            sessionFactory = new TubeMultiSessionFactory(conf);
        } catch (TubeClientException e) {
            logger.error("create connnection error in metasink, "
                    + "maybe tube master set error, please re-check. ex1 {}", e.getMessage());
            throw new FlumeException("connect to Tube error1, "
                    + "maybe zkstr/zkroot set error, please re-check");
        } catch (Throwable e) {
            logger.error("create connnection error in metasink, "
                            + "maybe tube master set error/shutdown in progress, please re-check. ex2 {}",
                    e.getMessage());
            throw new FlumeException("connect to meta error2, "
                    + "maybe tube master set error/shutdown in progress, please re-check");
        }

        if (producerMap == null) {
            producerMap = new HashMap<String, MessageProducer>();
        }
        logger.debug("building tube producer");
//        }
    }

    private void destroyConnection() {
        for (Map.Entry<String, MessageProducer> entry : producerMap.entrySet()) {
            MessageProducer producer = entry.getValue();
            try {
                producer.shutdown();
            } catch (TubeClientException e) {
                logger.error("destroy producer error in metasink, MetaClientException {}", e.getMessage());
            } catch (Throwable e) {
                logger.error("destroy producer error in metasink, ex {}", e.getMessage());
            }
        }
        producerMap.clear();

        if (sessionFactory != null) {
            try {
                sessionFactory.shutdown();
            } catch (TubeClientException e) {
                logger.error("destroy sessionFactory error in metasink, MetaClientException {}",
                        e.getMessage());
            } catch (Exception e) {
                logger.error("destroy sessionFactory error in metasink, ex {}", e.getMessage());
            }
        }
        sessionFactory = null;
        logger.debug("closed meta producer");
    }

    private void initTopicSet(Set<String> topicSet) throws Exception {
        List<String> sortedList = new ArrayList(topicSet);
        Collections.sort(sortedList);
        int cycle = sortedList.size() / MAX_TOPICS_EACH_PRODUCER_HOLD;
        int remainder = sortedList.size() % MAX_TOPICS_EACH_PRODUCER_HOLD;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i <= cycle; i++) {
            Set<String> subset = new HashSet<String>();
            int startIndex = i * MAX_TOPICS_EACH_PRODUCER_HOLD;
            int endIndex = startIndex + MAX_TOPICS_EACH_PRODUCER_HOLD - 1;
            if (i == cycle) {
                if (remainder == 0) {
                    continue;
                } else {
                    endIndex = startIndex + remainder - 1;
                }
            }
            for (int index = startIndex; index <= endIndex; index++) {
                subset.add(sortedList.get(index));
            }
            producer = sessionFactory.createProducer();
            try {
                Set<String> succTopicSet = producer.publish(subset);
                if (succTopicSet != null) {
                    for (String succTopic : succTopicSet) {
                        producerMap.put(succTopic, producer);
                    }
                    currentPublishTopicNum.set(succTopicSet.size());
                    logger.info(getName() + " success Subset  : " + succTopicSet);
                }
            } catch (Exception e) {
                logger.info(getName() + " meta sink initTopicSet fail.", e);
            }
        }
        logger.info(getName() + " initTopicSet cost: " + (System.currentTimeMillis() - startTime) + "ms");
        logger.info(getName() + " producer is ready for topics : " + producerMap.keySet());
    }

    @Override
    public void start() {
        this.dimensions = new HashMap<>();
        this.dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, "DataProxy");
        this.dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getName());
        //register metrics
        this.metricItemSet = new DataProxyMetricItemSet(this.getName());
        MetricRegister.register(metricItemSet);
        
        //create tube connection
        try {
            createConnection();
        } catch (FlumeException e) {
            logger.error("Unable to create tube client" + ". Exception follows.", e);

            /* Try to prevent leaking resources. */
            destroyConnection();

            /* FIXME: Mark ourselves as failed. */
            stop();
            return;
        }

        // start the cleaner thread
        if (clientIdCache && !isNewCache) {
            idCacheCleaner = new IdCacheCleaner(this, maxSurvivedTime, maxSurvivedSize);
            idCacheCleaner.start();
        }

        super.start();
        this.canSend = true;
        this.canTake = true;

        try {
            initTopicSet(new HashSet<String>(topicProperties.values()));
        } catch (Exception e) {
            logger.info("meta sink start publish topic fail.", e);
        }

        for (int i = 0; i < sinkThreadPool.length; i++) {
            sinkThreadPool[i] = new Thread(new SinkTask(), getName() + "_tube_sink_sender-" + i);
            sinkThreadPool[i].start();
        }

    }

    class SinkTask implements Runnable {
        private void sendMessage(Event event, String topic, AtomicBoolean flag, EventStat es)
            throws TubeClientException, InterruptedException {
            String clientId = event.getHeaders().get(ConfigConstants.SEQUENCE_ID);
            if (!isNewCache) {
                Long lastTime = 0L;
                if (clientIdCache && clientId != null) {
                    lastTime = agentIdMap.put(clientId, System.currentTimeMillis());
                }
                if (clientIdCache && clientId != null && lastTime != null && lastTime > 0) {
                    logger.info("{} agent package {} existed,just discard.", getName(), clientId);
                } else {
                    Message message = this.parseEvent2Message(topic, event);
                    producer.sendMessage(message, new MyCallback(es));
                    flag.set(true);

                }
            } else {
                boolean hasKey = false;
                if (clientIdCache && clientId != null) {
                    hasKey = agentIdCache.asMap().containsKey(clientId);
                }

                if (clientIdCache && clientId != null && hasKey) {
                    agentIdCache.put(clientId, System.currentTimeMillis());
                    logger.info("{} agent package {} existed,just discard.", getName(), clientId);
                } else {
                    if (clientId != null) {
                        agentIdCache.put(clientId, System.currentTimeMillis());
                    }

                    Message message = this.parseEvent2Message(topic, event);
                    producer.sendMessage(message, new MyCallback(es));
                    flag.set(true);
                }
            }
            illegalTopicMap.remove(topic);
        }
        
        /**
         * parseEvent2Message
         * @param topic
         * @param event
         * @return
         */
        private Message parseEvent2Message(String topic, Event event) {
            Message message = new Message(topic, event.getBody());
            message.setAttrKeyVal("dataproxyip", NetworkUtils.getLocalIp());
            String streamId = "";
            if (event.getHeaders().containsKey(AttributeConstants.STREAM_ID)) {
                streamId = event.getHeaders().get(AttributeConstants.STREAM_ID);
            } else if (event.getHeaders().containsKey(AttributeConstants.INAME)) {
                streamId = event.getHeaders().get(AttributeConstants.INAME);
            }
            message.putSystemHeader(streamId, event.getHeaders().get(ConfigConstants.PKG_TIME_KEY));
            // common attributes
            Map<String, String> headers = event.getHeaders();
            message.setAttrKeyVal(Constants.INLONG_GROUP_ID, headers.get(Constants.INLONG_GROUP_ID));
            message.setAttrKeyVal(Constants.INLONG_STREAM_ID, headers.get(Constants.INLONG_STREAM_ID));
            message.setAttrKeyVal(Constants.TOPIC, headers.get(Constants.TOPIC));
            message.setAttrKeyVal(Constants.HEADER_KEY_MSG_TIME, headers.get(Constants.HEADER_KEY_MSG_TIME));
            message.setAttrKeyVal(Constants.HEADER_KEY_SOURCE_IP, headers.get(Constants.HEADER_KEY_SOURCE_IP));
            return message;
        }

        private void handleException(Throwable t, String topic, boolean decrementFlag, EventStat es) {
            if (t instanceof TubeClientException) {
                String message = t.getMessage();
                if (message != null && (message.contains("No available queue for topic")
                    || message.contains("The brokers of topic are all forbidden"))) {
                    illegalTopicMap.put(topic, System.currentTimeMillis() + 60 * 1000);
                    logger.info("IllegalTopicMap.put " + topic);
                    return;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //ignore..
                    }
                }
            }
            logger.error("Sink task fail to send the message, decrementFlag=" + decrementFlag + ",sink.name="
                + Thread.currentThread().getName()
                + ",event.headers=" + es.getEvent().getHeaders(), t);
        }

        @Override
        public void run() {
            logger.info("Sink task {} started.", Thread.currentThread().getName());
            while (canSend) {
                boolean decrementFlag = false;
                boolean resendBadEvent = false;
                Event event = null;
                EventStat es = null;
                String topic = null;
                try {
                    if (SimpleMessageTubeSink.this.overflow) {
                        SimpleMessageTubeSink.this.overflow = false;
                        Thread.sleep(10);
                    }
                    if (!resendQueue.isEmpty()) {
                        es = resendQueue.poll();
                        if (es != null) {
                            event = es.getEvent();
                            // logger.warn("Resend event: {}", event.toString());
                            if (event.getHeaders().containsKey(TOPIC)) {
                                topic = event.getHeaders().get(TOPIC);
                            }
                            resendBadEvent = true;
                        }
                    } else {
                        event = eventQueue.take();
                        es = new EventStat(event);
//                            sendCnt.incrementAndGet();
                        if (event.getHeaders().containsKey(TOPIC)) {
                            topic = event.getHeaders().get(TOPIC);
                        }
                    }

                    if (event == null) {
                        // ignore event is null, when multiple-thread SinkTask running
                        // this null value comes from resendQueue
                        continue;
                    }

                    if (topic == null || topic.equals("")) {
                        logger.warn("no topic specified in event header, just skip this event");
                        continue;
                    }

                    Long expireTime = illegalTopicMap.get(topic);
                    if (expireTime != null) {
                        long currentTime = System.currentTimeMillis();
                        if (expireTime > currentTime) {

                            // TODO: need to be improved.
//                            reChannelEvent(es, topic);
                            continue;
                        } else {

                            illegalTopicMap.remove(topic);
                        }
                    }
                    MessageProducer producer = null;
                    try {
                        producer = getProducer(topic);
                    } catch (Exception e) {
                        logger.error("Get producer failed!", e);
                    }

                    if (producer == null) {
                        illegalTopicMap.put(topic, System.currentTimeMillis() + 30 * 1000);
                        continue;
                    }

                    AtomicBoolean flagAtomic = new AtomicBoolean(decrementFlag);
                    sendMessage(event, topic, flagAtomic, es);
                    decrementFlag = flagAtomic.get();

                } catch (InterruptedException e) {
                    logger.info("Thread {} has been interrupted!", Thread.currentThread().getName());
                    return;
                } catch (Throwable t) {
                    handleException(t, topic, decrementFlag, es);
                    resendEvent(es, decrementFlag);
                }
            }
        }
    }

    public class MyCallback implements MessageSentCallback {
        private EventStat myEventStat;
        private long sendTime;

        public MyCallback(EventStat eventStat) {
            this.myEventStat = eventStat;
            this.sendTime = System.currentTimeMillis();
        }

        @Override
        public void onMessageSent(final MessageSentResult result) {
            if (result.isSuccess()) {
                // TODO: add stats
                this.addMetric(myEventStat.getEvent(), true, sendTime);
            } else {
                this.addMetric(myEventStat.getEvent(), false, 0);
                if (result.getErrCode() == TErrCodeConstants.FORBIDDEN) {
                    logger.warn("Send message failed, error message: {}, resendQueue size: {}, event:{}",
                            result.getErrMsg(), resendQueue.size(),
                            myEventStat.getEvent().hashCode());

                    return;
                }
                if (result.getErrCode() != TErrCodeConstants.SERVER_RECEIVE_OVERFLOW) {
                    logger.warn("Send message failed, error message: {}, resendQueue size: {}, event:{}",
                            result.getErrMsg(), resendQueue.size(),
                            myEventStat.getEvent().hashCode());
                }
                resendEvent(myEventStat, true);
            }
        }

        /**
         * addMetric
         * 
         * @param event
         * @param result
         * @param sendTime
         */
        private void addMetric(Event event, boolean result, long sendTime) {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, SimpleMessageTubeSink.this.getName());
            dimensions.put(DataProxyMetricItem.KEY_SINK_ID, SimpleMessageTubeSink.this.getName());
            dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, event.getHeaders().getOrDefault(TOPIC, ""));
            DataProxyMetricItem.fillInlongId(event, dimensions);
            DataProxyMetricItem.fillAuditFormatTime(event, dimensions);
            
            DataProxyMetricItem metricItem = SimpleMessageTubeSink.this.metricItemSet.findMetricItem(dimensions);
            if (result) {
                metricItem.sendSuccessCount.incrementAndGet();
                metricItem.sendSuccessSize.addAndGet(event.getBody().length);
                AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_SEND_SUCCESS, event);
                if (sendTime > 0) {
                    long currentTime = System.currentTimeMillis();
                    long msgTime = NumberUtils.toLong(event.getHeaders().get(Constants.HEADER_KEY_MSG_TIME),
                            sendTime);
                    long sinkDuration = currentTime - sendTime;
                    long nodeDuration = currentTime - NumberUtils.toLong(Constants.HEADER_KEY_SOURCE_TIME, msgTime);
                    long wholeDuration = currentTime - msgTime;
                    metricItem.sinkDuration.addAndGet(sinkDuration);
                    metricItem.nodeDuration.addAndGet(nodeDuration);
                    metricItem.wholeDuration.addAndGet(wholeDuration);
                }
            } else {
                metricItem.sendFailCount.incrementAndGet();
                metricItem.sendFailSize.addAndGet(event.getBody().length);
            }
        }

        @Override
        public void onException(final Throwable e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof OverflowException) {
                SimpleMessageTubeSink.this.overflow = true;
            }
            resendEvent(myEventStat, true);
        }
    }

    /**
     * resend event
     *
     * @param es
     * @param isDecrement
     */
    private void resendEvent(EventStat es, boolean isDecrement) {
        try {
            if (es == null || es.getEvent() == null) {
                return;
            }

            if (clientIdCache) {
                String clientId = es.getEvent().getHeaders().get(ConfigConstants.SEQUENCE_ID);
                if (!isNewCache) {
                    if (clientId != null && agentIdMap.containsKey(clientId)) {
                        agentIdMap.remove(clientId);
                    }
                } else {
                    if (clientId != null && agentIdCache.asMap().containsKey(clientId)) {
                        agentIdCache.invalidate(clientId);
                    }
                }
            }
        } catch (Throwable throwable) {
            logger.error(getName() + " Discard msg because put events to both of queue and "
                    + "fileChannel fail,current resendQueue.size = "
                    + resendQueue.size(), throwable);
        }
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
                if (!eventQueue.offer(event, 3 * 1000, TimeUnit.MILLISECONDS)) {
                    logger.info("[{}] Channel --> Queue(has no enough space,current code point) "
                            + "--> Tube,Check if Tube server or network is ok.(if this situation last long time "
                            + "it will cause memoryChannel full and fileChannel write.)", getName());
                    tx.rollback();
                } else {
                    tx.commit();
                    // metric
                    if (event.getHeaders().containsKey(TOPIC)) {
                        dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, event.getHeaders().get(TOPIC));
                    } else {
                        dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID, "");
                    }
                    DataProxyMetricItem metricItem = this.metricItemSet.findMetricItem(dimensions);
                    metricItem.readFailCount.incrementAndGet();
                    metricItem.readFailSize.addAndGet(event.getBody().length);
                }
            } else {

                // logger.info("[{}]No data to process in the channel.",getName());
                status = Status.BACKOFF;
                tx.commit();
            }
        } catch (Throwable t) {
            logger.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                logger.error("metasink transaction rollback exception", e);

            }
        } finally {
            tx.close();
        }
        return status;
    }

    @Override
    public void configure(Context context) {
        logger.info(context.toString());
//        logger.info("sinktest:"+getName()+getChannel());//sinktest:meta-sink-msg2null

        configManager = ConfigManager.getInstance();
        topicProperties = configManager.getTopicProperties();
        configManager.getTopicConfig().addUpdateCallback(new ConfigUpdateCallback() {
            @Override
            public void update() {

                diffSetPublish(new HashSet<String>(topicProperties.values()),
                        new HashSet<String>(configManager.getTopicProperties().values()));
            }
        });

        masterHostAndPortList = context.getString(MASTER_HOST_PORT_LIST);
        Preconditions.checkState(masterHostAndPortList != null, "No master and port list specified");

        producerMap = new HashMap<String, MessageProducer>();

        logEveryNEvents = context.getInteger(LOG_EVERY_N_EVENTS, defaultLogEveryNEvents);
        logger.debug(this.getName() + " " + LOG_EVERY_N_EVENTS + " " + logEveryNEvents);
        Preconditions.checkArgument(logEveryNEvents > 0, "logEveryNEvents must be > 0");

        sendTimeout = context.getInteger(SEND_TIMEOUT, defaultSendTimeout);
        logger.debug(this.getName() + " " + SEND_TIMEOUT + " " + sendTimeout);
        Preconditions.checkArgument(sendTimeout > 0, "sendTimeout must be > 0");

        MAX_TOPICS_EACH_PRODUCER_HOLD = context.getInteger(MAX_TOPICS_EACH_PRODUCER_HOLD_NAME, 200);
        retryCnt = context.getInteger(RETRY_CNT, defaultRetryCnt);
        logger.debug(this.getName() + " " + RETRY_CNT + " " + retryCnt);

        boolean isSlaMetricSink = context.getBoolean(SLA_METRIC_SINK, false);
        if (isSlaMetricSink) {
            this.metaTopicFilePath = slaTopicFilePath;
        }

        clientIdCache = context.getBoolean(CLIENT_ID_CACHE, clientIdCache);
        if (clientIdCache) {
            int survivedTime = context.getInteger(MAX_SURVIVED_TIME, maxSurvivedTime);
            if (survivedTime > 0) {
                maxSurvivedTime = survivedTime;
            } else {
                logger.warn("invalid {}:{}", MAX_SURVIVED_TIME, survivedTime);
            }

            int survivedSize = context.getInteger(MAX_SURVIVED_SIZE, maxSurvivedSize);
            if (survivedSize > 0) {
                maxSurvivedSize = survivedSize;
            } else {
                logger.warn("invalid {}:{}", MAX_SURVIVED_SIZE, survivedSize);
            }
        }

        String requestTimeout = context.getString(TUBE_REQUEST_TIMEOUT);
        if (requestTimeout != null) {
            this.requestTimeout = Integer.parseInt(requestTimeout);
        }

        String sendRemoteStr = context.getString(SEND_REMOTE);
        if (sendRemoteStr != null) {
            sendRemote = Boolean.parseBoolean(sendRemoteStr);
        }
        if (sendRemote) {
            proxyLogTopic = context.getString(LOG_TOPIC, proxyLogTopic);
            proxyLogGroupId = context.getString(GROUPID, proxyLogStreamId);
            proxyLogStreamId = context.getString(STREAMID, proxyLogStreamId);
        }

        resendQueue = new LinkedBlockingQueue<>(BAD_EVENT_QUEUE_SIZE);

        String sinkThreadNum = context.getString(SINK_THREAD_NUM, "4");
        threadNum = Integer.parseInt(sinkThreadNum);
        Preconditions.checkArgument(threadNum > 0, "threadNum must be > 0");
        sinkThreadPool = new Thread[threadNum];
        eventQueue = new LinkedBlockingQueue<Event>(EVENT_QUEUE_SIZE);

        diskIORatePerSec = context.getLong(KEY_DISK_IO_RATE_PER_SEC, 0L);
        if (diskIORatePerSec != 0) {
            diskRateLimiter = RateLimiter.create(diskIORatePerSec);
        }

        linkMaxAllowedDelayedMsgCount = context.getLong(ConfigConstants.LINK_MAX_ALLOWED_DELAYED_MSG_COUNT,
                80000L);
        sessionWarnDelayedMsgCount = context.getLong(ConfigConstants.SESSION_WARN_DELAYED_MSG_COUNT,
                2000000L);
        sessionMaxAllowedDelayedMsgCount = context.getLong(ConfigConstants.SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT,
                4000000L);
        nettyWriteBufferHighWaterMark = context.getLong(ConfigConstants.NETTY_WRITE_BUFFER_HIGH_WATER_MARK,
                15 * 1024 * 1024L);
        recoverthreadcount = context.getInteger(ConfigConstants.RECOVER_THREAD_COUNT,
                Runtime.getRuntime().availableProcessors() + 1);
    }

    /**
     * get metricItemSet
     * @return the metricItemSet
     */
    public DataProxyMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }
    
}
