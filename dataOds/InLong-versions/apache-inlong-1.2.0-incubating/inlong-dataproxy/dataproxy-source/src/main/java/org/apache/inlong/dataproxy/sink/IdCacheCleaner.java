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

package org.apache.inlong.dataproxy.sink;

import org.apache.flume.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.apache.inlong.dataproxy.sink.TubeSink.agentIdMap;
import static org.apache.inlong.dataproxy.sink.TubeSink.idCleanerStarted;

public class IdCacheCleaner extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(IdCacheCleaner.class);
    private final long maxSurvivedTime;
    private final int maxSurvivedSize;
    private Sink sink;

    public IdCacheCleaner(Sink sink, long maxSurvivedTime, int maxSurvivedSize) {
        this.sink = sink;
        this.maxSurvivedSize = maxSurvivedSize;
        this.maxSurvivedTime = maxSurvivedTime;
    }

    @Override
    public synchronized void start() {
        if (idCleanerStarted) {
            return;
        }
        idCleanerStarted = true;
        super.start();
    }

    @Override
    public void run() {
        while (idCleanerStarted) {
            long currentTime = System.currentTimeMillis();
            long survivedTime = maxSurvivedTime;
            if (agentIdMap.size() > maxSurvivedSize) {
                survivedTime = maxSurvivedTime / 2;
            }
            logger.info("{} map size is:{};set survive time:{}", sink.getName(),
                    agentIdMap.size(), survivedTime);
            int cleanCount = 0;
            for (Map.Entry<String, Long> entry : agentIdMap.entrySet()) {
                long idTime = entry.getValue();
                if (currentTime - idTime > survivedTime) {
                    agentIdMap.remove(entry.getKey());
                    cleanCount++;
                }
            }
            logger.info("{} clear {} client ids", sink.getName(), cleanCount);
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                logger.info("cleaner thread has exit! cache size :", agentIdMap.size());
                return;
            }
        }
    }
}
