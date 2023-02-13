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

import org.apache.flume.Transaction;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.pojo.CacheClusterConfig;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KafkaProducerFederation.
 */
public class KafkaProducerFederation implements Runnable {

    private static final Logger LOG = InlongLoggerFactory.getLogger(KafkaProducerFederation.class);
    private static final int CORE_POOL_SIZE = 1;

    private final String workerName;
    private final KafkaFederationSinkContext context;
    private ScheduledExecutorService pool;
    private long reloadInterval;

    private List<KafkaProducerCluster> clusterList = new ArrayList<>();
    private List<KafkaProducerCluster> deletingClusterList = new ArrayList<>();

    private AtomicInteger clusterIndex = new AtomicInteger(0);

    /**
     * constructor of KafkaProducerFederation
     *
     * @param workerName workerName
     * @param context    context
     */
    public KafkaProducerFederation(String workerName, KafkaFederationSinkContext context) {
        this.workerName = Preconditions.checkNotNull(workerName);
        this.context = Preconditions.checkNotNull(context);
        this.reloadInterval = context.getReloadInterval();
    }

    /** close */
    public void close() {
        try {
            this.pool.shutdownNow();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        for (KafkaProducerCluster cluster : this.clusterList) {
            cluster.stop();
        }
    }

    /** start */
    public void start() {
        try {
            this.reload();
            this.initReloadExecutor();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /** Implements {@link Runnable} method. */
    @Override
    public void run() {
        this.reload();
    }

    /** reload module */
    private void reload() {
        try {
            LOG.info("stop deleting clusters, size is {}", deletingClusterList.size());
            deletingClusterList.forEach(KafkaProducerCluster::stop);
            deletingClusterList.clear();

            LOG.info("update cluster list");
            List<CacheClusterConfig> newClusterConfigList = this.context.getClusterConfigList();
            // prepare
            Set<String> newClusterNames = new HashSet<>();
            Set<String> oldClusterNames = new HashSet<>();
            newClusterConfigList.forEach(
                    clusterConfig -> newClusterNames.add(clusterConfig.getClusterName()));
            clusterList.forEach(cluster -> oldClusterNames.add(cluster.getCacheClusterName()));
            List<KafkaProducerCluster> newClusterList = new ArrayList<>(newClusterConfigList.size());

            // add new cluster
            newClusterConfigList.forEach(
                    config -> {
                        if (!oldClusterNames.contains(config.getClusterName())) {
                            KafkaProducerCluster cluster = new KafkaProducerCluster(workerName, config, context);
                            cluster.start();
                            newClusterList.add(cluster);
                        }
                    });

            // remove expire cluster
            clusterList.forEach(
                    cluster -> {
                        if (!newClusterNames.contains(cluster.getCacheClusterName())) {
                            deletingClusterList.add(cluster);
                        } else {
                            newClusterList.add(cluster);
                        }
                    });
            LOG.info("the modified cluster list size is {}", newClusterList.size());
            this.clusterList = newClusterList;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * send event
     *
     * @param  profileEvent event to send
     * @param  tx           transaction
     * @return              send result
     * @throws IOException
     */
    public boolean send(ProfileEvent profileEvent, Transaction tx) throws IOException {
        int currentIndex = clusterIndex.getAndIncrement();
        if (currentIndex > Integer.MAX_VALUE / 2) {
            clusterIndex.set(0);
        }
        List<KafkaProducerCluster> currentClusterList = this.clusterList;
        int currentSize = currentClusterList.size();
        int realIndex = currentIndex % currentSize;
        KafkaProducerCluster clusterProducer = currentClusterList.get(realIndex);
        return clusterProducer.send(profileEvent, tx);
    }

    /** Init ScheduledExecutorService with fix reload rate {@link #reloadInterval}. */
    private void initReloadExecutor() {
        this.pool = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
        pool.scheduleAtFixedRate(this, reloadInterval, reloadInterval, TimeUnit.SECONDS);
    }
}
