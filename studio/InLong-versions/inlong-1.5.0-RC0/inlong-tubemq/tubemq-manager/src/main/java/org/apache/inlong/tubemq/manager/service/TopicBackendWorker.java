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

package org.apache.inlong.tubemq.manager.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.repository.TopicRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Topic backend thread worker.
 */
@Component
@Slf4j
public class TopicBackendWorker implements DisposableBean, Runnable {

    // old code, stop first
    private final AtomicBoolean runFlag = new AtomicBoolean(false);
    private final ConcurrentHashMap<Integer, BlockingQueue<TopicFuture>> pendingTopics =
            new ConcurrentHashMap<>();
    private final AtomicInteger notSatisfiedCount = new AtomicInteger(0);
    private final NodeService nodeService;
    private final ScheduledExecutorService workerExecutor;

    @Autowired
    private TopicRepository topicRepository;

    @Value("${manager.topic.queue.warning.size:100}")
    private int queueWarningSize;

    // value in seconds
    @Value("${manager.topic.queue.thread.interval:10}")
    private int queueThreadInterval;

    @Value("${manager.topic.queue.max.wait:3}")
    private int queueMaxWait;

    @Value("${manager.topic.queue.max.running.size:20}")
    private int queueMaxRunningSize;

    TopicBackendWorker() {
        ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
        this.workerExecutor = Executors
                .newSingleThreadScheduledExecutor(
                        factoryBuilder.setNameFormat("tubemq-manager-topic-backend-worker").build());
        workerExecutor.schedule(this, queueThreadInterval, TimeUnit.SECONDS);
        nodeService = new NodeServiceImpl(this);
    }

    /**
     * add topic future to pending executing queue.
     * @param future - TopicFuture.
     */
    public void addTopicFuture(TopicFuture future) {
        BlockingQueue<TopicFuture> tmpQueue = new LinkedBlockingQueue<>();
        BlockingQueue<TopicFuture> queue = pendingTopics.putIfAbsent(
                future.getEntry().getClusterId(), tmpQueue);
        if (queue == null) {
            queue = tmpQueue;
        }
        queue.add(future);
        if (queue.size() > queueWarningSize) {
            log.warn("queue size exceed {}, please check it", queueWarningSize);
        }
    }

    /**
     * batch executing adding topic, wait util max n seconds or max size satisfied.
     */
    private void batchAddTopic() {
        pendingTopics.forEach((clusterId, queue) -> {
            Map<String, TopicFuture> pendingTopicList = new HashMap<>(32);
            if (notSatisfiedCount.get() > queueMaxWait || queue.size() > queueMaxRunningSize) {
                notSatisfiedCount.set(0);
                List<TopicFuture> tmpTopicList = new ArrayList<>();
                queue.drainTo(tmpTopicList, queueMaxRunningSize);
                for (TopicFuture topicFuture : tmpTopicList) {
                    pendingTopicList.put(topicFuture.getEntry().getTopic(), topicFuture);
                }
            } else {
                notSatisfiedCount.incrementAndGet();
            }
            // update broker status
            nodeService.updateBrokerStatus(clusterId, pendingTopicList);
        });

    }

    /**
     * check topic from db
     */
    private void checkTopicFromDB() {
    }

    @Override
    public void run() {
        log.info("TopicBackendWorker has started");
        if (runFlag.get()) {
            try {
                batchAddTopic();
                checkTopicFromDB();
            } catch (Exception exception) {
                log.warn("exception caught", exception);
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        runFlag.set(false);
        nodeService.close();
        this.workerExecutor.shutdown();
    }
}
