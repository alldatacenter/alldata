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
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.audit.base.HighPriorityThreadFactory;
import org.apache.inlong.audit.file.ConfigManager;
import org.apache.inlong.audit.utils.FailoverChannelProcessorHolder;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.common.pojo.audit.MQInfo;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(KafkaSink.class);

    // for kafka producer
    private static Properties properties = new Properties();
    private String kafkaServerUrl;
    private static final String BOOTSTRAP_SERVER = "bootstrap_servers";
    private static final String TOPIC = "topic";
    private static final String RETRIES = "retries";
    private static final String BATCH_SIZE = "batch_size";
    private static final String LINGER_MS = "linger_ms";
    private static final String BUFFER_MEMORY = "buffer_memory";
    private static final String defaultRetries = "0";
    private static final String defaultBatchSize = "16384";
    private static final String defaultLingerMs = "0";
    private static final String defaultBufferMemory = "33554432";
    private static final String defaultAcks = "all";

    private static final Long PRINT_INTERVAL = 30L;
    private static final KafkaPerformanceTask kafkaPerformanceTask = new KafkaPerformanceTask();
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
            new HighPriorityThreadFactory("kafkaPerformance-Printer-thread"));

    private KafkaProducer<String, byte[]> producer;
    public Map<String, KafkaProducer<String, byte[]>> producerMap;
    private SinkCounter sinkCounter;
    private String topic;
    private volatile boolean canSend = false;
    private volatile boolean canTake = false;
    private int threadNum;
    private Thread[] sinkThreadPool;

    private static final int BAD_EVENT_QUEUE_SIZE = 10000;
    private static final int EVENT_QUEUE_SIZE = 1000;
    private static final int DEFAULT_LOG_EVERY_N_EVENTS = 100000;
    private LinkedBlockingQueue<EventStat> resendQueue;
    private LinkedBlockingQueue<Event> eventQueue;

    // for log
    private Integer logEveryNEvents;
    private long diskIORatePerSec;
    private RateLimiter diskRateLimiter;

    // properties for stat
    private static final String LOG_EVERY_N_EVENTS = "log_every_n_events";
    private static final String DISK_IO_RATE_PER_SEC = "disk_io_rate_per_sec";
    private static final String SINK_THREAD_NUM = "thread-num";

    // for stas
    private AtomicLong currentSuccessSendCnt = new AtomicLong(0);
    private AtomicLong lastSuccessSendCnt = new AtomicLong(0);
    private long t1 = System.currentTimeMillis();
    private long t2 = 0L;
    private static AtomicLong totalKafkaSuccSendCnt = new AtomicLong(0);
    private static AtomicLong totalKafkaSuccSendSize = new AtomicLong(0);

    private boolean overflow = false;

    private String localIp = "127.0.0.1";

    static {
        // stat kafka performance
        logger.info("init kafkaPerformanceTask");
        scheduledExecutorService.scheduleWithFixedDelay(kafkaPerformanceTask, 0L,
                PRINT_INTERVAL, TimeUnit.SECONDS);
    }

    public KafkaSink() {
        super();
        logger.debug("new instance of KafkaSink!");
    }

    @Override
    public synchronized void start() {
        logger.info("kafka sink starting");
        // create connection

        sinkCounter.start();
        super.start();
        this.canSend = true;
        this.canTake = true;

        // init topic producer
        initTopicProducer(topic);

        for (int i = 0; i < sinkThreadPool.length; i++) {
            sinkThreadPool[i] = new Thread(new SinkTask(), getName() + "_tube_sink_sender-" + i);
            sinkThreadPool[i].start();
        }
        logger.debug("kafka sink started");
    }

    @Override
    public synchronized void stop() {
        logger.info("kafka sink stopping");
        // stop connection
        this.canTake = false;
        int waitCount = 0;
        while (eventQueue.size() != 0 && waitCount++ < 10) {
            try {
                Thread.sleep(1000);
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
        logger.debug("kafka sink stopped. Metrics:{}", sinkCounter);
    }

    @Override
    public Status process() {
        logger.info("kafka sink processing");
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
                    logger.info("[{}] Channel --> Queue(not enough space, current code point) "
                            + "--> Kafka, check if Kafka server or network is ok. (If this situation "
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
                logger.error("Kafka Sink transaction rollback exception = {}", e);
            }
        } finally {
            tx.close();
        }
        return status;
    }

    @Override
    public void configure(Context context) {
        logger.info("KafkaSink started and context = {}", context.toString());

        topic = context.getString(TOPIC);
        Preconditions.checkState(StringUtils.isNotEmpty(topic), "No topic specified");

        producerMap = new HashMap<>();

        logEveryNEvents = context.getInteger(LOG_EVERY_N_EVENTS, DEFAULT_LOG_EVERY_N_EVENTS);
        logger.debug(this.getName() + " " + LOG_EVERY_N_EVENTS + " " + logEveryNEvents);
        Preconditions.checkArgument(logEveryNEvents > 0, "logEveryNEvents must be > 0");

        resendQueue = new LinkedBlockingQueue<>(BAD_EVENT_QUEUE_SIZE);

        String sinkThreadNum = context.getString(SINK_THREAD_NUM, "4");
        threadNum = Integer.parseInt(sinkThreadNum);
        Preconditions.checkArgument(threadNum > 0, "threadNum must be > 0");
        sinkThreadPool = new Thread[threadNum];
        eventQueue = new LinkedBlockingQueue<>(EVENT_QUEUE_SIZE);

        diskIORatePerSec = context.getLong(DISK_IO_RATE_PER_SEC, 0L);
        if (diskIORatePerSec != 0) {
            diskRateLimiter = RateLimiter.create(diskIORatePerSec);
        }

        if (sinkCounter == null) {
            sinkCounter = new SinkCounter(getName());
        }

        localIp = NetworkUtils.getLocalIp();

        properties = new Properties();
        properties.put(ProducerConfig.ACKS_CONFIG, defaultAcks);
        properties.put(ProducerConfig.RETRIES_CONFIG, context.getString(RETRIES, defaultRetries));
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, context.getString(BATCH_SIZE, defaultBatchSize));
        properties.put(ProducerConfig.LINGER_MS_CONFIG, context.getString(LINGER_MS, defaultLingerMs));
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, context.getString(BUFFER_MEMORY, defaultBufferMemory));
    }

    private void initTopicProducer(String topic) {
        ConfigManager configManager = ConfigManager.getInstance();
        List<MQInfo> mqInfoList = configManager.getMqInfoList();
        mqInfoList.forEach(mqClusterInfo -> {
            if (MQType.KAFKA.equals(mqClusterInfo.getMqType())) {
                kafkaServerUrl = mqClusterInfo.getUrl();
            }
        });
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUrl);

        if (StringUtils.isEmpty(topic)) {
            logger.error("topic is empty");
        }

        if (producer == null) {
            producer = new KafkaProducer<>(properties, new StringSerializer(), new ByteArraySerializer());
        }

        producerMap.put(topic, producer);
        logger.info(getName() + " success create producer");
    }

    private KafkaProducer<String, byte[]> getProducer(String topic) {
        if (!producerMap.containsKey(topic)) {
            synchronized (this) {
                if (!producerMap.containsKey(topic)) {
                    if (producer == null) {
                        producer = new KafkaProducer<>(properties);
                    }
                    producerMap.put(topic, producer);
                }
            }
        }
        return producerMap.get(topic);
    }

    static class KafkaPerformanceTask implements Runnable {

        @Override
        public void run() {
            try {
                if (totalKafkaSuccSendSize.get() != 0) {
                    logger.info("Total kafka performance tps: "
                            + totalKafkaSuccSendCnt.get() / PRINT_INTERVAL
                            + "/s, avg msg size: "
                            + totalKafkaSuccSendSize.get() / totalKafkaSuccSendCnt.get()
                            + ", print every " + PRINT_INTERVAL + " seconds");

                    // totalKafkaSuccSendCnt represents the number of packets
                    totalKafkaSuccSendSize.set(0);
                    totalKafkaSuccSendCnt.set(0);
                }

            } catch (Exception e) {
                logger.info("tubePerformanceTask error", e);
            }
        }
    }

    public void handleMessageSendSuccess(EventStat es) {
        // Statistics tube performance
        totalKafkaSuccSendCnt.incrementAndGet();
        totalKafkaSuccSendSize.addAndGet(es.getEvent().getBody().length);

        // add to sinkCounter
        sinkCounter.incrementEventDrainSuccessCount();
        currentSuccessSendCnt.incrementAndGet();
        long nowCnt = currentSuccessSendCnt.get();
        long oldCnt = lastSuccessSendCnt.get();
        if (nowCnt % logEveryNEvents == 0 && nowCnt != lastSuccessSendCnt.get()) {
            lastSuccessSendCnt.set(nowCnt);
            t2 = System.currentTimeMillis();
            logger.info("KafkaSink {}, succ put {} events to kafka, in the past {} millisecond",
                    getName(), (nowCnt - oldCnt), (t2 - t1));
            t1 = t2;
        }
    }

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

    class SinkTask implements Runnable {

        @Override
        public void run() {
            logger.info("Sink task {} started.", Thread.currentThread().getName());
            while (canSend) {
                boolean decrementFlag = false;
                Event event = null;
                EventStat eventStat = null;
                try {
                    if (KafkaSink.this.overflow) {
                        KafkaSink.this.overflow = false;
                        Thread.sleep(10);
                    }
                    if (!resendQueue.isEmpty()) {
                        // Send the data in the retry queue first
                        eventStat = resendQueue.poll();
                        if (eventStat != null) {
                            event = eventStat.getEvent();
                        }
                    } else {
                        event = eventQueue.take();
                        eventStat = new EventStat(event);
                        sinkCounter.incrementEventDrainAttemptCount();
                    }

                    if (event == null || StringUtils.isBlank(topic)) {
                        logger.warn("event is null or no topic specified in event header, just skip");
                        continue;
                    }

                    final EventStat es = eventStat;
                    boolean sendResult = sendMessage(event, topic, es);
                    if (!sendResult) {
                        continue;
                    }

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
                                // ignore..
                            }
                        }
                    }
                    resendEvent(eventStat, decrementFlag);
                }
            }
        }

        private boolean sendMessage(Event event, String topic, EventStat es) {
            KafkaProducer<String, byte[]> producer = getProducer(topic);
            if (producer == null) {
                logger.error("Get producer is null, topic:{}", topic);
                return false;
            }

            logger.debug("producer start to send msg...");
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, event.getBody());
            producer.send(record, (recordMetadata, e) -> {

                if (e == null) {
                    handleMessageSendSuccess(es);
                    return;
                } else {
                    logger.warn("Send message failed, error message: {}, resendQueue size: {}, event:{}",
                            e.getMessage(), resendQueue.size(), es.getEvent().hashCode());
                }

                es.incRetryCnt();
                resendEvent(es, true);
            });
            return true;
        }
    }
}
