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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.flume.Event;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * PulsarProducerSet
 */
public class PulsarProducerFederation {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarProducerFederation.class);

    private final String workerName;
    private final PulsarFederationSinkContext context;
    private Timer reloadTimer;

    private List<PulsarProducerCluster> clusterList = new ArrayList<>();
    private List<PulsarProducerCluster> deletingClusterList = new ArrayList<>();

    private AtomicInteger clusterIndex = new AtomicInteger(0);

    /**
     * Constructor
     * 
     * @param workerName
     * @param context
     */
    public PulsarProducerFederation(String workerName, PulsarFederationSinkContext context) {
        this.workerName = workerName;
        this.context = context;
    }

    /**
     * start
     */
    public void start() {
        try {
            this.reload();
            this.setReloadTimer();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * close
     */
    public void close() {
        try {
            this.reloadTimer.cancel();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        for (PulsarProducerCluster cluster : this.clusterList) {
            cluster.stop();
        }
    }

    /**
     * setReloadTimer
     */
    private void setReloadTimer() {
        reloadTimer = new Timer(true);
        TimerTask task = new TimerTask() {

            public void run() {
                reload();
            }
        };
        reloadTimer.schedule(task, new Date(System.currentTimeMillis() + context.getReloadInterval()),
                context.getReloadInterval());
    }

    /**
     * reload
     */
    public void reload() {
        try {
            // stop deleted cluster
            deletingClusterList.forEach(item -> {
                item.stop();
            });
            deletingClusterList.clear();
            // update cluster list
            List<CacheClusterConfig> configList = this.context.getCacheHolder().getConfigList();
            List<PulsarProducerCluster> newClusterList = new ArrayList<>(configList.size());
            // prepare
            Set<String> newClusterNames = new HashSet<>();
            configList.forEach(item -> {
                newClusterNames.add(item.getClusterName());
            });
            Set<String> oldClusterNames = new HashSet<>();
            clusterList.forEach(item -> {
                oldClusterNames.add(item.getCacheClusterName());
            });
            // add
            for (CacheClusterConfig config : configList) {
                if (!oldClusterNames.contains(config.getClusterName())) {
                    PulsarProducerCluster cluster = new PulsarProducerCluster(workerName, config, context);
                    cluster.start();
                    newClusterList.add(cluster);
                }
            }
            // remove
            for (PulsarProducerCluster cluster : this.clusterList) {
                if (newClusterNames.contains(cluster.getCacheClusterName())) {
                    newClusterList.add(cluster);
                } else {
                    deletingClusterList.add(cluster);
                }
            }
            this.clusterList = newClusterList;
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * send
     * 
     * @param event
     */
    public boolean send(Event event) {
        int currentIndex = clusterIndex.getAndIncrement();
        if (currentIndex > Integer.MAX_VALUE / 2) {
            clusterIndex.set(0);
        }
        List<PulsarProducerCluster> currentClusterList = this.clusterList;
        int currentSize = currentClusterList.size();
        int realIndex = currentIndex % currentSize;
        PulsarProducerCluster clusterProducer = currentClusterList.get(realIndex);
        return clusterProducer.send(event);
    }
}
