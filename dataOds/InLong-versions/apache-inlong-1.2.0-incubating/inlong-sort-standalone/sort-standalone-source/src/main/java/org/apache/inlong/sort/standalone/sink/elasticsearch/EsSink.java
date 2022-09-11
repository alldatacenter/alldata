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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EsSink
 */
public class EsSink extends AbstractSink implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(EsSink.class);

    private Context parentContext;
    private final LinkedBlockingQueue<EsIndexRequest> dispatchQueue = new LinkedBlockingQueue<>();
    private EsSinkContext context;
    // workers
    private List<EsChannelWorker> workers = new ArrayList<>();
    // output
    private EsOutputChannel outputChannel;

    /**
     * start
     */
    @Override
    public void start() {
        super.start();
        try {
            this.context = new EsSinkContext(getName(), parentContext, getChannel(), dispatchQueue);
            this.context.start();
            for (int i = 0; i < context.getMaxThreads(); i++) {
                EsChannelWorker worker = new EsChannelWorker(context, i);
                this.workers.add(worker);
                worker.start();
            }
            this.outputChannel = EsSinkFactory.createEsOutputChannel(context);
            this.outputChannel.init();
            this.outputChannel.start();
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
            this.context.close();
            for (EsChannelWorker worker : this.workers) {
                worker.close();
            }
            this.workers.clear();
            this.outputChannel.close();
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
        return Status.BACKOFF;
    }

}
