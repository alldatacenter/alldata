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

import org.apache.flume.Context;
import org.apache.flume.Sink;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class KafkaFederationSink extends AbstractSink implements Configurable {

    private static final Logger LOG = InlongLoggerFactory.getLogger(KafkaFederationSink.class);
    private Context parentContext;
    private KafkaFederationSinkContext context;
    private List<KafkaFederationWorker> workers = new ArrayList<>();

    /** init and start workers */
    @Override
    public void start() {
        String sinkName = this.getName();
        this.context = new KafkaFederationSinkContext(sinkName, parentContext, getChannel());
        this.context.start();
        // create worker
        for (int i = 0; i < context.getMaxThreads(); i++) {
            KafkaFederationWorker worker = new KafkaFederationWorker(sinkName, i, context);
            LOG.info("new kafka worker, the context is {}", context.toString());
            worker.start();
            this.workers.add(worker);
        }
        super.start();
    }

    /** stop */
    @Override
    public void stop() {
        LOG.info("stop kafka sink");
        for (KafkaFederationWorker worker : workers) {
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
     * @param context context
     */
    @Override
    public void configure(Context context) {
        LOG.info(
                "start to configure:{}, context:{}.",
                this.getClass().getSimpleName(),
                context.toString());
        this.parentContext = context;
    }

    /**
     * process
     * @return Status
     */
    @Override
    public Sink.Status process() {
        return Sink.Status.BACKOFF;
    }
}
