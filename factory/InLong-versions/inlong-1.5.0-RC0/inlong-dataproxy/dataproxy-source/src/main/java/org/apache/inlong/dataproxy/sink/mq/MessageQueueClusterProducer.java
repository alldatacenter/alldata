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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageQueueClusterProducer
 */
public class MessageQueueClusterProducer implements LifecycleAware {

    public static final Logger LOG = LoggerFactory.getLogger(MessageQueueClusterProducer.class);

    private final String workerName;
    private final CacheClusterConfig config;
    private final MessageQueueZoneSinkContext sinkContext;
    private final String cacheClusterName;
    private LifecycleState state;

    private MessageQueueHandler handler;

    /**
     * Constructor
     * 
     * @param workerName
     * @param config
     * @param context
     */
    public MessageQueueClusterProducer(String workerName, CacheClusterConfig config,
            MessageQueueZoneSinkContext context) {
        this.workerName = workerName;
        this.config = config;
        this.sinkContext = context;
        this.state = LifecycleState.IDLE;
        this.cacheClusterName = config.getClusterName();
        this.handler = this.sinkContext.createMessageQueueHandler(config);
        this.handler.init(config, context);
    }

    /**
     * start
     */
    @Override
    public void start() {
        this.state = LifecycleState.START;
        this.handler.start();
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        this.state = LifecycleState.STOP;
        this.handler.stop();
    }

    /**
     * getLifecycleState
     * 
     * @return
     */
    @Override
    public LifecycleState getLifecycleState() {
        return state;
    }

    /**
     * send
     * 
     * @param event
     */
    public boolean send(BatchPackProfile event) {
        return this.handler.send(event);
    }

    /**
     * get cacheClusterName
     * 
     * @return the cacheClusterName
     */
    public String getCacheClusterName() {
        return cacheClusterName;
    }

    /**
     * get workerName
     * @return the workerName
     */
    public String getWorkerName() {
        return workerName;
    }

    /**
     * get state
     * @return the state
     */
    public LifecycleState getState() {
        return state;
    }

    /**
     * get config
     * @return the config
     */
    public CacheClusterConfig getConfig() {
        return config;
    }

}
