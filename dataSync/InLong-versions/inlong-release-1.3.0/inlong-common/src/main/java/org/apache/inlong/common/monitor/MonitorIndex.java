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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorIndex {

    private static final Logger logger = LoggerFactory.getLogger(MonitorIndex.class);
    private static final LogCounter logPrinter = new LogCounter(10, 100000, 60 * 1000);

    private IndexCollectThread indexCol;
    private String name;
    private ConcurrentHashMap<String, String> counterMap = new ConcurrentHashMap<String, String>();
    private int intervalSec;
    private int maxCnt;

    public MonitorIndex(String name, int intervalSec, int maxCnt) {
        /*
         * Main indicators, placed in flume_index.log
         */
        this.intervalSec = intervalSec;
        /*
         * "TDBus","TDBus_intf"...tmetric pattern name
         */
        this.name = name;
        this.maxCnt = maxCnt;
        indexCol = new IndexCollectThread();
        indexCol.setDaemon(true);
        indexCol.setName("IndexCollectThread_MonitorIndex");
        indexCol.start();
    }

    /**
     * addAndGet
     * @param key
     * @param cnt
     * @param packcnt
     * @param packsize
     * @param failcnt
     */
    public void addAndGet(String key, int cnt, int packcnt, long packsize, int failcnt) {
        try {
            if (counterMap.size() < maxCnt) {
                counterMap.compute(key, (key1, value) -> {
                    if (value != null) {
                        String[] va = value.split("#");
                        value = (Integer.parseInt(va[0]) + cnt) + "#"
                                + (Integer.parseInt(va[1]) + packcnt) + "#"
                                + (Long.parseLong(va[2]) + packsize) + "#"
                                + (Integer.parseInt(va[3]) + failcnt);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        value = stringBuilder.append(cnt).append("#").append(packcnt).append("#")
                                .append(packsize).append("#").append(failcnt).toString();
                    }
                    return value;
                });
            } else if (logPrinter.shouldPrint()) {
                logger.error(this.name + "exceed monitor's max size");
            }
        } catch (Exception e) {
            if (logPrinter.shouldPrint()) {
                logger.error("monitor exception", e);
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
            Map<String, String> counterExt = new HashMap<String, String>();
            while (!bShutDown) {
                try {
                    Thread.sleep(intervalSec * 1000L);
                    for (String str : counterMap.keySet()) {
                        counterMap.computeIfPresent(str, (s, s2) -> {
                            counterExt.put(s, s2);
                            return null;
                        });
                    }
                    for (Map.Entry<String, String> entrys : counterExt.entrySet()) {
                        logger.info("{}#{}#{}",
                                new Object[]{name, entrys.getKey(), entrys.getValue()});
                    }
                    counterExt.clear();
                } catch (Exception e) {
                    logger.warn("monitor interrupted");
                }
            }

        }
    }
}

