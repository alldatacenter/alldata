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

package org.apache.inlong.sort.standalone.sink.cls;

import com.google.common.base.Preconditions;
import com.tencentcloudapi.cls.producer.AsyncProducerClient;
import com.tencentcloudapi.cls.producer.common.LogItem;
import com.tencentcloudapi.cls.producer.errors.ProducerException;
import org.apache.flume.Channel;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * Cls channel worker.
 */
public class ClsChannelWorker extends Thread {

    private static final Logger LOG = InlongLoggerFactory.getLogger(ClsChannelWorker.class);

    private final ClsSinkContext context;
    private final String workerName;
    private final Channel channel;
    private final IEvent2LogItemHandler handler;
    private LifecycleState status;

    /**
     * Constructor.
     *
     * @param sinkName Sink name.
     * @param context Cls context.
     * @param workerIndex Index of cls channel worker.
     */
    public ClsChannelWorker(String sinkName, ClsSinkContext context, int workerIndex) {
        this.context = Preconditions.checkNotNull(context);
        this.workerName = sinkName + "-" + workerIndex;
        this.channel = Preconditions.checkNotNull(context.getChannel());
        this.handler = Preconditions.checkNotNull(context.getLogItemHandler());
        this.status = LifecycleState.IDLE;
    }

    @Override
    public void start() {
        LOG.info("Start new cls channel worker {}", this.workerName);
        status = LifecycleState.START;
        super.start();
    }

    /**
     * Close cls channel worker.
     */
    public void close() {
        LOG.info("Close cls channel worker {}", this.workerName);
        status = LifecycleState.STOP;
    }

    /**
     * Run until status is STOP.
     */
    @Override
    public void run() {
        LOG.info("worker {} start to run, the state is {}", this.workerName, status.name());
        while (status != LifecycleState.STOP) {
            doRun();
        }
    }

    /**
     * Do run.
     */
    private void doRun() {
        Transaction tx = null;
        try {
            tx = channel.getTransaction();
            tx.begin();
            Event rowEvent = channel.take();

            // if event is null, close tx and sleep for a while.
            if (rowEvent == null) {
                this.commitTransaction(tx);
                sleepOneInterval();
                return;
            }
            // if is the instanceof ProfileEvent
            if (!(rowEvent instanceof ProfileEvent)) {
                this.commitTransaction(tx);
                LOG.error("The type of row event is not compatible with ProfileEvent");
                return;
            }
            // do send
            this.send(rowEvent, tx);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            this.rollbackTransaction(tx);
            sleepOneInterval();
        }
    }

    /**
     * Send event to Cls
     *
     * @param rowEvent Row event.
     * @param tx Transaction
     * @throws ProducerException
     * @throws InterruptedException
     */
    private void send(Event rowEvent, Transaction tx) throws ProducerException, InterruptedException {
        ProfileEvent event = (ProfileEvent) rowEvent;
        ClsIdConfig idConfig = context.getIdConfig(event.getUid());
        if (idConfig == null) {
            event.ack();
            LOG.error("There is no cls id config for uid {}, discard it", event.getUid());
            context.addSendResultMetric(event, context.getTaskName(), false, System.currentTimeMillis());
            return;
        }
        event.getHeaders().put(ClsSinkContext.KEY_TOPIC_ID, idConfig.getTopicId());
        AsyncProducerClient client = context.getClient(idConfig.getSecretId());
        List<LogItem> record = handler.parse(context, event);
        ClsCallback callback = new ClsCallback(tx, context, event);
        client.putLogs(idConfig.getTopicId(), record, callback);
    }

    /** sleepOneInterval */
    private void sleepOneInterval() {
        try {
            Thread.sleep(context.getProcessInterval());
        } catch (InterruptedException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }

    /**
     * Rollback transaction if it exists.
     * @param tx Transaction
     */
    private void rollbackTransaction(Transaction tx) {
        if (tx != null) {
            tx.rollback();
            tx.close();
        }
    }

    /**
     * Commit transaction if it exists.
     * @param tx Transaction
     */
    private void commitTransaction(Transaction tx) {
        if (tx != null) {
            tx.commit();
            tx.close();
        }
    }

}
