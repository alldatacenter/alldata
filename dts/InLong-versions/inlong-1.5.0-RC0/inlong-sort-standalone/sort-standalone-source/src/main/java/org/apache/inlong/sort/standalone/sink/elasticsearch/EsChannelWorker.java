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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import org.apache.flume.Channel;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EsChannelWorker
 */
public class EsChannelWorker extends Thread {

    public static final Logger LOG = LoggerFactory.getLogger(EsChannelWorker.class);

    private final EsSinkContext context;
    private final int workerIndex;

    private LifecycleState status;
    private IEvent2IndexRequestHandler handler;

    /**
     * Constructor
     * 
     * @param context
     * @param workerIndex
     */
    public EsChannelWorker(EsSinkContext context, int workerIndex) {
        this.context = context;
        this.workerIndex = workerIndex;
        this.status = LifecycleState.IDLE;
        this.handler = context.createIndexRequestHandler();
    }

    /**
     * run
     */
    @Override
    public void run() {
        status = LifecycleState.START;
        LOG.info("start to EsChannelWorker:{},status:{},index:{}", context.getTaskName(), status, workerIndex);
        while (status == LifecycleState.START) {
            try {
                this.doRun();
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        }
    }

    /**
     * doRun
     */
    public void doRun() {
        Channel channel = context.getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            Event event = channel.take();
            if (event == null) {
                tx.commit();
                Thread.sleep(context.getProcessInterval());
                return;
            }
            if (!(event instanceof ProfileEvent)) {
                tx.commit();
                this.context.addSendFailMetric();
                Thread.sleep(context.getProcessInterval());
                return;
            }
            // to profileEvent
            ProfileEvent profileEvent = (ProfileEvent) event;
            EsIndexRequest indexRequest = handler.parse(context, profileEvent);
            // offer queue
            if (indexRequest != null) {
                context.offerDispatchQueue(indexRequest);
            } else {
                context.addSendFailMetric();
                profileEvent.ack();
            }
            tx.commit();
            return;
        } catch (Throwable t) {
            LOG.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                LOG.error("Channel take transaction rollback exception:" + getName(), e);
            }
            return;
        } finally {
            tx.close();
        }
    }

    /**
     * close
     */
    public void close() {
        this.status = LifecycleState.STOP;
    }
}
