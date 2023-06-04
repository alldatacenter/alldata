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

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.dataproxy.sink.common.SinkContext;
import org.apache.inlong.dataproxy.utils.BufferQueue;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxyPackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MessageQueueZoneSink
 */
public class MessageQueueZoneSink extends AbstractSink implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(MessageQueueZoneSink.class);

    private Context parentContext;
    private MessageQueueZoneSinkContext context;
    private List<MessageQueueZoneWorker> workers = new ArrayList<>();
    // message group
    private BatchPackManager dispatchManager;
    private BufferQueue<BatchPackProfile> dispatchQueue;
    // scheduled thread pool
    // reload
    // dispatch
    private ScheduledExecutorService scheduledPool;

    private MessageQueueZoneProducer zoneProducer;

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        LOG.info("start to configure:{}, context:{}.", this.getClass().getSimpleName(), context.toString());
        this.parentContext = context;
    }

    /**
     * start
     */
    @Override
    public void start() {
        try {
            this.dispatchQueue = SinkContext.createBufferQueue();
            this.context = new MessageQueueZoneSinkContext(getName(), parentContext, getChannel(), this.dispatchQueue);
            if (getChannel() == null) {
                LOG.error("channel is null");
            }
            this.context.start();
            this.dispatchManager = new BatchPackManager(parentContext, dispatchQueue);
            this.scheduledPool = Executors.newScheduledThreadPool(2);
            // dispatch
            this.scheduledPool.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    dispatchManager.setNeedOutputOvertimeData();
                }
            }, this.dispatchManager.getDispatchTimeout(), this.dispatchManager.getDispatchTimeout(),
                    TimeUnit.MILLISECONDS);
            // create producer
            this.zoneProducer = new MessageQueueZoneProducer(this.getName(), this.context);
            this.zoneProducer.start();
            // create worker
            for (int i = 0; i < context.getMaxThreads(); i++) {
                MessageQueueZoneWorker worker = new MessageQueueZoneWorker(this.getName(), i, context, zoneProducer);
                worker.start();
                this.workers.add(worker);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        super.start();
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        for (MessageQueueZoneWorker worker : workers) {
            try {
                worker.close();
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }
        this.context.close();
        super.stop();
    }

    /**
     * process
     * 
     * @return                        Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
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
            tx.commit();
            this.context.addSendFailMetric();
            return Status.READY;
        } catch (Throwable t) {
            LOG.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                LOG.error("Channel take transaction rollback exception:" + getName(), e);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }
}
