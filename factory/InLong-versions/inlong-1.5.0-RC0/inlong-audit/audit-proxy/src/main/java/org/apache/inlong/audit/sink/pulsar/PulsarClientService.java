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

package org.apache.inlong.audit.sink.pulsar;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.inlong.audit.consts.AttributeConstants;
import org.apache.inlong.audit.sink.EventStat;
import org.apache.inlong.audit.utils.LogCounter;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PulsarClientService {

    private static final Logger logger = LoggerFactory.getLogger(PulsarClientService.class);

    private static final LogCounter logPrinterA = new LogCounter(10, 100000, 60 * 1000);

    /*
     * properties key for pulsar client
     */
    private static String PULSAR_SERVER_URL = "pulsar_server_url";
    private static String PULSAR_ENABLE_AUTH = "enable_token_auth";
    private static String PULSAR_ENABLE_AUTH_TOKEN = "auth_token";
    /*
     * properties key pulsar producer
     */
    private static String SEND_TIMEOUT = "send_timeout_ms";
    private static String CLIENT_TIMEOUT = "client_op_timeout_second";
    private static String ENABLE_BATCH = "enable_batch";
    private static String BLOCK_IF_QUEUE_FULL = "block_if_queue_full";
    private static String MAX_PENDING_MESSAGES = "max_pending_messages";
    private static String MAX_BATCHING_MESSAGES = "max_batching_messages";

    private static int DEFAULT_SEND_TIMEOUT_MILL = 30 * 1000;
    private static int DEFAULT_CLIENT_TIMEOUT_SECOND = 30;
    private static boolean DEFAULT_ENABLE_BATCH = true;
    private static boolean DEFAULT_BLOCK_IF_QUEUE_FULL = true;
    private static int DEFAULT_MAX_PENDING_MESSAGES = 10000;
    private static int DEFAULT_MAX_BATCHING_MESSAGES = 1000;

    private static boolean DEFAULT_PULSAR_ENABLE_TOKEN_AUTH = false;
    private static String DEFAULT_PULSAR_TOKEN_AUTH = "";
    /*
     * for producer
     */
    private Integer sendTimeout; // in millsec
    private Integer clientOpTimeout;
    private boolean enableBatch = true;
    private boolean blockIfQueueFull = true;
    private int maxPendingMessages = 10000;
    private int maxBatchingMessages = 1000;
    public ConcurrentHashMap<String, Producer> producerInfoMap;
    public PulsarClient pulsarClient;
    public String pulsarServerUrl;
    public boolean pulsarEnableTokenAuth;
    public String pulsarTokenAuth;

    private String localIp = "127.0.0.1";

    /**
     * pulsar client service
     *
     * @param context
     */
    public PulsarClientService(Context context) {

        pulsarServerUrl = context.getString(PULSAR_SERVER_URL);
        Preconditions.checkState(pulsarServerUrl != null, "No pulsar server url specified");

        sendTimeout = context.getInteger(SEND_TIMEOUT, DEFAULT_SEND_TIMEOUT_MILL);
        clientOpTimeout = context.getInteger(CLIENT_TIMEOUT, DEFAULT_CLIENT_TIMEOUT_SECOND);
        logger.debug("PulsarClientService " + SEND_TIMEOUT + " " + sendTimeout);
        Preconditions.checkArgument(sendTimeout > 0, "sendTimeout must be > 0");

        enableBatch = context.getBoolean(ENABLE_BATCH, DEFAULT_ENABLE_BATCH);
        blockIfQueueFull = context.getBoolean(BLOCK_IF_QUEUE_FULL, DEFAULT_BLOCK_IF_QUEUE_FULL);
        maxPendingMessages = context.getInteger(MAX_PENDING_MESSAGES, DEFAULT_MAX_PENDING_MESSAGES);
        maxBatchingMessages = context.getInteger(MAX_BATCHING_MESSAGES, DEFAULT_MAX_BATCHING_MESSAGES);
        producerInfoMap = new ConcurrentHashMap<>();
        localIp = NetworkUtils.getLocalIp();

        pulsarEnableTokenAuth = context.getBoolean(PULSAR_ENABLE_AUTH, DEFAULT_PULSAR_ENABLE_TOKEN_AUTH);
        pulsarTokenAuth = context.getString(PULSAR_ENABLE_AUTH_TOKEN, DEFAULT_PULSAR_TOKEN_AUTH);
    }

    /**
     * init connection
     *
     * @param callBack
     */
    public void initCreateConnection(CreatePulsarClientCallBack callBack) {
        try {
            createConnection(callBack);
        } catch (FlumeException e) {
            logger.error("Unable to create pulsar client" + ". Exception follows.", e);
            close();
        }
    }

    /**
     * send message
     *
     * @param topic
     * @param event
     * @param sendMessageCallBack
     * @param es
     * @return
     */
    public boolean sendMessage(String topic, Event event,
            SendMessageCallBack sendMessageCallBack, EventStat es) {
        Producer producer = null;
        try {
            producer = getProducer(topic);
        } catch (Exception e) {
            if (logPrinterA.shouldPrint()) {
                logger.error("Get producer failed!", e);
            }
        }

        if (producer == null) {
            logger.error("Get producer is null!");
            return false;
        }

        Map<String, String> proMap = new HashMap<>();
        proMap.put("auditIp", localIp);
        String streamId = "";
        String groupId = "";
        if (event.getHeaders().containsKey(AttributeConstants.INLONG_STREAM_ID)) {
            streamId = event.getHeaders().get(AttributeConstants.INLONG_STREAM_ID);
            proMap.put(AttributeConstants.INLONG_STREAM_ID, streamId);
        }
        if (event.getHeaders().containsKey(AttributeConstants.INLONG_GROUP_ID)) {
            groupId = event.getHeaders().get(AttributeConstants.INLONG_GROUP_ID);
            proMap.put(AttributeConstants.INLONG_GROUP_ID, groupId);
        }

        logger.debug("producer send msg!");
        producer.newMessage().properties(proMap).value(event.getBody())
                .sendAsync().thenAccept((msgId) -> {
                    sendMessageCallBack.handleMessageSendSuccess((MessageIdImpl) msgId, es);

                }).exceptionally((e) -> {
                    sendMessageCallBack.handleMessageSendException(es, e);
                    return null;
                });
        return true;
    }

    /**
     * If this function is called successively without calling {@see #destroyConnection()}, only the
     * first call has any effect.
     *
     * @throws FlumeException if an RPC client connection could not be opened
     */
    private void createConnection(CreatePulsarClientCallBack callBack) throws FlumeException {
        if (pulsarClient != null) {
            return;
        }
        try {
            pulsarClient = initPulsarClient(pulsarServerUrl);
            callBack.handleCreateClientSuccess(pulsarServerUrl);
        } catch (PulsarClientException e) {
            callBack.handleCreateClientException(pulsarServerUrl);
            logger.error("create connnection error in metasink, "
                    + "maybe pulsar master set error, please re-check.url{}, ex1 {}",
                    pulsarServerUrl,
                    e.getMessage());
        } catch (Throwable e) {
            callBack.handleCreateClientException(pulsarServerUrl);
            logger.error("create connnection error in metasink, "
                    + "maybe pulsar master set error/shutdown in progress, please "
                    + "re-check. url{}, ex2 {}",
                    pulsarServerUrl,
                    e.getMessage());
        }
    }

    private PulsarClient initPulsarClient(String pulsarUrl) throws Exception {
        PulsarClient pulsarClient = null;
        ClientBuilder builder = PulsarClient.builder();
        if (pulsarEnableTokenAuth && StringUtils.isNotEmpty(pulsarTokenAuth)) {
            builder.authentication(AuthenticationFactory.token(pulsarTokenAuth));
        }
        pulsarClient = builder.serviceUrl(pulsarUrl)
                .connectionTimeout(clientOpTimeout, TimeUnit.SECONDS).build();

        return pulsarClient;
    }

    public Producer initTopicProducer(String topic) {
        logger.info("initTopicProducer topic = {}", topic);
        Producer producer = null;
        try {
            producer = pulsarClient.newProducer().sendTimeout(sendTimeout,
                    TimeUnit.MILLISECONDS)
                    .topic(topic)
                    .enableBatching(enableBatch)
                    .blockIfQueueFull(blockIfQueueFull)
                    .maxPendingMessages(maxPendingMessages)
                    .batchingMaxMessages(maxBatchingMessages)
                    .create();
        } catch (PulsarClientException e) {
            logger.error("create pulsar client has error e = {}", e);
        }
        return producer;
    }

    private Producer getProducer(String topic) {
        return producerInfoMap.computeIfAbsent(topic, (k) -> initTopicProducer(topic));
    }

    public void closeTopicProducer(String topic) {
        logger.info("closeTopicProducer topic = {}", topic);
        Producer producer = producerInfoMap.remove(topic);
        if (producer != null) {
            producer.closeAsync();
        }
    }

    private void destroyConnection() {
        producerInfoMap.clear();
        if (pulsarClient != null) {
            try {
                pulsarClient.shutdown();
            } catch (PulsarClientException e) {
                logger.error("destroy pulsarClient error in PulsarSink, PulsarClientException {}",
                        e.getMessage());
            } catch (Exception e) {
                logger.error("destroy pulsarClient error in PulsarSink, ex {}", e.getMessage());
            }
        }
        pulsarClient = null;
        logger.debug("closed meta producer");
    }

    public void close() {
        destroyConnection();
    }
}
