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

package org.apache.inlong.sort.standalone.sink.pulsar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.slf4j.Logger;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;

/**
 * 
 * PulsarFederationSink
 */
public class PulsarFederationSink extends AbstractSink implements Configurable {

    public static final Logger LOG = InlongLoggerFactory.getLogger(PulsarFederationSink.class);

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
        this.context = new PulsarFederationSinkContext(getName(), context, getChannel());
        this.context.start();
        this.dimensions = new HashMap<>();
        this.dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.context.getClusterId());
        this.dimensions.put(SortMetricItem.KEY_TASK_NAME, this.context.getTaskName());
        this.dimensions.put(SortMetricItem.KEY_SINK_ID, this.context.getSinkName());
    }

    /**
     * process
     * 
     * @return                        Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
        return Status.BACKOFF;
    }
}
