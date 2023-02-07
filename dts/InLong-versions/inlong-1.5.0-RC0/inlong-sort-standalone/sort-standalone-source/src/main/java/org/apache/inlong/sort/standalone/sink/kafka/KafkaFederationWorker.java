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

package org.apache.inlong.sort.standalone.sink.kafka;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Worker of */
public class KafkaFederationWorker extends Thread {

    public static final Logger LOG = InlongLoggerFactory.getLogger(KafkaFederationWorker.class);

    private final String workerName;
    private final KafkaFederationSinkContext context;

    private final KafkaProducerFederation producerFederation;
    private final Map<String, String> dimensions = new ConcurrentHashMap<>();
    private LifecycleState status;

    /**
     * Constructor of KafkaFederationWorker
     *
     * @param sinkName Name of sink.
     * @param workerIndex Index of this worker thread.
     * @param context Context of kafka sink.
     */
    public KafkaFederationWorker(
            String sinkName, int workerIndex, KafkaFederationSinkContext context) {
        super();
        this.workerName = sinkName + "-" + workerIndex;
        this.context = Preconditions.checkNotNull(context);
        this.producerFederation =
                new KafkaProducerFederation(String.valueOf(workerIndex), this.context);
        this.status = LifecycleState.IDLE;
        this.dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.context.getClusterId());
        this.dimensions.put(SortMetricItem.KEY_TASK_NAME, this.context.getTaskName());
        this.dimensions.put(SortMetricItem.KEY_SINK_ID, this.context.getSinkName());
    }

    /** Entrance of KafkaFederationWorker */
    @Override
    public void start() {
        LOG.info("start a new kafka worker {}", this.workerName);
        this.producerFederation.start();
        this.status = LifecycleState.START;
        super.start();
    }

    /** Close */
    public void close() {
        // close all producers
        LOG.info("close a kafka worker {}", this.workerName);
        this.producerFederation.close();
        this.status = LifecycleState.STOP;
    }

    @Override
    public void run() {
        LOG.info("worker {} start to run, the state is {}", this.workerName, status.name());
        while (status != LifecycleState.STOP) {
            Transaction tx = null;
            try {
                Channel channel = context.getChannel();
                tx = channel.getTransaction();
                tx.begin();
                Event rowEvent = channel.take();

                // if event is null, close tx and sleep for a while.
                if (rowEvent == null) {
                    tx.commit();
                    tx.close();
                    sleepOneInterval();
                    continue;
                }
                if (!(rowEvent instanceof ProfileEvent)) {
                    tx.commit();
                    tx.close();
                    LOG.error("The type of row event is not compatible with ProfileEvent");
                    continue;
                }

                ProfileEvent profileEvent = (ProfileEvent) rowEvent;
                String topic = this.context.getTopic(profileEvent.getUid());
                if (StringUtils.isBlank(topic)) {
                    this.context.addSendResultMetric(profileEvent, profileEvent.getUid(),
                            false, System.currentTimeMillis());
                    profileEvent.ack();
                    tx.commit();
                    tx.close();
                }
                profileEvent.getHeaders().put(Constants.TOPIC, topic);
                this.context.addSendMetric(profileEvent, topic);
                this.producerFederation.send(profileEvent, tx);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                if (tx != null) {
                    tx.rollback();
                    tx.close();
                }
                // metric
                SortMetricItem metricItem =
                        this.context.getMetricItemSet().findMetricItem(dimensions);
                metricItem.sendFailCount.incrementAndGet();
                sleepOneInterval();
            }
        }
    }

    /** sleepOneInterval */
    private void sleepOneInterval() {
        try {
            Thread.sleep(context.getProcessInterval());
        } catch (InterruptedException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }
}
