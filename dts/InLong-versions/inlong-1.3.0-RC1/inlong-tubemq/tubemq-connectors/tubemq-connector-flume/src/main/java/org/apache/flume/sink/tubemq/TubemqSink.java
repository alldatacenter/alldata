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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flume.sink.tubemq;

import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_EVENT_MAX_RETRY_TIME;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_EVENT_OFFER_TIMEOUT;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_EVENT_QUEUE_CAPACITY;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_HEARTBEAT_PERIOD;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_RETRY_QUEUE_CAPACITY;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_RPC_TIMEOUT;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT;
import static org.apache.flume.sink.tubemq.ConfigOptions.DEFAULT_SINK_THREAD_NUM;
import static org.apache.flume.sink.tubemq.ConfigOptions.EVENT_MAX_RETRY_TIME;
import static org.apache.flume.sink.tubemq.ConfigOptions.EVENT_OFFER_TIMEOUT;
import static org.apache.flume.sink.tubemq.ConfigOptions.EVENT_QUEUE_CAPACITY;
import static org.apache.flume.sink.tubemq.ConfigOptions.HEARTBEAT_PERIOD;
import static org.apache.flume.sink.tubemq.ConfigOptions.LINK_MAX_ALLOWED_DELAYED_MSG_COUNT;
import static org.apache.flume.sink.tubemq.ConfigOptions.MASTER_HOST_PORT_LIST;
import static org.apache.flume.sink.tubemq.ConfigOptions.NETTY_WRITE_BUFFER_HIGH_WATER_MARK;
import static org.apache.flume.sink.tubemq.ConfigOptions.RETRY_QUEUE_CAPACITY;
import static org.apache.flume.sink.tubemq.ConfigOptions.RPC_TIMEOUT;
import static org.apache.flume.sink.tubemq.ConfigOptions.SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT;
import static org.apache.flume.sink.tubemq.ConfigOptions.SESSION_WARN_DELAYED_MSG_COUNT;
import static org.apache.flume.sink.tubemq.ConfigOptions.SINK_THREAD_NUM;
import static org.apache.flume.sink.tubemq.ConfigOptions.TOPIC;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentCallback;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corerpc.exception.OverflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Make tubemq as one of flume sinks
 */
public class TubemqSink extends AbstractSink implements Configurable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TubemqSink.class);

    public TubeMultiSessionFactory sessionFactory;
    public ConcurrentHashMap<String, MessageProducer> producerMap;

    private String masterHostAndPortList;
    private String defaultTopic;
    private long heartbeatPeriod;
    private long rpcTimeout;

    private long linkMaxAllowedDelayedMsgCount;
    private long sessionWarnDelayedMsgCount;
    private long sessionMaxAllowedDelayedMsgCount;
    private long nettyWriteBufferHighWaterMark;

    private ExecutorService sinkThreadPool;
    private final List<Future<?>> threadFutures = new ArrayList<>();
    private int threadNum;

    private boolean started = false;
    // check if overflow
    private boolean overflow = false;

    private LinkedBlockingQueue<EventStat> resendQueue;

    private LinkedBlockingQueue<Event> eventQueue;

    private int maxRetryTime;
    private long eventOfferTimeout;

    private TubeClientConfig clientConfig;

    private TubeSinkCounter counter;

    /**
     * init tube config
     *
     * @return tube config
     */
    private TubeClientConfig initTubeConfig() {
        final TubeClientConfig tubeClientConfig = new TubeClientConfig(this.masterHostAndPortList);
        tubeClientConfig.setLinkMaxAllowedDelayedMsgCount(linkMaxAllowedDelayedMsgCount);
        tubeClientConfig.setSessionWarnDelayedMsgCount(sessionWarnDelayedMsgCount);
        tubeClientConfig.setSessionMaxAllowedDelayedMsgCount(sessionMaxAllowedDelayedMsgCount);
        tubeClientConfig.setNettyWriteBufferHighWaterMark(nettyWriteBufferHighWaterMark);
        tubeClientConfig.setHeartbeatPeriodMs(heartbeatPeriod);
        tubeClientConfig.setRpcTimeoutMs(rpcTimeout);

        return tubeClientConfig;
    }

    @VisibleForTesting
    TubeClientConfig getClientConfig() {
        return clientConfig;
    }

    @VisibleForTesting
    TubeSinkCounter getCounter() {
        return counter;
    }

    /**
     * Create producer
     *
     * @throws FlumeException
     */
    private void createConnection() throws FlumeException {
        if (sessionFactory != null) {
            return;
        }
        try {
            sessionFactory = new TubeMultiSessionFactory(clientConfig);
        } catch (TubeClientException e) {
            LOGGER.error("create connection error in tubemqSink, "
                    + "maybe tubemq master set error, please re-check. ex1 {}", e.getMessage());
            throw new FlumeException("connect to tubemq error1, please re-check", e);
        }

        if (producerMap == null) {
            producerMap = new ConcurrentHashMap<>();
        }

    }

    /**
     * Destroy all producers and clear up caches.
     */
    private void destroyConnection() {
        for (String topic : producerMap.keySet()) {
            MessageProducer producer = producerMap.get(topic);
            try {
                producer.shutdown();
            } catch (Throwable e) {
                LOGGER.error("destroy producer error in tubemqSink, ex", e);
            }
        }
        producerMap.clear();

        if (sessionFactory != null) {
            try {
                sessionFactory.shutdown();
            } catch (Exception e) {
                LOGGER.error("destroy sessionFactory error in tubemqSink, MetaClientException", e);
            }
        }
        sessionFactory = null;
        LOGGER.debug("closed meta producer");
    }

    @Override
    public void start() {
        LOGGER.info("tubemq sink starting...");

        // create connection
        try {
            createConnection();
        } catch (FlumeException e) {
            // close connection
            destroyConnection();
            LOGGER.error("Unable to create tubemq client" + ". Exception follows.", e);
        }
        started = true;
        // submit worker threads
        for (int i = 0; i < threadNum; i++) {
            threadFutures.add(sinkThreadPool.submit(new SinkTask()));
        }
        super.start();
    }

    @Override
    public void stop() {
        LOGGER.info("tubemq sink stopping");
        started = false;
        if (sinkThreadPool != null) {
            sinkThreadPool.shutdown();
        }
        for (Future<?> future : threadFutures) {
            // interrupt threads
            future.cancel(true);
        }
        threadFutures.clear();
        destroyConnection();
        super.stop();
    }

    @Override
    public Status process() {
        if (!started) {
            return Status.BACKOFF;
        }
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        Status status = Status.READY;
        try {
            Event event = channel.take();
            if (event != null) {
                if (!eventQueue.offer(event, eventOfferTimeout, TimeUnit.MILLISECONDS)) {
                    LOGGER.info("[{}] Channel --> Queue(has no enough space,current code point) --> tubemq, Check "
                            + "if tubemq server or network is ok.(if this situation last long time it will cause"
                            + " memoryChannel full and fileChannel write.)", getName());
                    counter.incrementRollbackCount();
                    tx.rollback();
                } else {
                    tx.commit();
                }
            } else {
                // if event is null, that means queue is empty, backoff it.
                status = Status.BACKOFF;
                tx.commit();
            }
        } catch (Throwable t) {
            LOGGER.error("Process event failed!" + this.getName(), t);
            try {
                counter.incrementRollbackCount();
                tx.rollback();
            } catch (Throwable e) {
                LOGGER.error("tubemq sink transaction rollback exception", e);
            }
        } finally {
            tx.close();
        }
        return status;
    }

    @Override
    public void configure(Context context) {
        LOGGER.info(context.toString());
        masterHostAndPortList = context.getString(MASTER_HOST_PORT_LIST);
        defaultTopic = context.getString(TOPIC);
        heartbeatPeriod = context.getLong(HEARTBEAT_PERIOD, DEFAULT_HEARTBEAT_PERIOD);
        rpcTimeout = context.getLong(RPC_TIMEOUT, DEFAULT_RPC_TIMEOUT);

        linkMaxAllowedDelayedMsgCount = context.getLong(
                LINK_MAX_ALLOWED_DELAYED_MSG_COUNT,
                DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT);
        sessionWarnDelayedMsgCount = context.getLong(
                SESSION_WARN_DELAYED_MSG_COUNT,
                DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT);
        sessionMaxAllowedDelayedMsgCount = context.getLong(
                SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT,
                DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT);
        nettyWriteBufferHighWaterMark = context.getLong(
                NETTY_WRITE_BUFFER_HIGH_WATER_MARK,
                DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK);

        producerMap = new ConcurrentHashMap<>();

        threadNum = context.getInteger(SINK_THREAD_NUM, DEFAULT_SINK_THREAD_NUM);
        sinkThreadPool = Executors.newFixedThreadPool(threadNum);

        int retryQueueCapacity = context.getInteger(RETRY_QUEUE_CAPACITY, DEFAULT_RETRY_QUEUE_CAPACITY);
        resendQueue = new LinkedBlockingQueue<>(retryQueueCapacity);

        int eventQueueCapacity = context.getInteger(EVENT_QUEUE_CAPACITY, DEFAULT_EVENT_QUEUE_CAPACITY);
        eventQueue = new LinkedBlockingQueue<>(eventQueueCapacity);

        maxRetryTime = context.getInteger(EVENT_MAX_RETRY_TIME, DEFAULT_EVENT_MAX_RETRY_TIME);
        eventOfferTimeout = context.getLong(EVENT_OFFER_TIMEOUT, DEFAULT_EVENT_OFFER_TIMEOUT);

        counter = new TubeSinkCounter(this.getName());

        clientConfig = initTubeConfig();
    }

    /**
     * Get producer from cache, create it if not exists.
     *
     * @param topic - topic name
     * @return MessageProducer
     * @throws TubeClientException
     */
    private MessageProducer getProducer(String topic) throws TubeClientException {
        if (!producerMap.containsKey(topic)) {
            MessageProducer producer = sessionFactory.createProducer();
            // publish topic
            producer.publish(topic);
            producerMap.putIfAbsent(topic, producer);
        }
        return producerMap.get(topic);
    }

    class SinkTask implements Runnable {

        private void sleepIfOverflow() throws Exception {
            if (overflow) {
                overflow = false;
                Thread.sleep(50);
            }
        }

        /**
         * fetch message, wait if queue is empty
         *
         * @return EventStat
         * @throws Exception
         */
        private EventStat fetchEventStat() throws Exception {
            EventStat es = null;
            if (!resendQueue.isEmpty()) {
                es = resendQueue.poll();
            } else {
                // wait if eventQueue is empty
                Event event = eventQueue.take();
                es = new EventStat(event);
            }
            return es;
        }

        private void sendEvent(MessageProducer producer, EventStat es) throws Exception {
            // send message with callback
            Message message = new Message(es.getTopic(), es.getEvent().getBody());
            producer.sendMessage(message, new MessageSentCallback() {
                @Override
                public void onMessageSent(MessageSentResult result) {
                    if (!result.isSuccess()) {
                        resendEvent(es);
                    }
                }

                @Override
                public void onException(Throwable e) {
                    LOGGER.error("exception caught", e);
                    if (e instanceof OverflowException) {
                        overflow = true;
                    }
                    resendEvent(es);
                }
            });
        }

        /**
         * Resent event
         *
         * @param es EventStat
         */
        private void resendEvent(EventStat es) {
            if (es == null || es.getEvent() == null) {
                return;
            }
            es.incRetryCnt();
            if (es.getRetryCnt() > maxRetryTime) {
                LOGGER.error("event max retry reached, ignore it");
                return;
            }

            // if resend queue is full, send back to channel
            if (!resendQueue.offer(es)) {
                getChannel().put(es.getEvent());
                LOGGER.warn("resend queue is full, size: {}, send back to channel", resendQueue.size());
            }
        }

        @Override
        public void run() {
            LOGGER.info("Sink task {} started.", Thread.currentThread().getName());
            while (started) {
                boolean decrementFlag = false;
                EventStat es = null;
                try {
                    sleepIfOverflow();
                    // fetch event, wait if necessary
                    es = fetchEventStat();
                    if (es.getTopic() == null || es.getTopic().equals("")) {
                        LOGGER.debug("no topic specified in event header, use default topic instead");
                        es.setTopic(defaultTopic);
                    }
                    counter.incrementSendCount();
                    MessageProducer producer;
                    try {
                        producer = getProducer(es.getTopic());
                        sendEvent(producer, es);
                    } catch (Exception e) {
                        LOGGER.error("Get producer failed!", e);
                    }
                } catch (InterruptedException e) {
                    LOGGER.info("Thread {} has been interrupted!", Thread.currentThread().getName());
                    return;
                } catch (Throwable t) {
                    LOGGER.error("error while sending event", t);
                    resendEvent(es);
                }
            }
        }
    }
}
