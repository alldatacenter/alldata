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

package org.apache.inlong.sort.standalone.sink.clickhouse;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.dispatch.DispatchManager;
import org.apache.inlong.sort.standalone.dispatch.DispatchProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ClickHouseSink
 */
public class ClickHouseSink extends AbstractSink implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(ClickHouseSink.class);

    private Context parentContext;
    private ClickHouseSinkContext context;
    private DispatchManager dispatchManager;
    private LinkedBlockingQueue<DispatchProfile> dispatchQueue = new LinkedBlockingQueue<>();
    // workers
    private List<ClickHouseChannelWorker> workers = new ArrayList<>();
    // schedule
    private ScheduledExecutorService scheduledPool;

    /**
     * start
     */
    @Override
    public void start() {
        super.start();
        try {
            this.context = new ClickHouseSinkContext(getName(), parentContext, getChannel(), dispatchQueue);
            this.context.start();
            for (int i = 0; i < context.getMaxThreads(); i++) {
                ClickHouseChannelWorker worker = new ClickHouseChannelWorker(context, i);
                this.workers.add(worker);
                worker.start();
            }
            this.dispatchManager = new DispatchManager(parentContext, dispatchQueue);
            this.scheduledPool = Executors.newScheduledThreadPool(1);
            // dispatch
            this.scheduledPool.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    dispatchManager.setNeedOutputOvertimeData();
                }
            }, this.dispatchManager.getDispatchTimeout(), this.dispatchManager.getDispatchTimeout(),
                    TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        super.stop();
        try {
            for (ClickHouseChannelWorker worker : this.workers) {
                worker.close();
            }
            this.context.close();
            this.scheduledPool.shutdown();
            super.stop();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        LOG.info("start to configure:{}, context:{}.", this.getName(), context.toString());
        this.parentContext = context;
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
            if (event == null) {
                tx.commit();
                return Status.BACKOFF;
            }
            if (!(event instanceof ProfileEvent)) {
                tx.commit();
                this.context.addSendFailMetric("event is not ProfileEvent");
                return Status.READY;
            }
            //
            ProfileEvent profileEvent = (ProfileEvent) event;
            this.dispatchManager.addEvent(profileEvent);
            tx.commit();
            return Status.READY;
        } catch (Throwable t) {
            LOG.error("Process event failed!" + t.getMessage(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                LOG.error("Channel take transaction rollback exception:" + e.getMessage(), e);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }

}
