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

import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * MessageQueueZoneProducer
 */
public class MessageQueueZoneProducer {

    public static final Logger LOG = LoggerFactory.getLogger(MessageQueueZoneProducer.class);
    public static final int MAX_INDEX = Integer.MAX_VALUE / 2;

    private final String workerName;
    private final MessageQueueZoneSinkContext context;
    private Timer reloadTimer;

    private List<MessageQueueClusterProducer> clusterList = new ArrayList<>();
    private List<MessageQueueClusterProducer> deletingClusterList = new ArrayList<>();

    private AtomicInteger clusterIndex = new AtomicInteger(0);

    /**
     * Constructor
     * 
     * @param workerName
     * @param context
     */
    public MessageQueueZoneProducer(String workerName, MessageQueueZoneSinkContext context) {
        this.workerName = workerName;
        this.context = context;
    }

    /**
     * start
     */
    public void start() {
        try {
            LOG.info("start MessageQueueZoneProducer:{}", workerName);
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
        for (MessageQueueClusterProducer cluster : this.clusterList) {
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
            deletingClusterList.forEach(MessageQueueClusterProducer::stop);
            deletingClusterList.clear();
            // update cluster list
            List<CacheClusterConfig> configList = this.context.getCacheHolder().getConfigList();
            List<MessageQueueClusterProducer> newClusterList = new ArrayList<>(configList.size());
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
                    MessageQueueClusterProducer cluster = new MessageQueueClusterProducer(workerName, config, context);
                    cluster.start();
                    newClusterList.add(cluster);
                }
            }
            // remove
            for (MessageQueueClusterProducer cluster : this.clusterList) {
                if (newClusterNames.contains(cluster.getCacheClusterName())) {
                    newClusterList.add(cluster);
                } else {
                    deletingClusterList.add(cluster);
                }
            }
            this.clusterList = newClusterList;
            if (!ConfigManager.getInstance().isMqClusterReady()) {
                LOG.info("set mq cluster status ready");
                ConfigManager.getInstance().updMqClusterStatus(true);
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * send
     * 
     * @param event
     */
    public boolean send(BatchPackProfile event) {
        int currentIndex = clusterIndex.getAndIncrement();
        if (currentIndex > MAX_INDEX) {
            clusterIndex.set(0);
        }
        List<MessageQueueClusterProducer> currentClusterList = this.clusterList;
        int currentSize = currentClusterList.size();
        int realIndex = currentIndex % currentSize;
        MessageQueueClusterProducer clusterProducer = currentClusterList.get(realIndex);
        return clusterProducer.send(event);
    }
}
