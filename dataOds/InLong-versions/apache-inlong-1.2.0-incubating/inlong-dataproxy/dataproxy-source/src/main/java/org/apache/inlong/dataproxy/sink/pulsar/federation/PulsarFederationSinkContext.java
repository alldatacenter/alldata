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

import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.config.RemoteConfigManager;
import org.apache.inlong.dataproxy.config.holder.CacheClusterConfigHolder;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.IdTopicConfigHolder;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.utils.BufferQueue;

/**
 * 
 * PulsarFederationContext
 */
public class PulsarFederationSinkContext {

    public static final String KEY_MAX_THREADS = "max-threads";
    public static final String KEY_MAXTRANSACTION = "maxTransaction";
    public static final String KEY_PROCESSINTERVAL = "processInterval";
    public static final String KEY_RELOADINTERVAL = "reloadInterval";
    public static final String KEY_MAXBUFFERQUEUESIZE = "maxBufferQueueSize";
    public static final String PREFIX_PRODUCER = "producer.";

    private final String proxyClusterId;
    private final Context sinkContext;
    private final Context producerContext;
    //
    private final IdTopicConfigHolder idTopicHolder;
    private final CacheClusterConfigHolder cacheHolder;
    private final BufferQueue<Event> bufferQueue;
    //
    private final int maxThreads;
    private final int maxTransaction;
    private final long processInterval;
    private final long reloadInterval;
    //
    private final DataProxyMetricItemSet metricItemSet;

    /**
     * Constructor
     * 
     * @param context
     */
    public PulsarFederationSinkContext(String sinkName, Context context) {
        this.proxyClusterId = CommonPropertiesHolder.getString(RemoteConfigManager.KEY_PROXY_CLUSTER_NAME);
        this.sinkContext = context;
        this.maxThreads = context.getInteger(KEY_MAX_THREADS, 10);
        this.maxTransaction = context.getInteger(KEY_MAXTRANSACTION, 1);
        this.processInterval = context.getInteger(KEY_PROCESSINTERVAL, 100);
        this.reloadInterval = context.getLong(KEY_RELOADINTERVAL, 60000L);
        //
        this.idTopicHolder = new IdTopicConfigHolder();
        this.idTopicHolder.configure(context);
        this.idTopicHolder.start();
        //
        this.cacheHolder = new CacheClusterConfigHolder();
        this.cacheHolder.configure(context);
        this.cacheHolder.start();
        //
        int maxBufferQueueSize = context.getInteger(KEY_MAXBUFFERQUEUESIZE, 128 * 1024);
        this.bufferQueue = new BufferQueue<Event>(maxBufferQueueSize);
        //
        Map<String, String> producerParams = context.getSubProperties(PREFIX_PRODUCER);
        this.producerContext = new Context(producerParams);
        //
        this.metricItemSet = new DataProxyMetricItemSet(sinkName);
        MetricRegister.register(this.metricItemSet);
    }

    /**
     * close
     */
    public void close() {
        this.idTopicHolder.close();
        this.cacheHolder.close();
    }

    /**
     * get proxyClusterId
     * 
     * @return the proxyClusterId
     */
    public String getProxyClusterId() {
        return proxyClusterId;
    }

    /**
     * get sinkContext
     * 
     * @return the sinkContext
     */
    public Context getSinkContext() {
        return sinkContext;
    }

    /**
     * get producerContext
     * 
     * @return the producerContext
     */
    public Context getProducerContext() {
        return producerContext;
    }

    /**
     * get idTopicHolder
     * 
     * @return the idTopicHolder
     */
    public IdTopicConfigHolder getIdTopicHolder() {
        return idTopicHolder;
    }

    /**
     * get cacheHolder
     * 
     * @return the cacheHolder
     */
    public CacheClusterConfigHolder getCacheHolder() {
        return cacheHolder;
    }

    /**
     * get bufferQueue
     * 
     * @return the bufferQueue
     */
    public BufferQueue<Event> getBufferQueue() {
        return bufferQueue;
    }

    /**
     * get maxThreads
     * 
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * get maxTransaction
     * 
     * @return the maxTransaction
     */
    public int getMaxTransaction() {
        return maxTransaction;
    }

    /**
     * get processInterval
     * 
     * @return the processInterval
     */
    public long getProcessInterval() {
        return processInterval;
    }

    /**
     * get reloadInterval
     * 
     * @return the reloadInterval
     */
    public long getReloadInterval() {
        return reloadInterval;
    }

    /**
     * get metricItemSet
     * 
     * @return the metricItemSet
     */
    public DataProxyMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }

}
