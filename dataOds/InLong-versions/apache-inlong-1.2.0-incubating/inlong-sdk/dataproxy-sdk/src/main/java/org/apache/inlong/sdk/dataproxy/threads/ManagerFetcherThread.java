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

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.utils.ServiceDiscoveryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * manager fetch thread
 */
public class ManagerFetcherThread extends Thread {
    private final Logger logger = LoggerFactory.getLogger(ManagerFetcherThread.class);
    private volatile boolean isShutdown;
    private final ProxyClientConfig proxyClientConfig;

    public ManagerFetcherThread(ProxyClientConfig proxyClientConfig) {
        isShutdown = false;
        this.proxyClientConfig = proxyClientConfig;
        this.setDaemon(true);
        this.setName("ManagerFetcherThread");
    }

    public void shutdown() {
        logger.info("Begin to shutdown ManagerFetcherThread.");
        isShutdown = true;
    }

    @Override
    public void run() {
        logger.info("ManagerFetcherThread Thread=" + Thread.currentThread().getId() + " started !");
        while (!isShutdown) {
            try {
                String managerIpList = ServiceDiscoveryUtils.getManagerIpList(proxyClientConfig);
                if (StringUtils.isBlank(managerIpList)) {
                    logger.error("ManagerFetcher get managerIpList is blank.");
                } else {
                    ServiceDiscoveryUtils.updateManagerInfo2Local(managerIpList,
                        proxyClientConfig.getManagerIpLocalPath());
                }
                TimeUnit.MILLISECONDS.sleep((long) proxyClientConfig.getProxyUpdateIntervalMinutes() * 60 * 1000);
            } catch (Throwable e) {
                logger.error("ManagerFetcher get or save managerIpList occur error,", e);
            }
        }
    }
}