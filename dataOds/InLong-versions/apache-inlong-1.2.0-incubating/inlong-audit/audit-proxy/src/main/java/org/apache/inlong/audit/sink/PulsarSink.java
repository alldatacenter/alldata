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
import io.netty.handler.codec.TooLongFrameException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.audit.base.HighPriorityThreadFactory;
import org.apache.inlong.audit.sink.pulsar.CreatePulsarClientCallBack;
import org.apache.inlong.audit.sink.pulsar.PulsarClientService;
import org.apache.inlong.audit.sink.pulsar.SendMessageCallBack;
import org.apache.inlong.audit.utils.FailoverChannelProcessorHolder;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.PulsarClientException.AlreadyClosedException;
import org.apache.pulsar.client.api.PulsarClientException.NotConnectedException;
import org.apache.pulsar.client.api.PulsarClientException.ProducerQueueIsFullError;
import org.apache.pulsar.client.api.PulsarClientException.TopicTerminatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * pulsar sink
 *
 * send to one pulsar cluster
 */
public class PulsarSink extends AbstractSink implements Configurable, SendMessageCallBack,
        CreatePulsarClientCallBack {
    private static final Logger logger = LoggerFactory.getLogger(PulsarSink.class);

    /*
     * properties for header info
     */
    private static String TOPIC = "topic";

    /*
     * default value
     */
    private static int BAD_EVENT_QUEUE_SIZE = 10000;
    private static int BATCH_SIZE = 10000;
    private static final int DEFAULT_LOG_EVERY_N_EVENTS = 100000;

    /*
     * properties for stat
     */
    private static String LOG_EVERY_N_EVENTS = "log_every_n_events";

    private static String DISK_IO_RATE_PER_SEC = "disk_io_rate_per_sec";

    private static final String SINK_THREAD_NUM = "thread_num";

    /*
     * for log
     */
    private Integer logEveryNEvents;

    private long diskIORatePerSec;

    private RateLimiter diskRateLimiter;

    /*
     * for stat
     */
    private AtomicLong currentSuccessSendCnt = new AtomicLong(0);

    private AtomicLong lastSuccessSendCnt = new AtomicLong(0);

    private long t1 = System.currentTimeMillis();

    private long t2 = 0L;

    private static AtomicLong totalPulsarSuccSendCnt = new AtomicLong(0);

    private static AtomicLong totalPulsarSuccSendSize = new AtomicLong(0);
    /*
     * for control
     */
    private boolean overflow = false;

    private LinkedBlockingQueue<EventStat> resendQueue;

    private long logCounter = 0;

    private final AtomicLong currentInFlightCount = new AtomicLong(0);

    /*
     * whether the SendTask thread can send data to pulsar
     */
    private volatile boolean canSend = false;

    /*
     * Control whether the SinkRunner thread can read data from the Channel
     */
    private volatile boolean canTake = false;


    private static int EVENT_QUEUE_SIZE = 1000;

    private int threadNum;


    /*
     * send thread pool
     */
    private Thread[] sinkThreadPool;
    private LinkedBlockingQueue<Event> eventQueue;

    private SinkCounter sinkCounter;

    private PulsarClientService pulsarClientService;

    private static final Long PRINT_INTERVAL = 30L;

    private static final PulsarPerformanceTask pulsarPerformanceTask = new PulsarPerformanceTask();

    private static ScheduledExecutorService scheduledExecutorService = Executors
            .newScheduledThreadPool(1, new HighPriorityThreadFactory("pulsarPerformance-Printer-thread"));

    private String topic;

    static {
        /*
         * stat pulsar performance
         */
        System.out.println("pulsarPerformanceTask!!!!!!");
        scheduledExecutorService.scheduleWithFixedDelay(pulsarPerformanceTask, 0L,
                PRINT_INTERVAL, TimeUnit.SECONDS);
    }

    public PulsarSink() {
        super();
        logger.debug("new instance of PulsarSink!");
    }

    /**
     * configure
     * @param context
     */
    public void configure(Context context) {
        logger.info("PulsarSink started and context = {}", context.toString());
        /*
         * topic config
         */
        topic  = context.getString(TOPIC);
        logEveryNEvents = context.getInteger(LOG_EVERY_N_EVENTS, DEFAULT_LOG_EVERY_N_EVENTS);
        logger.debug(this.getName() + " " + LOG_EVERY_N_EVENTS + " " + logEveryNEvents);
        Preconditions.checkArgument(logEveryNEvents > 0, "logEveryNEvents must be > 0");

        resendQueue = new LinkedBlockingQueue<EventStat>(BAD_EVENT_QUEUE_SIZE);

        String sinkThreadNum = context.getString(SINK_THREAD_NUM, "4");
        threadNum = Integer.parseInt(sinkThreadNum);
        Preconditions.checkArgument(threadNum > 0, "threadNum must be > 0");
        sinkThreadPool = new Thread[threadNum];
        eventQueue = new LinkedBlockingQueue<Event>(EVENT_QUEUE_SIZE);

        diskIORatePerSec = context.getLong(DISK_IO_RATE_PER_SEC,0L);
        if (diskIORatePerSec != 0) {
            diskRateLimiter = RateLimiter.create(diskIORatePerSec);
        }
        pulsarClientService = new PulsarClientService(context);

        if (sinkCounter == null) {
            sinkCounter = new SinkCounter(getName());
        }
    }

    private void initTopic() throws Exception {
        long startTime = System.currentTimeMillis();
        if (topic != null) {
            pulsarClientService.initTopicProducer(topic);
        }
        logger.info(getName() + " initTopic cost: "
                + (System.currentTimeMillis() - startTime) + "ms");
    }

    @Override
    public void start() {
        logger.info("pulsar sink starting...");
        sinkCounter.start();
        pulsarClientService.initCreateConnection(this);

        super.start();
        this.canSend = true;
        this.canTake = true;
        try {
            initTopic();
        } catch (Exception e) {
            logger.info("meta sink start publish topic fail.",e);
        }

        for (int i = 0; i < sinkThreadPool.length; i++) {
            sinkThreadPool[i] = new Thread(new SinkTask(), getName()
                    + "_pulsar_sink_sender-"
                    + i);
            sinkThreadPool[i].start();
        }
        logger.debug("meta sink started");
    }

    @Override
    public void stop() {
        logger.info("pulsar sink stopping");
        pulsarClientService.close();
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
        logger.debug("pulsar sink stopped. Metrics:{}", sinkCounter);
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
                            + "--> pulsar,Check if pulsar server or network is ok.(if this situation "
                            + "last long time it will cause memoryChannel full and fileChannel write.)", getName());
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
                logger.error("Pulsar Sink transaction rollback exception = {}", e);
            }
        } finally {
            tx.close();
        }
        return status;
    }

    @Override
    public void handleCreateClientSuccess(String url) {
        logger.info("createConnection success for url = {}", url);
        sinkCounter.incrementConnectionCreatedCount();
    }

    @Override
    public void handleCreateClientException(String url) {
        logger.error("createConnection has exception for url = {}", url);
        sinkCounter.incrementConnectionFailedCount();
    }

    @Override
    public void handleMessageSendSuccess(Object result,  EventStat eventStat) {
        /*
         * Statistics pulsar performance
         */
        totalPulsarSuccSendCnt.incrementAndGet();
        totalPulsarSuccSendSize.addAndGet(eventStat.getEvent().getBody().length);
        /*
         *add to sinkCounter
         */
        sinkCounter.incrementEventDrainSuccessCount();
        currentInFlightCount.decrementAndGet();
        currentSuccessSendCnt.incrementAndGet();
        long nowCnt = currentSuccessSendCnt.get();
        long oldCnt = lastSuccessSendCnt.get();
        if (nowCnt % logEveryNEvents == 0 && nowCnt != lastSuccessSendCnt.get()) {
            lastSuccessSendCnt.set(nowCnt);
            t2 = System.currentTimeMillis();
            logger.info("metasink {}, succ put {} events to pulsar,"
                    + " in the past {} millsec", new Object[] {
                    getName(), (nowCnt - oldCnt), (t2 - t1)
            });
            t1 = t2;
        }
    }

    @Override
    public void handleMessageSendException(EventStat eventStat,  Object e) {
        if (e instanceof TooLongFrameException) {
            PulsarSink.this.overflow = true;
        } else if (e instanceof ProducerQueueIsFullError) {
            PulsarSink.this.overflow = true;
        } else if (!(e instanceof AlreadyClosedException
                || e instanceof NotConnectedException
                || e instanceof TopicTerminatedException)) {
            logger.error("handle message send exception ,msg will resend later, e = {}", e);
        }
        eventStat.incRetryCnt();
        resendEvent(eventStat, true);
    }

    /**
     * Resend the data and store the data in the memory cache.
     * @param es
     * @param isDecrement
     */
    private void resendEvent(EventStat es, boolean isDecrement) {
        try {
            if (isDecrement) {
                currentInFlightCount.decrementAndGet();
            }
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

    static class PulsarPerformanceTask implements Runnable {
        @Override
        public void run() {
            try {
                if (totalPulsarSuccSendSize.get() != 0) {
                    logger.info("Total pulsar performance tps :"
                            + totalPulsarSuccSendCnt.get() / PRINT_INTERVAL
                            + "/s, avg msg size:"
                            + totalPulsarSuccSendSize.get() / totalPulsarSuccSendCnt.get()
                            + ",print every " + PRINT_INTERVAL + " seconds");
                    /*
                     * totalpulsarSuccSendCnt represents the number of packets
                     */
                    totalPulsarSuccSendCnt.set(0);
                    totalPulsarSuccSendSize.set(0);
                }

            } catch (Exception e) {
                logger.info("pulsarPerformanceTask error", e);
            }
        }
    }

    class SinkTask implements Runnable {
        @Override
        public void run() {
            logger.info("Sink task {} started.", Thread.currentThread().getName());
            while (canSend) {
                logger.debug("SinkTask process......");
                boolean decrementFlag = false;
                Event event = null;
                EventStat eventStat = null;
                try {
                    if (PulsarSink.this.overflow) {
                        PulsarSink.this.overflow = false;
                        Thread.currentThread().sleep(10);
                    }
                    if (!resendQueue.isEmpty()) {
                        /*
                         * Send the data in the retry queue first
                         */
                        eventStat = resendQueue.poll();
                        if (eventStat != null) {
                            event = eventStat.getEvent();
                        }
                    } else {
                        if (currentInFlightCount.get() > BATCH_SIZE) {
                            /*
                             * Under the condition that the number of unresponsive messages
                             * is greater than 1w, the number of unresponsive messages sent
                             * to pulsar will be printed periodically
                             */
                            logCounter++;
                            if (logCounter == 1 || logCounter % 100000 == 0) {
                                logger.info(getName()
                                                + " currentInFlightCount={} resendQueue"
                                                + ".size={}",
                                        currentInFlightCount.get(),resendQueue.size());
                            }
                            if (logCounter > Long.MAX_VALUE - 10) {
                                logCounter = 0;
                            }
                        }
                        event = eventQueue.take();
                        eventStat = new EventStat(event);
                        sinkCounter.incrementEventDrainAttemptCount();
                    }
                    logger.debug("Event is {}, topic = {} ",event, topic);

                    if (event == null) {
                        continue;
                    }

                    if (topic == null || topic.equals("")) {
                        logger.warn("no topic specified in event header, just skip this event");
                        continue;
                    }

                    final EventStat es = eventStat;
                    boolean sendResult = pulsarClientService.sendMessage(topic, event,
                            PulsarSink.this, es);
                    if (!sendResult) {
                        continue;
                    }
                    currentInFlightCount.incrementAndGet();
                    decrementFlag = true;
                } catch (InterruptedException e) {
                    logger.info("Thread {} has been interrupted!", Thread.currentThread().getName());
                    return;
                } catch (Throwable t) {
                    if (t instanceof PulsarClientException) {
                        String message = t.getMessage();
                        if (message != null && (message.contains("No available queue for topic")
                                || message.contains("The brokers of topic are all forbidden"))) {
                            logger.info("IllegalTopicMap.put " + topic);
                            continue;
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                //ignore..
                            }
                        }
                    }
                    resendEvent(eventStat, decrementFlag);
                }
            }
        }
    }
}
