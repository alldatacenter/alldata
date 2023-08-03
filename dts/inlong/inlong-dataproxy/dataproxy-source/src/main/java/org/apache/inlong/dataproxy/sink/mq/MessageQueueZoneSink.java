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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.dataproxy.config.CommonConfigHolder;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.consts.StatConstants;
import org.apache.inlong.dataproxy.utils.BufferQueue;
import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxyPackEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MessageQueueZoneSink
 */
public class MessageQueueZoneSink extends AbstractSink implements Configurable, ConfigUpdateCallback {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueZoneSink.class);
    // log print count
    private static final LogCounter logCounter = new LogCounter(10, 100000, 30 * 1000);

    private final long MQ_CLUSTER_STATUS_CHECK_DUR_MS = 2000L;

    private Context parentContext;
    private MessageQueueZoneSinkContext context;
    private final List<MessageQueueZoneWorker> workers = new ArrayList<>();
    // message group
    private BatchPackManager dispatchManager;
    private final BufferQueue<PackProfile> dispatchQueue =
            new BufferQueue<>(CommonConfigHolder.getInstance().getMaxBufferQueueSizeKb());
    // scheduled thread pool
    // reload
    // dispatch
    private ScheduledExecutorService scheduledPool;

    private MessageQueueZoneProducer zoneProducer;
    // configure change notify
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final Condition condition = reentrantLock.newCondition();
    private final AtomicLong lastNotifyTime = new AtomicLong(0);
    // changeListerThread
    private Thread configListener;
    private volatile boolean isShutdown = false;
    // whether mq cluster connected
    private volatile boolean mqClusterStarted = false;

    /**
     * configure
     * 
     * @param context the sink context
     */
    @Override
    public void configure(Context context) {
        logger.info("{} start to configure, context:{}.", this.getName(), context.toString());
        this.parentContext = context;
    }

    /**
     * start
     */
    @Override
    public void start() {
        if (getChannel() == null) {
            logger.error("{}'s channel is null", this.getName());
        }
        try {
            ConfigManager.getInstance().regMetaConfigChgCallback(this);
            this.context = new MessageQueueZoneSinkContext(this, parentContext, getChannel());
            this.context.start();
            this.dispatchManager = new BatchPackManager(this, parentContext);
            this.scheduledPool = Executors.newScheduledThreadPool(2);
            // dispatch
            this.scheduledPool.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    dispatchManager.setNeedOutputOvertimeData();
                    zoneProducer.clearExpiredProducers();
                }
            }, this.dispatchManager.getDispatchTimeout(), this.dispatchManager.getDispatchTimeout(),
                    TimeUnit.MILLISECONDS);
            // create producer
            this.zoneProducer = new MessageQueueZoneProducer(this, this.context);
            this.zoneProducer.start();
            // start configure change listener thread
            this.configListener = new Thread(new ConfigChangeProcessor());
            this.configListener.setName(getName() + "-configure-listener");
            this.configListener.start();
            // create worker
            MessageQueueZoneWorker zoneWorker;
            for (int i = 0; i < context.getMaxThreads(); i++) {
                zoneWorker = new MessageQueueZoneWorker(this, i,
                        context.getProcessInterval(), zoneProducer);
                zoneWorker.start();
                this.workers.add(zoneWorker);
            }
        } catch (Exception e) {
            logger.error("{} start failure", this.getName(), e);
        }
        super.start();
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        this.isShutdown = true;
        // stop configure listener thread
        if (this.configListener != null) {
            try {
                this.configListener.interrupt();
                configListener.join();
                this.configListener = null;
            } catch (Throwable ee) {
                //
            }
        }
        // stop queue worker
        for (MessageQueueZoneWorker worker : workers) {
            try {
                worker.close();
            } catch (Throwable e) {
                logger.error("{} stop Zone worker failure", this.getName(), e);
            }
        }
        this.context.close();
        super.stop();
    }

    /**
     * process
     * 
     * @return  Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
        // wait mq cluster started
        while (!mqClusterStarted) {
            try {
                Thread.sleep(MQ_CLUSTER_STATUS_CHECK_DUR_MS);
            } catch (InterruptedException e1) {
                return Status.BACKOFF;
            } catch (Throwable e2) {
                //
            }
        }
        this.dispatchManager.outputOvertimeData();
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            Event event = channel.take();
            // no data
            if (event == null) {
                tx.commit();
                return Status.BACKOFF;
            }
            context.fileMetricIncSumStats(StatConstants.EVENT_SINK_EVENT_TAKE_SUCCESS);
            // ProxyEvent
            if (event instanceof ProxyEvent) {
                ProxyEvent proxyEvent = (ProxyEvent) event;
                this.dispatchManager.addEvent(proxyEvent);
                tx.commit();
                return Status.READY;
            }
            // ProxyPackEvent
            if (event instanceof ProxyPackEvent) {
                ProxyPackEvent packEvent = (ProxyPackEvent) event;
                this.dispatchManager.addPackEvent(packEvent);
                tx.commit();
                return Status.READY;
            }
            // SimpleEvent, send as is
            if (event instanceof SimpleEvent) {
                SimpleEvent simpleEvent = (SimpleEvent) event;
                this.dispatchManager.addSimpleEvent(simpleEvent);
                tx.commit();
                return Status.READY;
            }
            // file event
            if (StringUtils.isEmpty(event.getHeaders().get(ConfigConstants.MSG_ENCODE_VER))) {
                String groupId = event.getHeaders().get(EventConstants.INLONG_GROUP_ID);
                String streamId = event.getHeaders().get(EventConstants.INLONG_STREAM_ID);
                String msgTimeStr = event.getHeaders().get(EventConstants.HEADER_KEY_MSG_TIME);
                String sourceIp = event.getHeaders().get(EventConstants.HEADER_KEY_SOURCE_IP);
                String sourceTimeStr = event.getHeaders().get(EventConstants.HEADER_KEY_SOURCE_TIME);
                if (groupId != null
                        && streamId != null
                        && msgTimeStr != null
                        && sourceIp != null
                        && sourceTimeStr != null) {
                    ProxyEvent proxyEvent = new ProxyEvent(groupId, streamId, msgTimeStr,
                            sourceIp, sourceTimeStr, event.getHeaders(), event.getBody());
                    this.dispatchManager.addEvent(proxyEvent);
                    context.fileMetricIncSumStats(StatConstants.EVENT_SINK_EVENT_V1_FILE);
                } else {
                    context.fileMetricIncSumStats(StatConstants.EVENT_SINK_EVENT_V1_MALFORMED);
                }
            } else {
                SimpleEvent simpleEvent = new SimpleEvent();
                simpleEvent.setBody(event.getBody());
                simpleEvent.setHeaders(event.getHeaders());
                this.dispatchManager.addSimpleEvent(simpleEvent);
                context.fileMetricIncSumStats(StatConstants.EVENT_SINK_EVENT_V0_FILE);
            }
            tx.commit();
            return Status.READY;
        } catch (Throwable t) {
            context.fileMetricIncSumStats(StatConstants.EVENT_SINK_EVENT_TAKE_FAILURE);
            if (logCounter.shouldPrint()) {
                logger.error("{} process event failed!", this.getName(), t);
            }
            try {
                tx.rollback();
            } catch (Throwable e) {
                if (logCounter.shouldPrint()) {
                    logger.error("{} channel take transaction rollback exception", this.getName(), e);
                }
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }
    public boolean isMqClusterStarted() {
        return mqClusterStarted;
    }

    public void setMQClusterStarted() {
        this.mqClusterStarted = true;
    }

    public void acquireAndOfferDispatchedRecord(PackProfile record) {
        this.dispatchQueue.acquire(record.getSize());
        this.dispatchQueue.offer(record);
    }

    public void offerDispatchRecord(PackProfile record) {
        this.dispatchQueue.offer(record);
    }

    public PackProfile pollDispatchedRecord() {
        return this.dispatchQueue.pollRecord();
    }

    public PackProfile takeDispatchedRecord() {
        return this.dispatchQueue.takeRecord();
    }

    public void releaseAcquiredSizePermit(PackProfile record) {
        this.dispatchQueue.release(record.getSize());
    }

    public int getDispatchQueueSize() {
        return this.dispatchQueue.size();
    }

    public int getDispatchAvailablePermits() {
        return this.dispatchQueue.availablePermits();
    }

    @Override
    public void update() {
        if (zoneProducer == null) {
            return;
        }
        reentrantLock.lock();
        try {
            lastNotifyTime.set(System.currentTimeMillis());
            condition.signal();
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * ConfigChangeProcessor
     *
     * Metadata configuration change listener class, when the metadata change notification
     * arrives, check and change the mapping relationship between the mq cluster information
     * and the configured inlongid to Topic,
     */
    private class ConfigChangeProcessor implements Runnable {

        @Override
        public void run() {
            long lastCheckTime;
            logger.info("{} config-change processor start!", getName());
            while (!isShutdown) {
                reentrantLock.lock();
                try {
                    condition.await();
                } catch (InterruptedException e1) {
                    logger.info("{} config-change processor meet interrupt, break!", getName());
                    break;
                } finally {
                    reentrantLock.unlock();
                }
                if (zoneProducer == null) {
                    continue;
                }
                do {
                    lastCheckTime = lastNotifyTime.get();
                    zoneProducer.reloadMetaConfig();
                } while (lastCheckTime != lastNotifyTime.get());
            }
            logger.info("{} config-change processor exit!", getName());
        }
    }
}
