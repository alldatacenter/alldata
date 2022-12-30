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

package org.apache.inlong.dataproxy.sink.pulsar;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.inlong.dataproxy.base.OrderEvent;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.dataproxy.sink.EventStat;
import org.apache.inlong.dataproxy.source.MsgType;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.apache.inlong.dataproxy.utils.NetworkUtils;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.PulsarClientException.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PulsarClientService {

    private static final Logger logger = LoggerFactory.getLogger(PulsarClientService.class);
    public Map<String, List<TopicProducerInfo>> producerInfoMap;
    public Map<String, AtomicLong> topicSendIndexMap;
    public Map<String, PulsarClient> pulsarClients = new ConcurrentHashMap<>();
    public int pulsarClientIoThreads;
    public int pulsarConnectionsPreBroker;
    /*
     * for pulsar client
     */
    private Map<String, String> pulsarUrl2token;
    private String authType;
    /*
     * for producer
     */
    /*
     * unit mills
     */
    private Integer sendTimeout;
    private Integer clientTimeout;
    private boolean enableBatch = true;
    private boolean blockIfQueueFull = true;
    private int maxPendingMessages = 10000;
    private int maxPendingMessagesAcrossPartitions = 500000;
    private CompressionType compressionType;
    private int maxBatchingBytes = 128 * 1024;
    private int maxBatchingMessages = 1000;
    private long maxBatchingPublishDelayMillis = 1;
    private long retryIntervalWhenSendMsgError = 30 * 1000L;
    private String localIp = "127.0.0.1";

    private int sinkThreadPoolSize;

    /**
     * PulsarClientService
     *
     * @param pulsarConfig
     */
    public PulsarClientService(MQClusterConfig pulsarConfig, int sinkThreadPoolSize) {

        this.sinkThreadPoolSize = sinkThreadPoolSize;

        authType = pulsarConfig.getAuthType();
        sendTimeout = pulsarConfig.getSendTimeoutMs();
        retryIntervalWhenSendMsgError = pulsarConfig.getRetryIntervalWhenSendErrorMs();
        clientTimeout = pulsarConfig.getClientTimeoutSecond();

        Preconditions.checkArgument(sendTimeout > 0, "sendTimeout must be > 0");

        pulsarClientIoThreads = pulsarConfig.getPulsarClientIoThreads();
        pulsarConnectionsPreBroker = pulsarConfig.getPulsarConnectionsPreBroker();

        enableBatch = pulsarConfig.getEnableBatch();
        blockIfQueueFull = pulsarConfig.getBlockIfQueueFull();
        maxPendingMessages = pulsarConfig.getMaxPendingMessages();
        maxPendingMessagesAcrossPartitions = pulsarConfig.getMaxPendingMessagesAcrossPartitions();
        String compressionTypeStr = pulsarConfig.getCompressionType();
        if (StringUtils.isNotEmpty(compressionTypeStr)) {
            compressionType = CompressionType.valueOf(compressionTypeStr);
        } else {
            compressionType = CompressionType.NONE;
        }
        maxBatchingMessages = pulsarConfig.getMaxBatchingMessages();
        maxBatchingBytes = pulsarConfig.getMaxBatchingBytes();
        maxBatchingPublishDelayMillis = pulsarConfig.getMaxBatchingPublishDelayMillis();
        producerInfoMap = new ConcurrentHashMap<>();
        topicSendIndexMap = new ConcurrentHashMap<>();
        localIp = NetworkUtils.getLocalIp();
    }

    public void initCreateConnection(CreatePulsarClientCallBack callBack) {
        pulsarUrl2token = ConfigManager.getInstance().getMqClusterUrl2Token();
        if (pulsarUrl2token == null || pulsarUrl2token.isEmpty()) {
            logger.warn("failed to get Pulsar Cluster, make sure register pulsar to manager successfully.");
            return;
        }
        try {
            createConnection(callBack);
        } catch (FlumeException e) {
            logger.error("unable to create pulsar client: ", e);
            close();
        }
    }

    /**
     * send message
     */
    public boolean sendMessage(int poolIndex, String topic, Event event,
            SendMessageCallBack sendMessageCallBack, EventStat es) {
        TopicProducerInfo producerInfo = null;
        boolean result;
        final String inlongStreamId = getInlongStreamId(event);
        final String inlongGroupId = getInlongGroupId(event);
        try {
            producerInfo = getProducerInfo(poolIndex, topic, inlongGroupId, inlongStreamId);
        } catch (Exception e) {
            logger.error("get producer failed for topic=" + topic, e);
        }
        /*
         * If the producer is a null value,\ it means that the topic is not yet
         * ready, and it needs to be played back into the file channel
         */
        if (producerInfo == null) {
            /*
             * Data within 30s is placed in the exception channel to
             * prevent frequent checks
             * After 30s, reopen the topic check, if it is still a null value,
             *  put it back into the illegal map
             */
            checkAndResponse(event, inlongGroupId, inlongStreamId);
            sendMessageCallBack.handleMessageSendException(topic, es, new NotFoundException("producer info is null"));
            return true;
        }

        Map<String, String> proMap = new HashMap<>();
        proMap.put("data_proxy_ip", localIp);
        proMap.put(inlongStreamId, event.getHeaders().get(ConfigConstants.PKG_TIME_KEY));

        TopicProducerInfo forCallBackP = producerInfo;
        Producer producer = producerInfo.getProducer(poolIndex);
        if (producer == null) {
            logger.warn("get producer is null! topic = {}", topic);
            checkAndResponse(event, inlongGroupId, inlongStreamId);
            sendMessageCallBack.handleMessageSendException(topic, es, new NotFoundException("producer is null"));
            return true;
        }
        if (es.isOrderMessage()) {
            String partitionKey = event.getHeaders().get(AttributeConstants.MESSAGE_PARTITION_KEY);
            try {
                MessageId msgId = producer.newMessage()
                        .properties(proMap)
                        .key(partitionKey)
                        .value(event.getBody())
                        .send();
                sendMessageCallBack.handleMessageSendSuccess(topic, msgId, es);
                AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_SEND_SUCCESS, event);
                forCallBackP.setCanUseSend(true);
                result = true;
            } catch (PulsarClientException ex) {
                forCallBackP.setCanUseSend(false);
                sendMessageCallBack.handleMessageSendException(topic, es, ex);
                result = ex instanceof NotFoundException;
            }
            /*
             * avoid client timeout
             */
            logger.debug("es.getRetryCnt() = {}", es.getRetryCnt());
            if (es.getRetryCnt() == 0 || es.getRetryCnt() == 1) {
                sendResponse((OrderEvent) event, inlongGroupId, inlongStreamId);
            }
        } else {
            producer.newMessage().properties(proMap)
                    .value(event.getBody())
                    .sendAsync()
                    .thenAccept((msgId) -> {
                        AuditUtils.add(AuditUtils.AUDIT_ID_DATAPROXY_SEND_SUCCESS, event);
                        forCallBackP.setCanUseSend(true);
                        sendMessageCallBack.handleMessageSendSuccess(topic, msgId, es);
                    })
                    .exceptionally((e) -> {
                        forCallBackP.setCanUseSend(false);
                        sendMessageCallBack.handleMessageSendException(topic, es, e);
                        return null;
                    });
            result = true;
        }
        return result;
    }

    private void checkAndResponse(Event event, String inlongGroupId, String inlongStreamId) {
        if (MessageUtils.isSyncSendForOrder(event) && (event instanceof OrderEvent)) {
            sendResponse((OrderEvent) event, inlongGroupId, inlongStreamId);
        }
    }

    /**
     * send Response
     *
     * @param orderEvent orderEvent
     */
    private void sendResponse(OrderEvent orderEvent, String inlongGroupId, String inlongStreamId) {
        String sequenceId = orderEvent.getHeaders().get(AttributeConstants.UNIQ_ID);
        if ("false".equals(orderEvent.getHeaders().get(AttributeConstants.MESSAGE_IS_ACK))) {
            if (logger.isDebugEnabled()) {
                logger.debug("not need to rsp message: seqId = {}, inlongGroupId = {}, inlongStreamId = {}",
                        sequenceId, inlongGroupId, inlongStreamId);
            }
            return;
        }
        if (orderEvent.getCtx() != null && orderEvent.getCtx().channel().isActive()) {
            orderEvent.getCtx().channel().eventLoop().execute(() -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("order message rsp: seqId = {}, inlongGroupId = {}, inlongStreamId = {}", sequenceId,
                            inlongGroupId, inlongStreamId);
                }
                ByteBuf binBuffer = MessageUtils.getResponsePackage("", MsgType.MSG_BIN_MULTI_BODY, sequenceId);
                orderEvent.getCtx().writeAndFlush(binBuffer);
            });
        }
    }

    /**
     * If this function is called successively without calling {@see #destroyConnection()}, only the
     * first call has any effect.
     *
     * @throws FlumeException if an RPC client connection could not be opened
     */
    private void createConnection(CreatePulsarClientCallBack callBack) throws FlumeException {
        if (!pulsarClients.isEmpty()) {
            return;
        }
        logger.debug("number of pulsar cluster is {}", pulsarUrl2token.size());
        for (Map.Entry<String, String> info : pulsarUrl2token.entrySet()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("url = {}, token = {}", info.getKey(), info.getValue());
                }
                PulsarClient client = initPulsarClient(info.getKey(), info.getValue());
                pulsarClients.put(info.getKey(), client);
                callBack.handleCreateClientSuccess(info.getKey());
            } catch (PulsarClientException e) {
                callBack.handleCreateClientException(info.getKey());
                logger.error("create connection error in Pulsar sink, "
                        + "maybe pulsar master set error, please re-check. url " + info.getKey(), e);
            } catch (Throwable e) {
                callBack.handleCreateClientException(info.getKey());
                logger.error("create connection error in pulsar sink, "
                        + "maybe pulsar master set error/shutdown in progress, please "
                        + "re-check. url " + info.getKey(), e);
            }
        }
        if (pulsarClients.isEmpty()) {
            throw new FlumeException("connect to pulsar error, maybe zkstr/zkroot set error, please re-check");
        }
    }

    private PulsarClient initPulsarClient(String pulsarUrl, String token) throws Exception {
        ClientBuilder builder = PulsarClient.builder();
        if (MQClusterConfig.PULSAR_DEFAULT_AUTH_TYPE.equals(authType) && StringUtils.isNotEmpty(token)) {
            builder.authentication(AuthenticationFactory.token(token));
        }
        builder.serviceUrl(pulsarUrl)
                .ioThreads(pulsarClientIoThreads)
                .connectionsPerBroker(pulsarConnectionsPreBroker)
                .connectionTimeout(clientTimeout, TimeUnit.SECONDS);
        return builder.build();
    }

    /**
     * Producer initialization.
     */
    public List<TopicProducerInfo> initTopicProducer(String topic, String inlongGroupId,
            String inlongStreamId) {
        List<TopicProducerInfo> producerInfoList = producerInfoMap.computeIfAbsent(topic, (k) -> {
            List<TopicProducerInfo> newList = new ArrayList<>();
            for (PulsarClient pulsarClient : pulsarClients.values()) {
                TopicProducerInfo info = new TopicProducerInfo(pulsarClient,
                        sinkThreadPoolSize, topic);
                info.initProducer(inlongGroupId, inlongStreamId);
                if (info.isCanUseToSendMessage()) {
                    newList.add(info);
                }
            }
            if (newList.size() == 0) {
                newList = null;
            }
            return newList;
        });
        return producerInfoList;
    }

    public List<TopicProducerInfo> initTopicProducer(String topic) {
        return initTopicProducer(topic, null, null);
    }

    private TopicProducerInfo getProducerInfo(int poolIndex, String topic, String inlongGroupId,
            String inlongStreamId) {
        List<TopicProducerInfo> producerList = initTopicProducer(topic, inlongGroupId, inlongStreamId);
        AtomicLong topicIndex = topicSendIndexMap.computeIfAbsent(topic, (k) -> new AtomicLong(0));
        int maxTryToGetProducer = producerList == null ? 0 : producerList.size();
        if (maxTryToGetProducer == 0) {
            return null;
        }
        int retryTime = 0;
        TopicProducerInfo p;
        do {
            int index = (int) (topicIndex.getAndIncrement() % maxTryToGetProducer);
            p = producerList.get(index);
            if (p.isCanUseToSendMessage() && p.getProducer(poolIndex) != null
                    && p.getProducer(poolIndex).isConnected()) {
                break;
            }
            retryTime++;
        } while (retryTime < maxTryToGetProducer);
        return p;
    }

    public Map<String, List<TopicProducerInfo>> getProducerInfoMap() {
        return producerInfoMap;
    }

    private void destroyConnection() {
        producerInfoMap.clear();
        for (PulsarClient pulsarClient : pulsarClients.values()) {
            try {
                pulsarClient.shutdown();
            } catch (Exception e) {
                logger.error("destroy pulsarClient error in PulsarSink: ", e);
            }
        }
        pulsarClients.clear();
        logger.debug("closed meta producer");
    }

    private void removeProducers(PulsarClient pulsarClient) {
        for (List<TopicProducerInfo> producers : producerInfoMap.values()) {
            for (TopicProducerInfo topicProducer : producers) {
                if (topicProducer.getPulsarClient().equals(pulsarClient)) {
                    topicProducer.close();
                    producers.remove(topicProducer);
                }
            }
        }
    }

    /**
     * close pulsarClients(the related url is removed); start pulsarClients for new url, and create producers for them
     *
     * @param callBack callback
     * @param needToClose url-token map
     * @param needToStart url-token map
     * @param topicSet for new pulsarClient, create these topics' producers
     */
    public void updatePulsarClients(CreatePulsarClientCallBack callBack, Map<String, String> needToClose,
            Map<String, String> needToStart, Set<String> topicSet) {
        // close
        for (String url : needToClose.keySet()) {
            PulsarClient pulsarClient = pulsarClients.get(url);
            if (pulsarClient != null) {
                try {
                    removeProducers(pulsarClient);
                    pulsarClient.shutdown();
                    pulsarClients.remove(url);
                } catch (Exception e) {
                    logger.error("shutdown pulsarClient error in PulsarSink: ", e);
                }
            }
        }
        for (Map.Entry<String, String> entry : needToStart.entrySet()) {
            String url = entry.getKey();
            String token = entry.getValue();
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("url = {}, token = {}", url, token);
                }
                PulsarClient client = initPulsarClient(url, token);
                pulsarClients.put(url, client);
                callBack.handleCreateClientSuccess(url);

                //create related topicProducers
                for (String topic : topicSet) {
                    TopicProducerInfo info = new TopicProducerInfo(client, sinkThreadPoolSize,
                            topic);
                    info.initProducer();
                    if (info.isCanUseToSendMessage()) {
                        producerInfoMap.computeIfAbsent(topic, k -> new ArrayList<>()).add(info);
                    }
                }

            } catch (PulsarClientException e) {
                callBack.handleCreateClientException(url);
                logger.error("create connection error in pulsar sink, "
                        + "maybe pulsar master set error, please re-check.url " + url, e);
            } catch (Throwable e) {
                callBack.handleCreateClientException(url);
                logger.error("create connection error in pulsar sink, "
                        + "maybe pulsar master set error/shutdown in progress, please "
                        + "re-check. url " + url, e);
            }
        }
    }

    /**
     * get inlong stream id from event
     *
     * @param event event
     * @return inlong stream id
     */
    private String getInlongStreamId(Event event) {
        String streamId = "";
        if (event.getHeaders().containsKey(AttributeConstants.STREAM_ID)) {
            streamId = event.getHeaders().get(AttributeConstants.STREAM_ID);
        } else if (event.getHeaders().containsKey(AttributeConstants.INAME)) {
            streamId = event.getHeaders().get(AttributeConstants.INAME);
        }
        return streamId;
    }

    /**
     * get inlong group id from event
     *
     * @param event event
     * @return inlong group id
     */
    private String getInlongGroupId(Event event) {
        return event.getHeaders().get(AttributeConstants.GROUP_ID);
    }

    public void close() {
        destroyConnection();
    }

    class TopicProducerInfo {

        private final Producer[] producers;
        private final PulsarClient pulsarClient;
        private final int sinkThreadPoolSize;
        private final String topic;
        private long lastSendMsgErrorTime;
        private volatile Boolean isCanUseSend = true;

        private volatile Boolean isFinishInit = false;

        public TopicProducerInfo(PulsarClient pulsarClient, int sinkThreadPoolSize, String topic) {
            this.pulsarClient = pulsarClient;
            this.sinkThreadPoolSize = sinkThreadPoolSize;
            this.topic = topic;
            this.producers = new Producer[sinkThreadPoolSize];
        }

        public void initProducer() {
            initProducer(null, null);
        }

        public void initProducer(String inlongGroupId, String inlongStreamId) {
            try {
                for (int i = 0; i < sinkThreadPoolSize; i++) {
                    producers[i] = createProducer();
                }
                isFinishInit = true;
            } catch (PulsarClientException e) {
                logger.error("create pulsar client has error, topic = {}, inlongGroupId = {}, inlongStreamId= {}",
                        topic, inlongGroupId, inlongStreamId, e);
                isFinishInit = false;
                for (int i = 0; i < sinkThreadPoolSize; i++) {
                    if (producers[i] != null) {
                        producers[i].closeAsync();
                    }
                }
            }
        }

        private Producer createProducer() throws PulsarClientException {
            return pulsarClient.newProducer().sendTimeout(sendTimeout, TimeUnit.MILLISECONDS)
                    .topic(topic)
                    .enableBatching(enableBatch)
                    .blockIfQueueFull(blockIfQueueFull)
                    .maxPendingMessages(maxPendingMessages)
                    .maxPendingMessagesAcrossPartitions(maxPendingMessagesAcrossPartitions)
                    .compressionType(compressionType)
                    .batchingMaxMessages(maxBatchingMessages)
                    .batchingMaxBytes(maxBatchingBytes)
                    .batchingMaxPublishDelay(maxBatchingPublishDelayMillis, TimeUnit.MILLISECONDS)
                    .create();
        }

        public void setCanUseSend(Boolean isCanUseSend) {
            this.isCanUseSend = isCanUseSend;
            if (!isCanUseSend) {
                lastSendMsgErrorTime = System.currentTimeMillis();
            }
        }

        public boolean isCanUseToSendMessage() {
            if (isCanUseSend && isFinishInit) {
                return true;
            } else if (isFinishInit
                    && (System.currentTimeMillis() - lastSendMsgErrorTime) > retryIntervalWhenSendMsgError) {
                lastSendMsgErrorTime = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        public void close() {
            try {
                for (int i = 0; i < sinkThreadPoolSize; i++) {
                    if (producers[i] != null) {
                        producers[i].close();
                    }
                }
            } catch (PulsarClientException e) {
                logger.error("close pulsar producer has error: ", e);
            }
        }

        public Producer getProducer(int poolIndex) {
            if (poolIndex >= sinkThreadPoolSize || producers[poolIndex] == null) {
                return producers[0];
            }
            return producers[poolIndex];
        }

        public PulsarClient getPulsarClient() {
            return pulsarClient;
        }
    }
}
