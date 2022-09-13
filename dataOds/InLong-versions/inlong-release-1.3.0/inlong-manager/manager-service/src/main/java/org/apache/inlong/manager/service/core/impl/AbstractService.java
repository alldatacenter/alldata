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

package org.apache.inlong.manager.service.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractService<T> implements AutoCloseable, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);


    @Value("${msg.to.db.batch.size:10}")
    private int batchSize = 10;

    @Value("${msg.to.db.queue.size:10000}")
    private int queueSize = 10000;

    @Value("${msg.to.db.core.pool.size:2}")
    private int corePoolSize = 2;

    @Value("${msg.to.db.max.pool.size:2}")
    private int maximumPoolSize = 2;

    @Value("${msg.to.db.queue.pool.size:10}")
    private int syncSendQueueSize = 10;

    private volatile boolean isClose = false;

    private LinkedBlockingQueue<T> dataQueue;

    private ThreadPoolExecutor pool;

    /**
     * batch insert entities
     *
     * @param entryList entryList
     * @return boolean true/false
     */
    abstract boolean batchInsertEntities(List<T> entryList);

    /**
     * put Data
     *
     * @param data data
     * @return boolean true/false
     */
    public boolean putData(T data) {
        if (dataQueue == null) {
            return false;
        }
        return dataQueue.offer(data);
    }

    @Override
    public void close() {
        isClose = true;
    }

    @Override
    public void afterPropertiesSet() {
        dataQueue = new LinkedBlockingQueue(queueSize);
        pool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(syncSendQueueSize),
                Executors.defaultThreadFactory());
        for (int i = 0; i < corePoolSize; i++) {
            pool.execute(new Task());
        }
    }

    class Task implements Runnable {
        @Override
        public void run() {
            while (!isClose) {
                try {
                    List<T> entryList = new ArrayList<>();
                    int count = 0;
                    while (count < batchSize) {
                        T data = dataQueue.poll(1, TimeUnit.MILLISECONDS);
                        if (data != null) {
                            entryList.add(data);
                        }
                        count++;
                    }
                    if (CollectionUtils.isNotEmpty(entryList)) {
                        batchInsertEntities(entryList);
                    }
                } catch (Exception e) {
                    LOGGER.error("batchInsertEntities has exception = {}", e);
                }
            }
        }
    }
}
