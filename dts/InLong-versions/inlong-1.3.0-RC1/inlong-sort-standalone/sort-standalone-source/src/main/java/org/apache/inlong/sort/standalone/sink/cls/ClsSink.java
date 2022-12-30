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

package org.apache.inlong.sort.standalone.sink.cls;

import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Cls Sink implementation.
 *
 * <p>
 *     Response for initialization of {@link ClsChannelWorker}.
 * </p>
 */
public class ClsSink extends AbstractSink implements Configurable {
    private static final Logger LOG = LoggerFactory.getLogger(ClsSink.class);

    private Context parentContext;
    private ClsSinkContext context;
    private List<ClsChannelWorker> workers = new ArrayList<>();

    /**
     * Start {@link ClsChannelWorker}.
     */
    @Override
    public void start() {
        super.start();
        try {
            this.context = new ClsSinkContext(getName(), parentContext, getChannel());
            this.context.start();
            for (int i = 0; i < context.getMaxThreads(); i++) {
                ClsChannelWorker worker = new ClsChannelWorker(getName(), context, i);
                this.workers.add(worker);
                worker.start();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Stop {@link ClsChannelWorker}.
     */
    @Override
    public void stop() {
        super.stop();
        try {
            this.context.close();
            for (ClsChannelWorker worker : this.workers) {
                worker.close();
            }
            this.workers.clear();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Process.
     * @return Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
        return Status.BACKOFF;
    }

    /**
     * Config parent context.
     * @param context Parent context.
     */
    @Override
    public void configure(Context context) {
        LOG.info("start to configure:{}, context:{}.", this.getName(), context.toString());
        this.parentContext = context;
    }
}
