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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * PulsarSetSink
 */
public class PulsarFederationSink extends AbstractSink implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarFederationSink.class);

    private PulsarFederationSinkContext context;
    private List<PulsarFederationWorker> workers = new ArrayList<>();
    private Map<String, String> dimensions;

    /**
     * start
     */
    @Override
    public void start() {
        String sinkName = this.getName();
        // create worker
        for (int i = 0; i < context.getMaxThreads(); i++) {
            PulsarFederationWorker worker = new PulsarFederationWorker(sinkName, i, context);
            worker.start();
            this.workers.add(worker);
        }
        super.start();
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        for (PulsarFederationWorker worker : workers) {
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
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        LOG.info("start to configure:{}, context:{}.", this.getClass().getSimpleName(), context.toString());
        this.context = new PulsarFederationSinkContext(this.getName(), context);
        this.dimensions = new HashMap<>();
        this.dimensions.put(DataProxyMetricItem.KEY_CLUSTER_ID, this.context.getProxyClusterId());
        this.dimensions.put(DataProxyMetricItem.KEY_SINK_ID, this.getName());
    }

    /**
     * process
     * 
     * @return                        Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            Event event = channel.take();
            if (event == null) {
                tx.commit();
                return Status.BACKOFF;
            }
            //
            int eventSize = event.getBody().length;
            if (!this.context.getBufferQueue().tryAcquire(eventSize)) {
                // record the failure of queue full for monitor
                // metric
                DataProxyMetricItem metricItem = this.context.getMetricItemSet().findMetricItem(dimensions);
                metricItem.readFailCount.incrementAndGet();
                metricItem.readFailSize.addAndGet(eventSize);
                //
                tx.rollback();
                return Status.BACKOFF;
            }
            this.context.getBufferQueue().offer(event);
            tx.commit();
            return Status.READY;
        } catch (Throwable t) {
            LOG.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
                // metric
                DataProxyMetricItem metricItem = this.context.getMetricItemSet().findMetricItem(dimensions);
                metricItem.readFailCount.incrementAndGet();
            } catch (Throwable e) {
                LOG.error("Channel take transaction rollback exception:" + getName(), e);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }
}
