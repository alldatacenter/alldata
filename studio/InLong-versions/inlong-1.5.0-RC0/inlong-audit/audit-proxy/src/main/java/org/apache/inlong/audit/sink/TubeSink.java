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

package org.apache.inlong.audit.sink;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.FlumeException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.audit.base.HighPriorityThreadFactory;
import org.apache.inlong.audit.consts.ConfigConstants;
import org.apache.inlong.audit.utils.FailoverChannelProcessorHolder;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentCallback;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corerpc.exception.OverflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TubeSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(TubeSink.class);

    private static final String SINK_THREAD_NUM = "thread-num";

    private static final int defaultRetryCnt = -1;

    private static final int defaultLogEveryNEvents = 100000;

    private static final int defaultSendTimeout = 20000; // in millsec

    private static final Long PRINT_INTERVAL = 30L;

    private static final TubePerformanceTask tubePerformanceTask = new TubePerformanceTask();

    private static final int BAD_EVENT_QUEUE_SIZE = 10000;

    private static final int EVENT_QUEUE_SIZE = 1000;

    private static final String MASTER_HOST_PORT_LIST = "master-host-port-list";

    private static final String TOPIC = "topic";

    private static final String SEND_TIMEOUT = "send_timeout"; // in millsec

    private static final String LOG_EVERY_N_EVENTS = "log-every-n-events";

    private static final String RETRY_CNT = "retry-currentSuccSendedCnt";

    private static int retryCnt = defaultRetryCnt;

    private static AtomicLong totalTubeSuccSendCnt = new AtomicLong(0);

    private static AtomicLong totalTubeSuccSendSize = new AtomicLong(0);

    private static ConcurrentHashMap<String, Long> illegalTopicMap =
            new ConcurrentHashMap<String, Long>();

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
            new HighPriorityThreadFactory("tubePerformance-Printer-thread"));

    static {
        /*
         * stat tube performance
         */
        logger.info("tubePerformanceTask!!!!!!");
        scheduledExecutorService.scheduleWithFixedDelay(tubePerformanceTask, 0L,
                PRINT_INTERVAL, TimeUnit.SECONDS);
    }

    public MessageProducer producer;
    public Map<String, MessageProducer> producerMap;
    public TubeMultiSessionFactory sessionFactory;
    private SinkCounter sinkCounter;
    private String topic;
    private volatile boolean canTake = false;
    private volatile boolean canSend = false;
    private LinkedBlockingQueue<EventStat> resendQueue;
    private LinkedBlockingQueue<Event> eventQueue;
    private long diskIORatePerSec;
    private RateLimiter diskRateLimiter;
    private String masterHostAndPortList;
    private Integer logEveryNEvents;
    private Integer sendTimeout;
    private int threadNum;
    private Thread[] sinkThreadPool;
    private long linkMaxAllowedDelayedMsgCount;
    private long sessionWarnDelayedMsgCount;
    private long sessionMaxAllowedDelayedMsgCount;
    private long nettyWriteBufferHighWaterMark;
    private int recoverthreadcount;
    private boolean overflow = false;
    /*
     * for stat
     */
    private AtomicLong currentSuccessSendCnt = new AtomicLong(0);
    private AtomicLong lastSuccessSendCnt = new AtomicLong(0);
    private long t1 = System.currentTimeMillis();
    private long t2 = 0L;

    private String localIp = "127.0.0.1";

    public TubeSink() {
        super();
        logger.debug("new instance of TubeSink!");
    }

    @Override
    public synchronized void start() {
        logger.info("tube sink starting...");
        try {
            createConnection();
        } catch (FlumeException e) {
            logger.error("Unable to create tube client" + ". Exception follows.", e);

            // prevent leaking resources
            stop();
            return;
        }

        sinkCounter.start();
        super.start();
        this.canSend = true;
        this.canTake = true;

        try {
            initTopicProducer(topic);
        } catch (Exception e) {
            logger.error("tubesink start publish topic fail.", e);
        }

        for (int i = 0; i < sinkThreadPool.length; i++) {
            sinkThreadPool[i] = new Thread(new SinkTask(), getName() + "_tube_sink_sender-" + i);
            sinkThreadPool[i].start();
        }
        logger.debug("tubesink started");

    }

    @Override
    public synchronized void stop() {
        logger.info("tubesink stopping");
        destroyConnection();
        this.canTake = false;
        int waitCount = 0;
        while (eventQueue.size() != 0 && waitCount++ < 10) {
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                logger.info("Stop thread has been interrupt!");
                break;
            }
        }
        this.canSend = false;

        if (sinkThreadPool != null) {
            for (Thread thread : sinkThreadPool) {
                if (thread != null) {
                    thread.interrupt();
                }
            }
            sinkThreadPool = null;
        }

        super.stop();
        if (!scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        sinkCounter.stop();
        logger.debug("tubesink stopped. Metrics:{}", sinkCounter);
    }

    @Override
    public Status process() throws EventDeliveryException {
        logger.debug("process......");
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
                logger.error("tubesink transaction rollback exception", e);

            }
        } finally {
            tx.close();
        }
        return status;
    }

    @Override
    public void configure(Context context) {
        logger.info("Tubesink started and context = {}", context.toString());

        topic = context.getString(TOPIC);
        Preconditions.checkState(StringUtils.isNotEmpty(topic), "No topic specified");

        masterHostAndPortList = context.getString(MASTER_HOST_PORT_LIST);
        Preconditions.checkState(masterHostAndPortList != null,
                "No master and port list specified");

        producerMap = new HashMap<String, MessageProducer>();

        logEveryNEvents = context.getInteger(LOG_EVERY_N_EVENTS, defaultLogEveryNEvents);
        logger.debug(this.getName() + " " + LOG_EVERY_N_EVENTS + " " + logEveryNEvents);
        Preconditions.checkArgument(logEveryNEvents > 0, "logEveryNEvents must be > 0");

        sendTimeout = context.getInteger(SEND_TIMEOUT, defaultSendTimeout);
        logger.debug(this.getName() + " " + SEND_TIMEOUT + " " + sendTimeout);
        Preconditions.checkArgument(sendTimeout > 0, "sendTimeout must be > 0");

        retryCnt = context.getInteger(RETRY_CNT, defaultRetryCnt);
        logger.debug(this.getName() + " " + RETRY_CNT + " " + retryCnt);

        localIp = NetworkUtils.getLocalIp();

        if (sinkCounter == null) {
            sinkCounter = new SinkCounter(getName());
        }

        resendQueue = new LinkedBlockingQueue<>(BAD_EVENT_QUEUE_SIZE);

        String sinkThreadNum = context.getString(SINK_THREAD_NUM, "4");
        threadNum = Integer.parseInt(sinkThreadNum);
        Preconditions.checkArgument(threadNum > 0, "threadNum must be > 0");
        sinkThreadPool = new Thread[threadNum];
        eventQueue = new LinkedBlockingQueue<Event>(EVENT_QUEUE_SIZE);

        diskIORatePerSec = context.getLong("disk-io-rate-per-sec", 0L);
        if (diskIORatePerSec != 0) {
            diskRateLimiter = RateLimiter.create(diskIORatePerSec);
        }

        linkMaxAllowedDelayedMsgCount = context.getLong(ConfigConstants.LINK_MAX_ALLOWED_DELAYED_MSG_COUNT,
                ConfigConstants.DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT);
        sessionWarnDelayedMsgCount = context.getLong(ConfigConstants.SESSION_WARN_DELAYED_MSG_COUNT,
                ConfigConstants.DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT);
        sessionMaxAllowedDelayedMsgCount = context.getLong(ConfigConstants.SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT,
                ConfigConstants.DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT);
        nettyWriteBufferHighWaterMark = context.getLong(ConfigConstants.NETTY_WRITE_BUFFER_HIGH_WATER_MARK,
                ConfigConstants.DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK);
        recoverthreadcount = context.getInteger(ConfigConstants.RECOVER_THREAD_COUNT,
                Runtime.getRuntime().availableProcessors() + 1);

    }

    /**
     * Send message of success.
     */
    public void handleMessageSendSuccess(EventStat es) {
        // Statistics tube performance
        totalTubeSuccSendCnt.incrementAndGet();
        totalTubeSuccSendSize.addAndGet(es.getEvent().getBody().length);

        // add to sinkCounter
        sinkCounter.incrementEventDrainSuccessCount();
        currentSuccessSendCnt.incrementAndGet();
        long nowCnt = currentSuccessSendCnt.get();
        long oldCnt = lastSuccessSendCnt.get();
        if (nowCnt % logEveryNEvents == 0 && nowCnt != lastSuccessSendCnt.get()) {
            lastSuccessSendCnt.set(nowCnt);
            t2 = System.currentTimeMillis();
            logger.info("tubesink {}, succ put {} events to tube,"
                    + " in the past {} millsec",
                    new Object[]{
                            getName(), (nowCnt - oldCnt), (t2 - t1)
                    });
            t1 = t2;
        }

    }

    /**
     * Resend the data and store the data in the memory cache.
     *
     * @param es
     * @param isDecrement
     */
    private void resendEvent(EventStat es, boolean isDecrement) {
        try {
            if (es == null || es.getEvent() == null) {
                return;
            }
            if (!resendQueue.offer(es)) {
                FailoverChannelProcessorHolder.getChannelProcessor().processEvent(es.getEvent());
            }
        } catch (Throwable throwable) {
            logger.error("resendEvent e = {}", throwable);
        }
    }

    /**
     * If this function is called successively without calling {@see #destroyConnection()}, only the
     * first call has any effect.
     *
     * @throws FlumeException if an RPC client connection could not be opened
     */
    private void createConnection() throws FlumeException {
        // if already connected, just skip
        if (sessionFactory != null) {
            return;
        }

        try {
            TubeClientConfig conf = initTubeConfig(masterHostAndPortList);
            sessionFactory = new TubeMultiSessionFactory(conf);
            logger.info("create tube connection successfully");
        } catch (TubeClientException e) {
            logger.error("create connnection error in tubesink, "
                    + "maybe tube master set error, please re-check. ex1 {}", e.getMessage());
            throw new FlumeException("connect to Tube error1, "
                    + "maybe zkstr/zkroot set error, please re-check");
        } catch (Throwable e) {
            logger.error("create connnection error in tubesink, "
                    + "maybe tube master set error/shutdown in progress, please re-check. ex2 {}", e);
            throw new FlumeException("connect to meta error2, "
                    + "maybe tube master set error/shutdown in progress, please re-check");
        }

        if (producerMap == null) {
            producerMap = new HashMap<String, MessageProducer>();
        }
    }

    private TubeClientConfig initTubeConfig(String masterHostAndPortList) throws Exception {
        final TubeClientConfig tubeClientConfig = new TubeClientConfig(masterHostAndPortList);
        tubeClientConfig.setLinkMaxAllowedDelayedMsgCount(linkMaxAllowedDelayedMsgCount);
        tubeClientConfig.setSessionWarnDelayedMsgCount(sessionWarnDelayedMsgCount);
        tubeClientConfig.setSessionMaxAllowedDelayedMsgCount(sessionMaxAllowedDelayedMsgCount);
        tubeClientConfig.setNettyWriteBufferHighWaterMark(nettyWriteBufferHighWaterMark);
        tubeClientConfig.setHeartbeatPeriodMs(15000L);
        tubeClientConfig.setRpcTimeoutMs(20000L);

        return tubeClientConfig;
    }

    private void destroyConnection() {
        for (Map.Entry<String, MessageProducer> entry : producerMap.entrySet()) {
            MessageProducer producer = entry.getValue();
            try {
                producer.shutdown();
            } catch (TubeClientException e) {
                logger.error("destroy producer error in tubesink, MetaClientException {}", e.getMessage());
            } catch (Throwable e) {
                logger.error("destroy producer error in tubesink, ex {}", e.getMessage());
            }
        }
        producerMap.clear();

        if (sessionFactory != null) {
            try {
                sessionFactory.shutdown();
            } catch (Exception e) {
                logger.error("destroy sessionFactory error in tubesink, ex {}", e.getMessage());
            }
        }
        sessionFactory = null;
        logger.debug("closed meta producer");
    }

    /**
     * Currently, all topics are published by the same producer. If needed, extend it to multi producers.
     *
     * @param topic
     * @throws TubeClientException
     */
    private void initTopicProducer(String topic) throws TubeClientException {
        if (StringUtils.isEmpty(topic)) {
            logger.error("topic is empty");
            return;
        }
        if (sessionFactory == null) {
            throw new TubeClientException("sessionFactory is null, can't create producer");
        }

        if (producer == null) {
            producer = sessionFactory.createProducer();
        }

        producer.publish(topic);
        producerMap.put(topic, producer);
        logger.info(getName() + " success publish topic: " + topic);
    }

    private MessageProducer getProducer(String topic) throws TubeClientException {
        if (producerMap.containsKey(topic)) {
            return producerMap.get(topic);
        } else {
            synchronized (this) {
                if (!producerMap.containsKey(topic)) {
                    if (producer == null) {
                        producer = sessionFactory.createProducer();
                    }
                    // publish topic
                    producer.publish(topic);
                    producerMap.put(topic, producer);
                }
            }
            return producerMap.get(topic);
        }

    }

    static class TubePerformanceTask implements Runnable {

        @Override
        public void run() {
            try {
                if (totalTubeSuccSendSize.get() != 0) {
                    logger.info("Total tube performance tps :"
                            + totalTubeSuccSendCnt.get() / PRINT_INTERVAL
                            + "/s, avg msg size:"
                            + totalTubeSuccSendSize.get() / totalTubeSuccSendCnt.get()
                            + ",print every " + PRINT_INTERVAL + " seconds");

                    // totalpulsarSuccSendCnt represents the number of packets
                    totalTubeSuccSendSize.set(0);
                    totalTubeSuccSendCnt.set(0);
                }

            } catch (Exception e) {
                logger.info("tubePerformanceTask error", e);
            }
        }
    }

    class SinkTask implements Runnable {

        @Override
        public void run() {
            logger.info("Sink task {} started.", Thread.currentThread().getName());
            while (canSend) {
                boolean decrementFlag = false;
                Event event = null;
                EventStat es = null;
                String topic = null;
                try {
                    if (TubeSink.this.overflow) {
                        TubeSink.this.overflow = false;
                        Thread.sleep(10);
                    }
                    if (!resendQueue.isEmpty()) {
                        // Send the data in the retry queue first
                        es = resendQueue.poll();
                        if (es != null) {
                            event = es.getEvent();
                            if (event.getHeaders().containsKey(TOPIC)) {
                                topic = event.getHeaders().get(TOPIC);
                            }
                        }
                    } else {
                        event = eventQueue.take();
                        es = new EventStat(event);
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
                            continue;
                        } else {

                            illegalTopicMap.remove(topic);
                        }
                    }

                    final EventStat eventStat = es;
                    boolean sendResult = sendMessage(event, topic, eventStat);
                    if (!sendResult) {
                        continue;
                    }

                    decrementFlag = true;

                } catch (InterruptedException e) {
                    logger.info("Thread {} has been interrupted!", Thread.currentThread().getName());
                    return;
                } catch (Throwable throwable) {
                    if (throwable instanceof TubeClientException) {
                        String message = throwable.getMessage();
                        if (message != null && (message.contains("No available queue for topic")
                                || message.contains("The brokers of topic are all forbidden"))) {
                            illegalTopicMap.put(topic, System.currentTimeMillis() + 60 * 1000);
                            logger.info("IllegalTopicMap.put " + topic);
                            continue;
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                // ignore..
                            }
                        }
                    }
                    resendEvent(es, decrementFlag);
                }
            }
        }

        private boolean sendMessage(Event event, String topic, EventStat es)
                throws TubeClientException, InterruptedException {
            MessageProducer producer = getProducer(topic);
            if (producer == null) {
                illegalTopicMap.put(topic, System.currentTimeMillis() + 30 * 1000);
                logger.error("Get producer is null, topic:{}", topic);
                return false;
            }

            Message message = new Message(topic, event.getBody());
            message.setAttrKeyVal("auditIp", localIp);
            String streamId = "";
            String groupId = "";
            if (event.getHeaders().containsKey(org.apache.inlong.audit.consts.AttributeConstants.INLONG_STREAM_ID)) {
                streamId = event.getHeaders().get(org.apache.inlong.audit.consts.AttributeConstants.INLONG_STREAM_ID);
                message.setAttrKeyVal(org.apache.inlong.audit.consts.AttributeConstants.INLONG_STREAM_ID, streamId);
            }
            if (event.getHeaders().containsKey(org.apache.inlong.audit.consts.AttributeConstants.INLONG_GROUP_ID)) {
                groupId = event.getHeaders().get(org.apache.inlong.audit.consts.AttributeConstants.INLONG_GROUP_ID);
                message.setAttrKeyVal(org.apache.inlong.audit.consts.AttributeConstants.INLONG_GROUP_ID, groupId);
            }

            logger.debug("producer start to send msg...");
            producer.sendMessage(message, new MyCallback(es));

            illegalTopicMap.remove(topic);
            return true;
        }
    }

    public class MyCallback implements MessageSentCallback {

        private org.apache.inlong.audit.sink.EventStat myEventStat;
        private long sendTime;

        public MyCallback(org.apache.inlong.audit.sink.EventStat eventStat) {
            this.myEventStat = eventStat;
            this.sendTime = System.currentTimeMillis();
        }

        @Override
        public void onMessageSent(final MessageSentResult result) {
            if (result.isSuccess()) {
                handleMessageSendSuccess(myEventStat);
                return;
            }

            // handle sent error
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
            myEventStat.incRetryCnt();
            resendEvent(myEventStat, true);

        }

        @Override
        public void onException(final Throwable e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof OverflowException) {
                org.apache.inlong.audit.sink.TubeSink.this.overflow = true;
            }
            myEventStat.incRetryCnt();
            resendEvent(myEventStat, true);
        }
    }

}
