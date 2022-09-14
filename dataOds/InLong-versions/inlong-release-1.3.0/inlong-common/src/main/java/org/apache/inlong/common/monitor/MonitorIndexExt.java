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

package org.apache.inlong.common.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorIndexExt {

    private static final Logger logger = LoggerFactory.getLogger(MonitorIndexExt.class);
    private static final LogCounter logPrinter = new LogCounter(10, 100000, 60 * 1000);

    private IndexCollectThread indexCol;
    private String name;
    private ConcurrentHashMap<String, AtomicLong> counterMap =
            new ConcurrentHashMap<String, AtomicLong>();
    private int intervalSec;
    private int maxCnt;

    public MonitorIndexExt(String name, int intervalSec, int maxCnt) {
        /*
         * key
         * Almost unchanging. Component indicators, flume_monitors.log
         */
        this.intervalSec = intervalSec;
        /*
         * TDBus_monitors", used to monitor...tmetric pattern name
         */
        this.name = name;
        this.maxCnt = maxCnt;
        indexCol = new IndexCollectThread();
        indexCol.setDaemon(true);
        indexCol.setName("IndexCollectThread_MonitorIndexExt");
        indexCol.start();
    }

    /**
     * incrementAndGet
     * @param key
     */
    public void incrementAndGet(String key) {
        try {
            if (counterMap.size() < maxCnt) {
                counterMap.compute(key, (s, atomicLong) -> {
                    if (atomicLong != null) {
                        atomicLong.incrementAndGet();
                    } else {
                        atomicLong = new AtomicLong(1);
                    }
                    return atomicLong;
                });
            } else if (logPrinter.shouldPrint()) {
                logger.error(this.name + "exceed monitorExt's max size");
            }
        } catch (Exception e) {
            if (logPrinter.shouldPrint()) {
                logger.error("monitorExt exception", e);
            }
        }
    }

    public void shutDown() {
        indexCol.shutDown();
    }

    public int getMaxCnt() {
        return maxCnt;
    }

    public void setMaxCnt(int maxCnt) {
        this.maxCnt = maxCnt;
    }

    private class IndexCollectThread
            extends Thread {

        private boolean bShutDown = false;

        public IndexCollectThread() {
            bShutDown = false;
        }

        public void shutDown() {
            bShutDown = true;
        }

        @Override
        public void run() {
            Map<String, Long> counterExt = new HashMap<String, Long>();
            while (!bShutDown) {
                try {
                    Thread.sleep(intervalSec * 1000L);
                    for (String str : counterMap.keySet()) {
                        counterMap.computeIfPresent(str, (s, atomicLong) -> {
                            long cnt = atomicLong.get();
                            counterExt.put(str, cnt);
                            atomicLong.set(0L);
                            return atomicLong;
                        });
                    }

                    for (Map.Entry<String, Long> entrys : counterExt.entrySet()) {
                        logger.info("{}#{}#{}",
                                new Object[]{name, entrys.getKey(), entrys.getValue()});
                    }
                    counterExt.clear();

                } catch (Exception e) {
                    logger.warn("moniorExt interrupted");
                }
            }
        }
    }
}

