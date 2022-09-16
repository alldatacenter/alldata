/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * index collector, print the num of receive messages of one DefaultMessageSender in every one minute
 */
public class IndexCollectThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(IndexCollectThread.class);
    private final Map<String, Long> storeIndex;
    private volatile boolean bShutDown;

    public IndexCollectThread(Map<String, Long> storeIndex) {
        bShutDown = false;
        this.storeIndex = storeIndex;
        this.setDaemon(true);
        this.setName("IndexCollectThread");
    }

    public void shutDown() {
        logger.info("begin to shut down IndexCollectThread!");
        bShutDown = true;
    }

    @Override
    public void run() {
        logger.info("IndexCollectThread Thread=" + Thread.currentThread().getId() + " started !");
        while (!bShutDown) {
            try {
                TimeUnit.MILLISECONDS.sleep(60 * 1000);
                DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                for (Map.Entry<String, Long> entry : storeIndex.entrySet()) {
                    String key = entry.getKey();
                    Long val = entry.getValue();
                    key = "|" + sdf.format(System.currentTimeMillis()) + "|" + key;
                    logger.info("Monitor {} send message {}", key, val);
                    entry.setValue(0L);
                }
            } catch (Exception e) {
                if (!bShutDown) {
                    logger.error("IndexCollectThread exception", e);
                }
            }
        }
    }
}
