/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.sink.pulsar.federation;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.flume.Event;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.dataproxy.utils.Constants;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * PulsarSetWorker
 */
public class PulsarFederationWorker extends Thread {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarFederationWorker.class);

    private final String workerName;
    private final PulsarFederationSinkContext context;

    private PulsarProducerFederation producerFederation;
    private LifecycleState status;
    private Map<String, String> dimensions;

    /**
     * Constructor
     * 
     * @param sinkName
     * @param workerIndex
     * @param context
     */
    public PulsarFederationWorker(String sinkName, int workerIndex, PulsarFederationSinkContext context) {
        super();
        this.workerName = sinkName + "-worker-" + workerIndex;
        this.context = context;
        this.producerFederation = new PulsarProducerFederation(workerName, this.context);
        this.status = LifecycleState.IDLE;
        this.dimensions = new HashMap<>();
        this.dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, this.context.getProxyClusterId());
        this.dimensions.put(DataProxyMetricItem.KEY_SINK_ID, sinkName);
    }

    /**
     * start
     */
    @Override
    public void start() {
        this.producerFederation.start();
        this.status = LifecycleState.START;
        super.start();
    }

    /**
     * 
     * close
     */
    public void close() {
        // close all producers
        this.producerFederation.close();
        this.status = LifecycleState.STOP;
    }

    /**
     * run
     */
    @Override
    public void run() {
        LOG.info(String.format("start PulsarSetWorker:%s", this.workerName));
        while (status != LifecycleState.STOP) {
            try {
                Event currentRecord = context.getBufferQueue().pollRecord();
                if (currentRecord == null) {
                    Thread.sleep(context.getProcessInterval());
                    continue;
                }
                // fill topic
                this.fillTopic(currentRecord);
                // metric
                DataProxyMetricItem.fillInlongId(currentRecord, dimensions);
                this.dimensions.put(DataProxyMetricItem.KEY_SINK_DATA_ID,
                        currentRecord.getHeaders().get(Constants.TOPIC));
                long msgTime = NumberUtils.toLong(currentRecord.getHeaders().get(Constants.HEADER_KEY_MSG_TIME),
                        System.currentTimeMillis());
                long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
                dimensions.put(DataProxyMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
                DataProxyMetricItem metricItem = this.context.getMetricItemSet().findMetricItem(dimensions);
                metricItem.sendCount.incrementAndGet();
                metricItem.sendSize.addAndGet(currentRecord.getBody().length);
                // send
                this.producerFederation.send(currentRecord);
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
                this.sleepOneInterval();
            }
        }
    }

    /**
     * fillTopic
     * 
     * @param currentRecord
     */
    private void fillTopic(Event currentRecord) {
        Map<String, String> headers = currentRecord.getHeaders();
        String inlongGroupId = headers.get(Constants.INLONG_GROUP_ID);
        String inlongStreamId = headers.get(Constants.INLONG_STREAM_ID);
        String uid = IdTopicConfig.generateUid(inlongGroupId, inlongStreamId);
        String topic = this.context.getIdTopicHolder().getTopic(uid);
        if (!StringUtils.isBlank(topic)) {
            headers.put(Constants.TOPIC, topic);
        }
    }

    /**
     * sleepOneInterval
     */
    private void sleepOneInterval() {
        try {
            Thread.sleep(context.getProcessInterval());
        } catch (InterruptedException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }
}
