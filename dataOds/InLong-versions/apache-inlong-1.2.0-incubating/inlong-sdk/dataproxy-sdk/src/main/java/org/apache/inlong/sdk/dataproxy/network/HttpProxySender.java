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

package org.apache.inlong.sdk.dataproxy.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.SendMessageCallback;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigManager;
import org.apache.inlong.sdk.dataproxy.config.HostInfo;
import org.apache.inlong.sdk.dataproxy.http.InternalHttpSender;
import org.apache.inlong.sdk.dataproxy.utils.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http sender
 */
public class HttpProxySender extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(Sender.class);

    private final ConcurrentHashSet<HostInfo> hostList = new ConcurrentHashSet<>();

    private final ProxyClientConfig proxyClientConfig;
    private ProxyConfigManager proxyConfigManager;

    private boolean bShutDown = false;

    private final InternalHttpSender internalHttpSender;
    private final LinkedBlockingQueue<HttpMessage> messageCache;

    public HttpProxySender(ProxyClientConfig configure) throws Exception {
        this.proxyClientConfig = configure;
        initTDMClientAndRequest(configure);
        this.messageCache = new LinkedBlockingQueue<>(configure.getTotalAsyncCallbackSize());
        internalHttpSender = new InternalHttpSender(configure, hostList, messageCache);
    }

    /**
     * get proxy list
     *
     * @param configure
     * @throws Exception
     */
    private void initTDMClientAndRequest(ProxyClientConfig configure) throws Exception {

        try {
            proxyConfigManager = new ProxyConfigManager(configure,
                    Utils.getLocalIp(), null);
            proxyConfigManager.setGroupId(configure.getGroupId());
            ProxyConfigEntry proxyConfigEntry = retryGettingProxyConfig();
            hostList.addAll(proxyConfigEntry.getHostMap().values());

            this.setDaemon(true);
            this.start();
        } catch (Throwable e) {
            if (configure.isReadProxyIPFromLocal()) {
                throw new Exception("Get local proxy configure failure! e = {}", e);
            } else {
                throw new Exception("Visit TDManager error! e = {}", e);
            }
        }
        logger.info("http proxy sender starts");
    }

    /**
     * retry fetching proxy config in case of network issue.
     *
     * @return proxy config entry.
     */
    private ProxyConfigEntry retryGettingProxyConfig() throws Exception {
        return proxyConfigManager.getGroupIdConfigure();
    }

    /**
     * get proxy list
     */
    @Override
    public void run() {
        while (!bShutDown) {
            try {
                int rand = ThreadLocalRandom.current().nextInt(0, 600);
                int randSleepTime = proxyClientConfig.getProxyHttpUpdateIntervalMinutes() * 60 + rand;
                TimeUnit.MILLISECONDS.sleep(randSleepTime * 1000);
                if (proxyConfigManager != null) {
                    ProxyConfigEntry proxyConfigEntry = proxyConfigManager.getGroupIdConfigure();
                    hostList.addAll(proxyConfigEntry.getHostMap().values());
                    hostList.retainAll(proxyConfigEntry.getHostMap().values());
                } else {
                    logger.error("manager is null, please check it!");
                }
                logger.info("get new proxy list " + hostList.toString());
            } catch (InterruptedException ignored) {
                // ignore it.
            } catch (Exception ex) {
                logger.error("managerFetcher get or save managerIpList occur error,", ex);
            }
        }
    }

    /**
     * send by http
     *
     * @param body
     * @param groupId
     * @param streamId
     * @param dt
     * @param timeout
     * @param timeUnit
     * @return
     */
    public SendResult sendMessage(String body, String groupId, String streamId, long dt,
                                  long timeout, TimeUnit timeUnit) {
        return sendMessage(Collections.singletonList(body), groupId, streamId, dt, timeout, timeUnit);
    }

    /**
     * send multiple messages.
     *
     * @param bodies   list of bodies
     * @param groupId
     * @param streamId
     * @param dt
     * @param timeout
     * @param timeUnit
     * @return
     */
    public SendResult sendMessage(List<String> bodies, String groupId, String streamId, long dt,
                                  long timeout, TimeUnit timeUnit) {
        if (hostList.isEmpty()) {
            logger.error("proxy list is empty, maybe client has been "
                    + "closed or groupId is not assigned with proxy list");
            return SendResult.NO_CONNECTION;
        }
        return internalHttpSender.sendMessageWithHostInfo(
                bodies, groupId, streamId, dt, timeout, timeUnit);

    }

    /**
     * async sender
     *
     * @param bodies
     * @param groupId
     * @param streamId
     * @param dt
     * @param timeout
     * @param timeUnit
     * @param callback
     */
    public void asyncSendMessage(List<String> bodies, String groupId, String streamId, long dt,
                                 long timeout, TimeUnit timeUnit, SendMessageCallback callback) {
        List<String> bodyList = new ArrayList<>(bodies);
        HttpMessage httpMessage = new HttpMessage(bodyList, groupId, streamId, dt,
                timeout, timeUnit, callback);
        try {
            if (!messageCache.offer(httpMessage)) {
                if (!proxyClientConfig.isDiscardOldMessage()) {
                    // put and wait for capacity available.
                    messageCache.put(httpMessage);
                } else {
                    // discard old message and use new message instead.
                    logger.debug("discard old message and use new message instead");
                    HttpMessage oldMessage = messageCache.poll();
                    if (oldMessage != null) {
                        oldMessage.getCallback().onMessageAck(SendResult.TIMEOUT);
                    }
                    messageCache.offer(httpMessage);
                }
            }
        } catch (Exception exception) {
            logger.error("error async sending data", exception);
        }
    }

    /**
     * async send single message.
     *
     * @param body
     * @param groupId
     * @param streamId
     * @param dt
     * @param timeout
     * @param timeUnit
     * @param callback
     */
    public void asyncSendMessage(String body, String groupId, String streamId, long dt,
                                 long timeout, TimeUnit timeUnit, SendMessageCallback callback) {
        asyncSendMessage(Collections.singletonList(body), groupId, streamId,
                dt, timeout, timeUnit, callback);
    }

    /**
     * close
     */
    public void close() {
        hostList.clear();
        bShutDown = true;
        try {
            this.interrupt();
            internalHttpSender.close();
        } catch (Exception exception) {
            logger.error("error while closing http client", exception);
        }
    }
}
