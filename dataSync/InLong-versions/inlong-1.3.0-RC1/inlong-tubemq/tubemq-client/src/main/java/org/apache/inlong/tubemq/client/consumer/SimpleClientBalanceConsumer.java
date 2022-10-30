/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.client.consumer;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.inlong.tubemq.client.common.ClientStatsInfo;
import org.apache.inlong.tubemq.client.common.ConfirmResult;
import org.apache.inlong.tubemq.client.common.ConsumeResult;
import org.apache.inlong.tubemq.client.common.QueryMetaResult;
import org.apache.inlong.tubemq.client.common.TClientConstants;
import org.apache.inlong.tubemq.client.common.TubeClientVersion;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.InnerSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.aaaclient.ClientAuthenticateHandler;
import org.apache.inlong.tubemq.corebase.aaaclient.SimpleClientAuthenticateHandler;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.DataConverterUtil;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.corerpc.RpcConfig;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.RpcServiceFactory;
import org.apache.inlong.tubemq.corerpc.service.BrokerReadService;
import org.apache.inlong.tubemq.corerpc.service.MasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleClientBalanceConsumer, This type of consumer supports the client for
 * independent partition allocation and consumption.
 * Compared with the server-side allocation scheme, this type of client manages the partition by
 * itself and is not affected by the server-side allocation cycle
 */
public class SimpleClientBalanceConsumer implements ClientBalanceConsumer {
    private static final Logger logger =
            LoggerFactory.getLogger(SimpleClientBalanceConsumer.class);

    private static final AtomicInteger consumerCounter =
            new AtomicInteger(0);
    protected final String consumerId;
    protected final ConsumerConfig consumerConfig;
    private final InnerSessionFactory sessionFactory;
    private final RpcServiceFactory rpcServiceFactory;
    private final MasterService masterService;
    // client status
    //
    // 0, not initial
    // 1, starting
    // 2, started
    // 3, stopping
    private final AtomicInteger clientStatus = new AtomicInteger(0);
    private int sourceCount = TBaseConstants.META_VALUE_UNDEFINED;
    private int nodeId = TBaseConstants.META_VALUE_UNDEFINED;
    protected final ClientSubInfo consumeSubInfo = new ClientSubInfo();
    protected final RmtDataCache clientRmtDataCache;
    private final ConsumerSamplePrint samplePrintCtrl =
            new ConsumerSamplePrint();
    private final RpcConfig rpcConfig = new RpcConfig();
    private final AtomicLong visitToken =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final AtomicReference<String> authAuthorizedTokenRef =
            new AtomicReference<>("");
    private final ClientAuthenticateHandler authenticateHandler =
            new SimpleClientAuthenticateHandler();
    private final ScheduledExecutorService heartService2Master;
    private final AtomicInteger metaReqStatusId = new AtomicInteger(0);
    private final AtomicLong lstMetaQueryTime = new AtomicLong(0);
    private final AtomicBoolean needMetaSelfChk = new AtomicBoolean(false);
    private int heartbeat2MRetryTimes = 0;
    private long lastHeartbeatTime2Master = 0;
    private Thread heartBeatThread2Broker;
    private long lastHeartbeatTime2Broker = 0;
    private final ConcurrentHashMap<String, Long> partRegFreqCtrlMap =
            new ConcurrentHashMap<>();
    protected final ClientStatsInfo clientStatsInfo;

    /**
     * Initial a client-balance consumer object
     * @param messageSessionFactory   the session factory
     * @param consumerConfig          the consumer configure
     * @throws TubeClientException    the exception while creating object.
     */
    public SimpleClientBalanceConsumer(final InnerSessionFactory messageSessionFactory,
                                       final ConsumerConfig consumerConfig) throws TubeClientException {
        java.security.Security.setProperty("networkaddress.cache.ttl", "3");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "1");
        if (messageSessionFactory == null || consumerConfig == null) {
            throw new TubeClientException(
                    "Illegal parameter: messageSessionFactory or consumerConfig is null!");
        }
        this.sessionFactory = messageSessionFactory;
        this.consumerConfig = consumerConfig;
        try {
            this.consumerId = generateConsumerID();
        } catch (Exception e) {
            throw new TubeClientException("Get consumer id failed!", e);
        }
        this.clientRmtDataCache =
                new RmtDataCache(this.consumerConfig, null);
        this.clientStatsInfo =
                new ClientStatsInfo(false, this.consumerId,
                        this.consumerConfig.getStatsConfig());
        this.rpcServiceFactory =
                this.sessionFactory.getRpcServiceFactory();
        this.rpcConfig.put(RpcConstants.CONNECT_TIMEOUT, 3000);
        this.rpcConfig.put(RpcConstants.REQUEST_TIMEOUT,
                this.consumerConfig.getRpcTimeoutMs());
        this.rpcConfig.put(RpcConstants.WORKER_THREAD_NAME,
                "tube_consumer_netty_worker-");
        this.rpcConfig.put(RpcConstants.CALLBACK_WORKER_COUNT,
                this.consumerConfig.getRpcRspCallBackThreadCnt());
        this.masterService =
                rpcServiceFactory.getFailoverService(MasterService.class,
                        this.consumerConfig.getMasterInfo(), this.rpcConfig);
        this.heartService2Master =
                Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, new StringBuilder(512)
                                .append("Master-Heartbeat-Thread-")
                                .append(consumerId).toString());
                        t.setPriority(Thread.MAX_PRIORITY);
                        return t;
                    }
                });
    }

    @Override
    public boolean start(Map<String, TreeSet<String>> topicAndFilterCondMap,
                         int sourceCount, int nodeId,
                         ProcessResult result) throws TubeClientException {
        if (result == null) {
            throw new TubeClientException("Illegal parameter: parameter result is null!");
        }
        StringBuilder sBuffer = new StringBuilder(512);
        if (!validAndStoreConsumeTarget(topicAndFilterCondMap, sBuffer, result)) {
            return result.isSuccess();
        }
        if (sourceCount > 0) {
            if (nodeId < 0 || nodeId > (sourceCount - 1)) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        "When groupNodeCnt is valid, the nodeId value must be between in [0, sourceCount-1]!");
                return result.isSuccess();
            }
        }
        // judge client status
        if (clientStatus.get() != 0) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "The SDK is running, please shutdown first!");
            return result.isSuccess();
        }
        if (!clientStatus.compareAndSet(0, 1)) {
            switch (clientStatus.get()) {
                case 2: {
                    result.setSuccResult();
                }
                break;

                case 3: {
                    result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                            "The client is shutting down. Please try again later!");
                }
                break;

                case 0:
                case 1:
                default: {
                    result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                            "Duplicated calls, the client is starting, please wait a minute!");
                }
            }
            return result.isSuccess();
        }
        if (sourceCount > 0) {
            this.sourceCount = sourceCount;
            this.nodeId = nodeId;
        }
        // store consume target information
        Map<String, TreeSet<String>> consumeTargetMap =
                (Map<String, TreeSet<String>>) result.getRetData();
        this.consumeSubInfo.storeConsumeTarget(consumeTargetMap);
        if (!startMasterAndBrokerThreads(result, sBuffer)) {
            clientStatus.compareAndSet(1, 0);
            return result.isSuccess();
        }
        clientStatus.compareAndSet(1, 2);
        result.setSuccResult();
        return result.isSuccess();
    }

    @Override
    public boolean isShutdown() {
        int tmpStatusId = this.clientStatus.get();
        return (tmpStatusId <= 0 || tmpStatusId > 2);
    }

    @Override
    public ConsumerConfig getConsumerConfig() {
        return this.consumerConfig;
    }

    @Override
    public String getConsumerId() {
        return this.consumerId;
    }

    @Override
    public boolean isFilterConsume(String topic) {
        return this.consumeSubInfo.isFilterConsume(topic);
    }

    @Override
    public int getSourceCount() {
        return this.sourceCount;
    }

    @Override
    public int getNodeId() {
        return this.nodeId;
    }

    @Override
    public String getClientVersion() {
        return TubeClientVersion.CONSUMER_VERSION;
    }

    @Override
    public void shutdown() throws Throwable {
        StringBuilder strBuffer = new StringBuilder(512);
        if (!clientStatus.compareAndSet(2, 3)) {
            switch (clientStatus.get()) {
                case 3: {
                    logger.info(strBuffer.append("[SHUTDOWN_CONSUMER] ")
                            .append(this.consumerId)
                            .append(" is shutting down, do nothing...").toString());
                }
                break;

                case 0: {
                    logger.info(strBuffer.append("[SHUTDOWN_CONSUMER] ")
                            .append(this.consumerId)
                            .append(" was already shutdown, do nothing...").toString());
                }
                break;

                case 1:
                default: {
                    logger.info(strBuffer.append("[SHUTDOWN_CONSUMER] ")
                            .append(this.consumerId)
                            .append(" is starting, please wait a minute!").toString());
                }
            }
            return;
        }
        logger.info(strBuffer.append("[SHUTDOWN_CONSUMER] Shutting down consumer:")
                .append(this.consumerId).toString());
        strBuffer.delete(0, strBuffer.length());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //
        }
        //
        this.clientRmtDataCache.close();
        Map<BrokerInfo, List<PartitionSelectResult>> unRegisterInfoMap =
                clientRmtDataCache.getAllPartitionListWithStatus();
        unregisterPartitions(unRegisterInfoMap);
        this.sessionFactory.removeClient(this);
        if (this.heartService2Master != null) {
            try {
                this.heartService2Master.shutdownNow();
            } catch (Throwable ee) {
                //
            }
        }
        if (this.heartBeatThread2Broker != null) {
            try {
                this.heartBeatThread2Broker.interrupt();
                heartBeatThread2Broker.join();
                this.heartBeatThread2Broker = null;
            } catch (Throwable ee) {
                //
            }
        }
        // print metric information
        clientStatsInfo.selfPrintStatsInfo(true, true, strBuffer);
        logger.info(strBuffer
                .append("[SHUTDOWN_CONSUMER] Partitions unregistered,  consumer :")
                .append(this.consumerId).toString());
        strBuffer.delete(0, strBuffer.length());
        try {
            masterService.consumerCloseClientC2M(createMasterCloseRequest(),
                    AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
        } catch (Throwable e) {
            strBuffer.delete(0, strBuffer.length());
            logger.warn(strBuffer
                    .append("[SHUTDOWN_CONSUMER] call closeRequest failure, error is ")
                    .append(e.getMessage()).toString());
            strBuffer.delete(0, strBuffer.length());
        }
        logger.info(strBuffer.append("[SHUTDOWN_CONSUMER] Client closed, consumer : ")
                .append(this.consumerId).toString());
        this.clientStatus.set(0);
    }

    @Override
    public Set<String> getCurRegisteredPartSet() {
        return clientRmtDataCache.getCurRegisteredPartSet();
    }

    @Override
    public Map<String, ConsumeOffsetInfo> getCurPartitionOffsetInfos() {
        return this.clientRmtDataCache.getCurPartitionInfoMap();
    }

    @Override
    public boolean isPartitionsReady(long maxWaitTime) {
        return clientRmtDataCache.isPartitionsReady(maxWaitTime);
    }

    @Override
    public boolean getPartitionMetaInfo(QueryMetaResult result) throws TubeClientException {
        if (result == null) {
            throw new TubeClientException("Illegal parameter: parameter result is null!");
        }
        StringBuilder sBuffer = new StringBuilder(512);
        if (isShutdown()) {
            result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                    "The client is not started or closed!");
            return result.isSuccess();
        }
        if (System.currentTimeMillis() - lstMetaQueryTime.get()
                >= consumerConfig.getPartMetaInfoCheckPeriodMs()) {
            if (metaReqStatusId.compareAndSet(0, 1)) {
                try {
                    ClientMaster.GetPartMetaResponseM2C response =
                            masterService.consumerGetPartMetaInfoC2M(createMasterGetPartMetaRequest(),
                                    AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
                    if (response == null) {
                        result.setFailResult(TErrCodeConstants.CONNECT_RETURN_NULL,
                                sBuffer.append("Query Failed: ").append(consumerId)
                                        .append(" query master and return null!").toString());
                        sBuffer.delete(0, sBuffer.length());
                        return result.isSuccess();
                    }
                    needMetaSelfChk.set(false);
                    lstMetaQueryTime.set(System.currentTimeMillis());
                    if (response.getErrCode() != TErrCodeConstants.SUCCESS) {
                        // If the consumer group is forbidden, output the log
                        result.setFailResult(response.getErrCode(), response.getErrMsg());
                        return result.isSuccess();
                    }
                    // Process the successful response
                    if (response.hasBrokerConfigId()) {
                        clientRmtDataCache.updateBrokerInfoList(response.getBrokerConfigId(),
                                response.getBrokerConfigListList(), sBuffer);
                    }
                    if (response.hasTopicMetaInfoId()) {
                        // update local cache meta information
                        clientRmtDataCache.storeTopicMetaInfo(response.getTopicMetaInfoId(),
                                response.getTopicMetaInfoListList());
                        // clear unsubscribable partitions
                        clearUnSubscribablePartitions();
                    }
                } catch (Throwable e) {
                    result.setFailResult(TErrCodeConstants.INTERNAL_SERVER_ERROR,
                            sBuffer.append("Query MetaInfo throw exception: ")
                                    .append(e.getCause()).toString());
                    sBuffer.delete(0, sBuffer.length());
                    return result.isSuccess();
                } finally {
                    metaReqStatusId.set(0);
                }
            }
        }
        result.setSuccResult(clientRmtDataCache.getConfPartMetaInfo());
        return result.isSuccess();
    }

    @Override
    public boolean connect2Partition(String partitionKey,
                                     long boostrapOffset,
                                     ProcessResult result) throws TubeClientException {
        if (result == null) {
            throw new TubeClientException("Illegal parameter: parameter result is null!");
        }
        final StringBuilder sBuffer = new StringBuilder(512);
        if (TStringUtils.isBlank(partitionKey)) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Parameter partitionKey is blank!");
            return result.isSuccess();
        }
        if (isShutdown()) {
            result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                    "The client is not started or closed!");
            return result.isSuccess();
        }
        if (clientRmtDataCache.isPartitionInUse(partitionKey)) {
            result.setSuccResult();
            return result.isSuccess();
        }
        if (!clientRmtDataCache.getSubscribablePartition(
                partitionKey, result, sBuffer)) {
            return result.isSuccess();
        }
        // check if high frequency request for failure partition
        Long lstTime = partRegFreqCtrlMap.get(partitionKey);
        if (lstTime != null && (System.currentTimeMillis() - lstTime
                < TClientConstants.CFG_MIN_META_QUERY_WAIT_PERIOD_MS)) {
            result.setFailResult(TErrCodeConstants.CLIENT_HIGH_FREQUENCY_REQUEST,
                    sBuffer.append("High-frequency request, please call ").append(partitionKey)
                            .append(" at least ").append(TClientConstants.CFG_MIN_META_QUERY_WAIT_PERIOD_MS)
                            .append("ms interval!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        Partition partition = (Partition) result.getRetData();
        final String uniqueId =
                sBuffer.append(consumerConfig.getConsumerGroup())
                        .append("#").append(partitionKey).toString();
        sBuffer.delete(0, sBuffer.length());
        synchronized (uniqueId) {
            registerPartitions(partition, boostrapOffset, result, sBuffer);
        }
        if (!result.isSuccess()
                && (result.getErrCode() == TErrCodeConstants.PARTITION_OCCUPIED
                || result.getErrCode() == TErrCodeConstants.CERTIFICATE_FAILURE)) {
            // only partition occupied or certificate failure need control frequency
            partRegFreqCtrlMap.put(partitionKey, System.currentTimeMillis());
        }
        return result.isSuccess();
    }

    @Override
    public boolean disconnectFromPartition(String partitionKey,
                                           ProcessResult result) throws TubeClientException {
        if (result == null) {
            throw new TubeClientException("Illegal parameter: parameter result is null!");
        }
        final StringBuilder sBuffer = new StringBuilder(512);
        if (TStringUtils.isBlank(partitionKey)) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Parameter partitionKey is blank!");
            return result.isSuccess();
        }
        if (isShutdown()) {
            result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                    "The client is not started or closed!");
            return result.isSuccess();
        }
        if (!clientRmtDataCache.isPartitionInUse(partitionKey)) {
            result.setSuccResult();
            return result.isSuccess();
        }
        clientRmtDataCache.removeAndGetPartition(partitionKey,
                this.consumerConfig.getPullRebConfirmWaitPeriodMs(),
                this.consumerConfig.isPullRebConfirmTimeoutRollBack(), result, sBuffer);
        PartitionExt partitionExt = (PartitionExt) result.getRetData();
        if (partitionExt == null) {
            result.setSuccResult();
            return result.isSuccess();
        }
        unregisterPartition(partitionExt, partitionExt.isLastPackConsumed(), sBuffer);
        result.setSuccResult();
        return result.isSuccess();
    }

    @Override
    public boolean getMessage(ConsumeResult result) throws TubeClientException {
        if (result == null) {
            throw new TubeClientException("Illegal parameter: parameter result is null!");
        }
        if (isShutdown()) {
            result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                    "The client is not started or closed!");
            return result.isSuccess();
        }
        PartitionSelectResult selectResult = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            if (isShutdown()) {
                result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                        "The client has been shutdown!");
                return result.isSuccess();
            }
            selectResult = clientRmtDataCache.getCurrPartsStatus();
            if (selectResult.isSuccess()) {
                break;
            }
            if ((consumerConfig.getPullConsumeReadyWaitPeriodMs() >= 0L)
                    && ((System.currentTimeMillis() - startTime)
                    >= consumerConfig.getPullConsumeReadyWaitPeriodMs())) {
                result.setFailResult(selectResult.getErrCode(), selectResult.getErrMsg());
                return result.isSuccess();
            }
            if (consumerConfig.getPullConsumeReadyChkSliceMs() > 0L) {
                ThreadUtils.sleep(consumerConfig.getPullConsumeReadyChkSliceMs());
            }
        }
        StringBuilder sBuilder = new StringBuilder(512);
        // Check the data cache first
        selectResult = clientRmtDataCache.pullSelect();
        if (!selectResult.isSuccess()) {
            result.setFailResult(selectResult.getErrCode(), selectResult.getErrMsg());
            return result.isSuccess();
        }
        FetchContext taskContext = fetchMessage(selectResult, sBuilder);
        result.setProcessResult(taskContext);
        return result.isSuccess();
    }

    @Override
    public boolean confirmConsume(String confirmContext, boolean isConsumed,
                                  ConfirmResult result) throws TubeClientException {
        if (result == null) {
            throw new TubeClientException("Illegal parameter: parameter result is null!");
        }
        StringBuilder sBuilder = new StringBuilder(512);
        if (isShutdown()) {
            result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                    "The client is not started or closed!");
            return result.isSuccess();
        }
        long currOffset = TBaseConstants.META_VALUE_UNDEFINED;
        long maxOffset = TBaseConstants.META_VALUE_UNDEFINED;
        // Verify if the confirmContext is valid
        if (TStringUtils.isBlank(confirmContext)) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "ConfirmContext is null!");
            return result.isSuccess();
        }
        String[] strConfirmContextItems =
                confirmContext.split(TokenConstants.ATTR_SEP);
        if (strConfirmContextItems.length != 4) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "ConfirmContext format error: value must be aaaa:bbbb:cccc:ddddd !");
            return result.isSuccess();
        }
        for (String itemStr : strConfirmContextItems) {
            if (TStringUtils.isBlank(itemStr)) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        sBuilder.append("ConfirmContext's format error: item (")
                                .append(itemStr).append(") is null !").toString());
                sBuilder.delete(0, sBuilder.length());
                return result.isSuccess();
            }
        }
        String keyId = sBuilder.append(strConfirmContextItems[0].trim())
                .append(TokenConstants.ATTR_SEP).append(strConfirmContextItems[1].trim())
                .append(TokenConstants.ATTR_SEP).append(strConfirmContextItems[2].trim()).toString();
        sBuilder.delete(0, sBuilder.length());
        String topicName = strConfirmContextItems[1].trim();
        long timeStamp = Long.parseLong(strConfirmContextItems[3]);
        long midTime = System.currentTimeMillis();
        // book statistics information
        clientStatsInfo.bookReturnDuration(keyId, midTime - timeStamp);
        if (!clientRmtDataCache.isPartitionInUse(keyId, timeStamp)) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "The confirmContext's value invalid!");
            return result.isSuccess();
        }
        Partition curPartition =
                clientRmtDataCache.getPartitionByKey(keyId);
        if (curPartition == null) {
            result.setFailResult(TErrCodeConstants.NOT_FOUND,
                    sBuilder.append("Not found the partition by confirmContext:")
                            .append(confirmContext).toString());
            sBuilder.delete(0, sBuilder.length());
            return result.isSuccess();
        }
        if (this.consumerConfig.isPullConfirmInLocal()) {
            clientRmtDataCache.succRspRelease(keyId, topicName,
                    timeStamp, isConsumed, isFilterConsume(topicName), currOffset, maxOffset);
            result.setSuccResult(topicName, curPartition, currOffset, maxOffset);
            return result.isSuccess();
        } else {
            try {
                ClientBroker.CommitOffsetResponseB2C response =
                        getBrokerService(curPartition.getBroker())
                                .consumerCommitC2B(createBrokerCommitRequest(curPartition, isConsumed),
                                        AddressUtils.getLocalAddress(), getConsumerConfig().isTlsEnable());
                if (response == null) {
                    result.setFailResult(TErrCodeConstants.CONNECT_RETURN_NULL,
                            sBuilder.append("Confirm ").append(confirmContext)
                                    .append("'s offset failed, response is null!").toString());
                    sBuilder.delete(0, sBuilder.length());
                    return result.isSuccess();
                } else {
                    if (response.hasCurrOffset() && response.getCurrOffset() >= 0) {
                        currOffset = response.getCurrOffset();
                    }
                    if (response.hasMaxOffset() && response.getMaxOffset() >= 0) {
                        maxOffset = response.getMaxOffset();
                    }
                    result.setProcessResult(response.getSuccess(),
                            response.getErrCode(), response.getErrMsg(),
                            topicName, curPartition, currOffset, maxOffset);
                    return result.isSuccess();
                }
            } catch (Throwable e) {
                sBuilder.delete(0, sBuilder.length());
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        sBuilder.append("Confirm ").append(confirmContext)
                                .append("'s offset failed, exception is ")
                                .append(e.toString()).toString());
                sBuilder.delete(0, sBuilder.length());
                return result.isSuccess();
            } finally {
                clientRmtDataCache.succRspRelease(keyId, topicName, timeStamp,
                        isConsumed, isFilterConsume(topicName), currOffset, maxOffset);
                clientStatsInfo.bookConfirmDuration(keyId,
                        System.currentTimeMillis() - midTime);
            }
        }
    }

    /**
     * Register partitions.
     *
     * @param partition need register partition
     * @param boostrapOffset boostrap offset
     * @param result process result
     * @param sBuffer  string buffer
     *
     * @return process result
     */
    private boolean registerPartitions(Partition partition, long boostrapOffset,
                                       ProcessResult result, StringBuilder sBuffer) {
        int maxRegisterRetryTimes = 2;
        int retryTimesRegister2Broker = 0;
        while (retryTimesRegister2Broker < maxRegisterRetryTimes) {
            if (isShutdown()) {
                result.setFailResult(TErrCodeConstants.CLIENT_SHUTDOWN,
                        "The client is not started or closed!");
                return result.isSuccess();
            }
            if (clientRmtDataCache.isPartitionInUse(partition.getPartitionKey())) {
                result.setSuccResult();
                return result.isSuccess();
            }
            if (tryRegister2Broker(partition, boostrapOffset, result, sBuffer)) {
                return result.isSuccess();
            }
            logger.warn(sBuffer.append("register ")
                    .append(partition.toString()).append(" failure(")
                    .append(retryTimesRegister2Broker).append("), return ")
                    .append(result.getErrMsg()).toString());
            retryTimesRegister2Broker++;
            ThreadUtils.sleep(1000);
        }
        return result.isSuccess();
    }

    private FetchContext fetchMessage(PartitionSelectResult partSelectResult,
                                      StringBuilder sBuffer) {
        // Fetch task context based on selected partition
        FetchContext taskContext =
                new FetchContext(partSelectResult);
        Partition partition = taskContext.getPartition();
        String topic = partition.getTopic();
        String partitionKey = partition.getPartitionKey();
        long startTime = System.currentTimeMillis();
        // Response from broker
        ClientBroker.GetMessageResponseB2C msgRspB2C = null;
        try {
            msgRspB2C =
                    getBrokerService(partition.getBroker())
                            .getMessagesC2B(createBrokerGetMessageRequest(
                                    partition, taskContext.isLastConsumed()),
                                    AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
        } catch (Throwable ee) {
            // Process the exception
            clientStatsInfo.bookFailRpcCall(TErrCodeConstants.BAD_REQUEST);
            clientRmtDataCache.errReqRelease(partitionKey, taskContext.getUsedToken(), false);
            taskContext.setFailProcessResult(400, sBuffer
                    .append("Get message error, reason is ")
                    .append(ee.toString()).toString());
            sBuffer.delete(0, sBuffer.length());
            return taskContext;
        }
        long dltTime = System.currentTimeMillis() - startTime;
        if (msgRspB2C == null) {
            clientStatsInfo.bookFailRpcCall(TErrCodeConstants.INTERNAL_SERVER_ERROR);
            clientRmtDataCache.errReqRelease(partitionKey, taskContext.getUsedToken(), false);
            taskContext.setFailProcessResult(500, "Get message null");
            return taskContext;
        }
        try {
            // Process the response based on the return code
            switch (msgRspB2C.getErrCode()) {
                case TErrCodeConstants.SUCCESS: {
                    int msgSize = 0;
                    int msgCount = 0;
                    // Convert the message payload data
                    List<Message> tmpMessageList =
                            DataConverterUtil.convertMessage(topic, msgRspB2C.getMessagesList());
                    boolean isEscLimit =
                            (msgRspB2C.hasEscFlowCtrl() && msgRspB2C.getEscFlowCtrl());
                    // Filter the message based on its content
                    // Calculate the message size and do some flow control
                    boolean needFilter = false;
                    Set<String> topicFilterSet = null;
                    TopicProcessor topicProcessor = consumeSubInfo.getTopicProcessor(topic);
                    if (topicProcessor != null) {
                        topicFilterSet = topicProcessor.getFilterConds();
                        if (topicFilterSet != null && !topicFilterSet.isEmpty()) {
                            needFilter = true;
                        }
                    }
                    List<Message> messageList = new ArrayList<>();
                    for (Message message : tmpMessageList) {
                        if (message == null) {
                            continue;
                        }
                        if (needFilter && (TStringUtils.isBlank(message.getMsgType())
                                || !topicFilterSet.contains(message.getMsgType()))) {
                            continue;
                        }
                        msgCount++;
                        messageList.add(message);
                        msgSize += message.getData().length;
                    }
                    // Set the process result of current stage. Process the result based on the response
                    long dataDltVal = msgRspB2C.hasCurrDataDlt()
                            ? msgRspB2C.getCurrDataDlt() : -1;
                    long currOffset = msgRspB2C.hasCurrOffset()
                            ? msgRspB2C.getCurrOffset() : TBaseConstants.META_VALUE_UNDEFINED;
                    long maxOffset = msgRspB2C.hasMaxOffset()
                            ? msgRspB2C.getMaxOffset() : TBaseConstants.META_VALUE_UNDEFINED;
                    boolean isRequireSlow =
                            (msgRspB2C.hasRequireSlow() && msgRspB2C.getRequireSlow());
                    clientRmtDataCache
                            .setPartitionContextInfo(partitionKey, currOffset, 1,
                                    msgRspB2C.getErrCode(), isEscLimit, msgSize, 0,
                                    dataDltVal, isRequireSlow, maxOffset);
                    taskContext.setSuccessProcessResult(currOffset,
                            sBuffer.append(partitionKey).append(TokenConstants.ATTR_SEP)
                                    .append(taskContext.getUsedToken()).toString(), messageList, maxOffset);
                    sBuffer.delete(0, sBuffer.length());
                    clientStatsInfo.bookSuccGetMsg(dltTime,
                            topic, partitionKey, msgCount, msgSize);
                    break;
                }
                case TErrCodeConstants.HB_NO_NODE:
                case TErrCodeConstants.CERTIFICATE_FAILURE:
                case TErrCodeConstants.DUPLICATE_PARTITION: {
                    // Release the partitions when meeting these error codes
                    clientRmtDataCache.removePartition(partition);
                    taskContext.setFailProcessResult(msgRspB2C.getErrCode(), msgRspB2C.getErrMsg());
                    break;
                }
                case TErrCodeConstants.SERVER_CONSUME_SPEED_LIMIT: {
                    // Process with server side speed limit
                    long defDltTime =
                            msgRspB2C.hasMinLimitTime()
                                    ? msgRspB2C.getMinLimitTime() : consumerConfig.getMsgNotFoundWaitPeriodMs();
                    clientRmtDataCache.errRspRelease(partitionKey, topic,
                            taskContext.getUsedToken(), false, TBaseConstants.META_VALUE_UNDEFINED,
                            0, msgRspB2C.getErrCode(), false, 0,
                            defDltTime, isFilterConsume(topic), TBaseConstants.META_VALUE_UNDEFINED,
                            TBaseConstants.META_VALUE_UNDEFINED);
                    taskContext.setFailProcessResult(msgRspB2C.getErrCode(), msgRspB2C.getErrMsg());
                    break;
                }
                case TErrCodeConstants.NOT_FOUND:
                case TErrCodeConstants.FORBIDDEN:
                case TErrCodeConstants.SERVICE_UNAVAILABLE:
                case TErrCodeConstants.MOVED:
                default: {
                    // Slow down the request based on the limitation configuration when meet these errors
                    long limitDlt = 300;
                    switch (msgRspB2C.getErrCode()) {
                        case TErrCodeConstants.FORBIDDEN: {
                            limitDlt = 2000;
                            break;
                        }
                        case TErrCodeConstants.SERVICE_UNAVAILABLE: {
                            limitDlt = 300;
                            break;
                        }
                        case TErrCodeConstants.MOVED: {
                            limitDlt = 200;
                            break;
                        }
                        case TErrCodeConstants.NOT_FOUND: {
                            limitDlt = consumerConfig.getMsgNotFoundWaitPeriodMs();
                            break;
                        }
                        default: {
                            //
                        }
                    }
                    clientRmtDataCache.errRspRelease(partitionKey, topic,
                            taskContext.getUsedToken(), false, TBaseConstants.META_VALUE_UNDEFINED,
                            0, msgRspB2C.getErrCode(), false, 0,
                            limitDlt, isFilterConsume(topic), -1, TBaseConstants.META_VALUE_UNDEFINED);
                    taskContext.setFailProcessResult(msgRspB2C.getErrCode(), msgRspB2C.getErrMsg());
                    break;
                }
            }
            if (msgRspB2C.getErrCode() != TErrCodeConstants.SUCCESS) {
                clientStatsInfo.bookFailRpcCall(msgRspB2C.getErrCode());
            }
            return taskContext;
        } catch (Throwable ee) {
            clientStatsInfo.bookFailRpcCall(TErrCodeConstants.INTERNAL_SERVER_ERROR);
            logger.error("Process response code error", ee);
            clientRmtDataCache.succRspRelease(partitionKey, topic,
                    taskContext.getUsedToken(), false, isFilterConsume(topic),
                    TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED);
            taskContext.setFailProcessResult(500, sBuffer
                    .append("Get message failed,topic=")
                    .append(topic).append(",partition=").append(partition)
                    .append(", throw info is ").append(ee.toString()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
        return taskContext;
    }

    /**
     * Start heartbeat thread threads.
     *
     * @param result  process result
     * @param sBuffer  string buffer
     *
     * @return  process result
     */
    private boolean startMasterAndBrokerThreads(ProcessResult result,
                                                StringBuilder sBuffer) {
        int registerRetryTimes = 0;
        while (registerRetryTimes < consumerConfig.getMaxRegisterRetryTimes()) {
            if (tryRegister2Master(result, sBuffer)) {
                logger.info(sBuffer.append("[Registered] ")
                        .append(consumerId).toString());
                sBuffer.delete(0, sBuffer.length());
                break;
            } else {
                logger.error(result.getErrMsg());
                ThreadUtils.sleep(this.consumerConfig.getRegFailWaitPeriodMs());
            }
            if (++registerRetryTimes >= consumerConfig.getMaxRegisterRetryTimes()) {
                logger.error(result.getErrMsg());
                return result.isSuccess();
            }
        }
        // to master heartbeat
        this.lastHeartbeatTime2Master = System.currentTimeMillis();
        this.heartService2Master.scheduleWithFixedDelay(new HeartTask2MasterWorker(),
                0, consumerConfig.getHeartbeatPeriodMs(), TimeUnit.MILLISECONDS);
        // to broker
        this.lastHeartbeatTime2Broker = System.currentTimeMillis();
        this.heartBeatThread2Broker = new Thread(new HeartTask2BrokerWorker());
        heartBeatThread2Broker.setName(sBuffer
                .append("Broker-Heartbeat-Thread-")
                .append(consumerId).toString());
        sBuffer.delete(0, sBuffer.length());
        heartBeatThread2Broker.setPriority(Thread.MAX_PRIORITY);
        heartBeatThread2Broker.start();
        result.setSuccResult();
        return result.isSuccess();
    }

    private boolean validAndStoreConsumeTarget(Map<String, TreeSet<String>> consumeTargetMap,
                                               StringBuilder sBuffer, ProcessResult result) {
        if (consumeTargetMap == null
                || consumeTargetMap.isEmpty()) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Parameter error: the subscribed target is null or empty!");
            return result.isSuccess();
        }
        String topicName;
        String tmpFilter;
        TreeSet<String> filterCondSet;
        TreeSet<String> newFilterCondSet;
        Map<String, TreeSet<String>> newConsumeTargetMap = new HashMap<>();
        for (Map.Entry<String, TreeSet<String>> entry : consumeTargetMap.entrySet()) {
            // check topic value
            topicName = entry.getKey();
            if (TStringUtils.isBlank(topicName)) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        "Parameter error: an blank Topic field,topic is blank in map!");
                return result.isSuccess();
            }
            topicName = topicName.trim();
            if (topicName.length() > TBaseConstants.META_MAX_TOPICNAME_LENGTH) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        sBuffer.append("Parameter error: the max length of ")
                                .append(topicName).append(" in topicName parameter over ")
                                .append(TBaseConstants.META_MAX_TOPICNAME_LENGTH)
                                .append(" characters").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            // check topic's filter condition value
            filterCondSet = entry.getValue();
            newFilterCondSet = new TreeSet<>();
            if ((filterCondSet != null) && (!filterCondSet.isEmpty())) {
                if (filterCondSet.size() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT) {
                    result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                            sBuffer.append("Parameter error: over max allowed filter count of ")
                                    .append(topicName).append(", allowed count is ")
                                    .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT)
                                    .toString());
                    sBuffer.delete(0, sBuffer.length());
                    return result.isSuccess();
                }
                for (String filter : filterCondSet) {
                    if (TStringUtils.isBlank(filter)) {
                        result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                                sBuffer.append("Parameter error: include blank filter value of ")
                                        .append(topicName).toString());
                        sBuffer.delete(0, sBuffer.length());
                        return result.isSuccess();
                    }
                    tmpFilter = filter.trim();
                    if (tmpFilter.length() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH) {
                        result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                                sBuffer.append("Parameter error: over max allowed filter length, ")
                                        .append(tmpFilter).append(" in ").append(topicName)
                                        .append(", allowed length is ")
                                        .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH)
                                        .toString());
                        sBuffer.delete(0, sBuffer.length());
                        return result.isSuccess();
                    }
                    newFilterCondSet.add(tmpFilter);
                }
            }
            newConsumeTargetMap.put(topicName, newFilterCondSet);
        }
        result.setSuccResult(newConsumeTargetMap);
        return true;
    }

    // #lizard forgives
    private class HeartTask2MasterWorker implements Runnable {
        // Heartbeat logic between master and worker
        @Override
        public void run() {
            ProcessResult result = new ProcessResult();
            StringBuilder strBuffer = new StringBuilder(512);
            try {
                clientRmtDataCache.resumeTimeoutConsumePartitions(false,
                        consumerConfig.getPullProtectConfirmTimeoutMs());
                // print metric information
                clientStatsInfo.selfPrintStatsInfo(false, true, strBuffer);
                // Send heartbeat request to master
                ClientMaster.HeartResponseM2CV2 response =
                        masterService.consumerHeartbeatC2MV2(createMasterHeartBeatRequest(),
                                AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
                // Process unsuccessful response
                if (response == null) {
                    clientStatsInfo.bookHB2MasterTimeout();
                    logger.warn(strBuffer.append("[Heartbeat Failed] ")
                            .append("return result is null!").toString());
                    strBuffer.delete(0, strBuffer.length());
                    heartbeat2MRetryTimes++;
                    return;
                }
                if (response.getErrCode() != TErrCodeConstants.SUCCESS) {
                    // If master replies that cannot find current consumer node, re-register
                    if (response.getErrCode() == TErrCodeConstants.HB_NO_NODE) {
                        clientStatsInfo.bookHB2MasterTimeout();
                        if (tryRegister2Master(result, strBuffer)) {
                            logger.info(strBuffer.append("[Re-register] ")
                                    .append(consumerId).toString());
                            strBuffer.delete(0, strBuffer.length());
                        } else {
                            logger.info(result.getErrMsg());
                        }
                        return;
                    }
                    clientStatsInfo.bookHB2MasterException();
                    logger.error(strBuffer.append("[Heartbeat Failed] ")
                            .append(response.getErrMsg()).toString());
                    if (response.getErrCode() == TErrCodeConstants.CERTIFICATE_FAILURE) {
                        adjustHeartBeatPeriod("certificate failure", strBuffer);
                    } else {
                        heartbeat2MRetryTimes++;
                    }
                    return;
                }
                // Process the heartbeat success response
                heartbeat2MRetryTimes = 0;
                clientRmtDataCache.updateBrokerInfoList(
                        response.getBrokerConfigId(),
                        response.getBrokerConfigListList(), strBuffer);
                if (response.hasTopicMetaInfoId()) {
                    // update local cache meta information
                    needMetaSelfChk.compareAndSet(false, true);
                    clientRmtDataCache.storeTopicMetaInfo(response.getTopicMetaInfoId(),
                            response.getTopicMetaInfoListList());
                    lstMetaQueryTime.set(System.currentTimeMillis());
                }
                // Get the authorization rules and update the local rules
                clientRmtDataCache.updOpsTaskInfo(response.getOpsTaskInfo());
                // Get the latest authorized token
                processHeartBeatAuthorizedToken(response);
                // Warning if heartbeat interval is too long
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastHeartbeatTime2Master)
                        > consumerConfig.getHeartbeatPeriodMs() * 2) {
                    logger.warn(strBuffer.append(consumerId)
                            .append(" heartbeat interval to master is too long,please check! Total time : ")
                            .append(currentTime - lastHeartbeatTime2Master).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
                lastHeartbeatTime2Master = currentTime;
            } catch (InterruptedException ee) {
                logger.info("To Master Heartbeat thread is interrupted,existed!");
            } catch (Throwable e) {
                // Print the log when meeting heartbeat errors.
                // Reduce the heartbeat request frequency when failure count exceed the threshold
                if (!isShutdown()) {
                    samplePrintCtrl.printExceptionCaught(e);
                }
                adjustHeartBeatPeriod("heartbeat exception", strBuffer);
            }
        }

        private void adjustHeartBeatPeriod(String reason, StringBuilder sBuilder) {
            lastHeartbeatTime2Master = System.currentTimeMillis();
            heartbeat2MRetryTimes++;
            if (!isShutdown()
                    && heartbeat2MRetryTimes > consumerConfig.getMaxHeartBeatRetryTimes()) {
                logger.warn(sBuilder.append("Adjust HeartbeatPeriod for ").append(reason)
                        .append(", sleep ").append(consumerConfig.getHeartbeatPeriodAfterFail())
                        .append(" Ms").toString());
                sBuilder.delete(0, sBuilder.length());
                ThreadUtils.sleep(consumerConfig.getHeartbeatPeriodAfterFail());
            }
        }
    }

    private boolean tryRegister2Broker(Partition partition, long boostrapOffset,
                                       ProcessResult result, StringBuilder sBuffer) {
        try {
            ClientBroker.RegisterResponseB2C response =
                    getBrokerService(partition.getBroker()).consumerRegisterC2B(
                            createBrokerRegisterRequest(partition, boostrapOffset),
                            AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
            if (response == null) {
                clientStatsInfo.bookReg2Broker(true);
                result.setFailResult(TErrCodeConstants.CONNECT_RETURN_NULL,
                        sBuffer.append(" register ").append(partition.toString())
                                .append(" return null!").toString());
                return result.isSuccess();
            }
            if (response.getSuccess()) {
                clientStatsInfo.bookReg2Broker(false);
                long currOffset = response.hasCurrOffset()
                        ? response.getCurrOffset() : TBaseConstants.META_VALUE_UNDEFINED;
                long maxOffset = response.hasMaxOffset()
                        ? response.getMaxOffset() : TBaseConstants.META_VALUE_UNDEFINED;
                clientRmtDataCache.addPartition(partition, currOffset, maxOffset);
                logger.info(sBuffer.append("Registered partition: consumer is ")
                        .append(consumerId).append(", partition=")
                        .append(partition.toString()).append(", boostrapOffset=")
                        .append(boostrapOffset).toString());
                sBuffer.delete(0, sBuffer.length());
                result.setSuccResult();
                return result.isSuccess();
            } else {
                clientStatsInfo.bookReg2Broker(true);
                if (response.getErrCode() == TErrCodeConstants.PARTITION_OCCUPIED
                        || response.getErrCode() == TErrCodeConstants.CERTIFICATE_FAILURE) {
                    clientRmtDataCache.removePartition(partition);
                    if (response.getErrCode() == TErrCodeConstants.PARTITION_OCCUPIED) {
                        result.setFailResult(response.getErrCode(),
                                sBuffer.append("[Partition occupied], curr consumerId: ")
                                        .append(consumerId).append(", returned message : ")
                                        .append(response.getErrMsg()).toString());
                    } else {
                        result.setFailResult(response.getErrCode(),
                                sBuffer.append("[Certificate failure], curr consumerId: ")
                                        .append(consumerId).append(", returned message : ")
                                        .append(response.getErrMsg()).toString());
                    }
                } else {
                    result.setFailResult(response.getErrCode(),
                            sBuffer.append(" register ").append(partition.toString())
                                    .append(" return ").append(response.getErrMsg()).toString());
                }
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        } catch (Throwable e) {
            sBuffer.delete(0, sBuffer.length());
            result.setFailResult(TErrCodeConstants.UNSPECIFIED_ABNORMAL,
                    sBuffer.append("register ").append(partition.toString())
                            .append(" throw exception ").append(e.toString()).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
    }

    private boolean tryRegister2Master(ProcessResult result, StringBuilder sBuffer) {
        try {
            ClientMaster.RegisterResponseM2CV2 response =
                    masterService.consumerRegisterC2MV2(createMasterRegisterRequest(),
                            AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
            if (response == null) {
                clientStatsInfo.bookReg2Master(true);
                result.setFailResult(TErrCodeConstants.CONNECT_RETURN_NULL,
                        sBuffer.append("Register Failed: ").append(consumerId)
                                .append(" register to master return null!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            if (response.getErrCode() != TErrCodeConstants.SUCCESS) {
                clientStatsInfo.bookReg2Master(true);
                // If the consumer group is forbidden, output the log
                if (response.getErrCode()
                        == TErrCodeConstants.CONSUME_GROUP_FORBIDDEN) {
                    result.setFailResult(response.getErrCode(),
                            sBuffer.append("Register Failed: ").append(consumerId)
                                    .append("'s ConsumeGroup forbidden, ")
                                    .append(response.getErrMsg()).toString());
                } else {
                    result.setFailResult(response.getErrCode(),
                            sBuffer.append("Register Failed: ").append(consumerId)
                                    .append(" ").append(response.getErrMsg()).toString());
                }
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            clientStatsInfo.bookReg2Master(false);
            // Process the successful response
            clientRmtDataCache.updateReg2MasterTime();
            clientRmtDataCache.updateBrokerInfoList(response.getBrokerConfigId(),
                    response.getBrokerConfigListList(), sBuffer);
            clientRmtDataCache.updOpsTaskInfo(response.getOpsTaskInfo());
            processRegAuthorizedToken(response);
            result.setSuccResult();
            return result.isSuccess();
        } catch (Throwable e) {
            result.setFailResult(sBuffer.append("Register Failed: register to master throw ")
                    .append(e.getCause()).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
    }

    // #lizard forgives
    private class HeartTask2BrokerWorker implements Runnable {
        @Override
        public void run() {
            StringBuilder strBuffer = new StringBuilder(512);
            while (!isShutdown()) {
                try {
                    // First check the last heartbeat interval. If it's larger than two periods,
                    // there may be some system hang up(e.g. long time gc, CPU is too busy).
                    // Print the warning message.
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastHeartbeatTime2Broker)
                            > (consumerConfig.getHeartbeatPeriodMs() * 2)) {
                        logger.warn(strBuffer.append(consumerId)
                                .append(" heartbeat to broker is too long, please check! Total time : ")
                                .append(currentTime - lastHeartbeatTime2Broker).toString());
                        strBuffer.delete(0, strBuffer.length());
                    }
                    // Send heartbeat request to the broker connect by the client
                    processBrokerHeatBeat(strBuffer);
                    if (needMetaSelfChk.compareAndSet(true, false)) {
                        // clear unsubscribable partitions
                        clearUnSubscribablePartitions();
                    }
                    if (clientRmtDataCache.isCsmFromMaxOffset()) {
                        resetCsmFromMaxOffset(strBuffer);
                    }
                    // Wait for next heartbeat
                    lastHeartbeatTime2Broker = System.currentTimeMillis();
                    Thread.sleep(consumerConfig.getHeartbeatPeriodMs());
                } catch (Throwable e) {
                    lastHeartbeatTime2Broker = System.currentTimeMillis();
                    if (!isShutdown()) {
                        logger.error("heartbeat thread error 3 : ", e);
                    }
                }
            }
        }

        private void resetCsmFromMaxOffset(StringBuilder sBuffer) {
            Set<String> regPartSet = clientRmtDataCache.getCurRegisteredPartSet();
            if (regPartSet.isEmpty()) {
                return;
            }
            Partition partition;
            long boostrapOffset;
            for (String partitionKey : regPartSet) {
                if (TStringUtils.isBlank(partitionKey)) {
                    continue;
                }
                if (isShutdown()) {
                    break;
                }
                boostrapOffset =
                        clientRmtDataCache.getMaxOffsetOfPartition(partitionKey);
                final String uniqueId =
                        sBuffer.append(consumerConfig.getConsumerGroup())
                                .append("#").append(partitionKey).toString();
                sBuffer.delete(0, sBuffer.length());
                synchronized (uniqueId) {
                    partition = clientRmtDataCache.getPartitionByKey(partitionKey);
                    if (partition == null) {
                        continue;
                    }
                    try {
                        ClientBroker.RegisterResponseB2C response =
                                getBrokerService(partition.getBroker()).consumerRegisterC2B(
                                        createBrokerRegisterRequest(partition, boostrapOffset),
                                        AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
                        if (response == null) {
                            continue;
                        }
                        if (response.getSuccess()) {
                            long currOffset = response.hasCurrOffset()
                                    ? response.getCurrOffset() : TBaseConstants.META_VALUE_UNDEFINED;
                            long maxOffset = response.hasMaxOffset()
                                    ? response.getMaxOffset() : TBaseConstants.META_VALUE_UNDEFINED;
                            clientRmtDataCache.updPartOffsetInfo(
                                    partitionKey, currOffset, maxOffset);
                            logger.info(sBuffer.append("[Admin Reset] consumer is ")
                                    .append(consumerId).append(", partition=")
                                    .append(partition.toString()).append(", consume from max=")
                                    .append(currOffset).toString());
                            sBuffer.delete(0, sBuffer.length());
                        } else {
                            if (response.getErrCode() == TErrCodeConstants.PARTITION_OCCUPIED
                                    || response.getErrCode() == TErrCodeConstants.CERTIFICATE_FAILURE) {
                                clientRmtDataCache.removePartition(partition);
                            }
                        }
                    } catch (Throwable e) {
                        sBuffer.delete(0, sBuffer.length());
                        logger.info(sBuffer.append("register ").append(partition.toString())
                                .append(" throw exception ").append(e.toString()).toString());
                        sBuffer.delete(0, sBuffer.length());
                    }
                }
            }
        }

        private void processBrokerHeatBeat(StringBuilder sBuffer) {
            List<String> partStrSet;
            List<Partition> partitions;
            List<String> strFailInfoList;
            for (BrokerInfo brokerInfo : clientRmtDataCache.getAllRegisterBrokers()) {
                if (isShutdown()) {
                    break;
                }
                partStrSet = new ArrayList<>();
                try {
                    // Handle the heartbeat response for partitions belong to the same broker.
                    partitions = clientRmtDataCache.getBrokerPartitionList(brokerInfo);
                    if ((partitions != null) && (!partitions.isEmpty())) {
                        for (Partition partition : partitions) {
                            partStrSet.add(partition.toString());
                        }
                        ClientBroker.HeartBeatResponseB2C response =
                                getBrokerService(brokerInfo).consumerHeartbeatC2B(
                                        createBrokerHeartBeatRequest(brokerInfo.getBrokerId(), partStrSet),
                                        AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
                        if (response == null) {
                            clientStatsInfo.bookHB2BrokerTimeout();
                            continue;
                        }
                        if (response.getSuccess()) {
                            // store require authenticate information
                            clientRmtDataCache.bookBrokerRequireAuthInfo(
                                    brokerInfo.getBrokerId(), response);
                            // If the heartbeat response report failed partitions, release the
                            // corresponding local partition and log the operation
                            if (response.getHasPartFailure()) {
                                try {
                                    strFailInfoList = response.getFailureInfoList();
                                    for (String strFailInfo : strFailInfoList) {
                                        final int index =
                                                strFailInfo.indexOf(TokenConstants.ATTR_SEP);
                                        if (index < 0) {
                                            logger.error(sBuffer
                                                    .append("Parse Heartbeat response error : ")
                                                    .append("invalid response, ")
                                                    .append(strFailInfo).toString());
                                            sBuffer.delete(0, sBuffer.length());
                                            continue;
                                        }
                                        int errorCode =
                                                Integer.parseInt(strFailInfo.substring(0, index));
                                        Partition failPartition =
                                                new Partition(strFailInfo.substring(index + 1));
                                        clientRmtDataCache.removePartition(failPartition);
                                        logger.warn(sBuffer
                                                .append("[heart2broker error] partition:")
                                                .append(failPartition.toString())
                                                .append(", errorCode=")
                                                .append(errorCode).toString());
                                        sBuffer.delete(0, sBuffer.length());
                                    }
                                } catch (Throwable ee) {
                                    if (!isShutdown()) {
                                        sBuffer.delete(0, sBuffer.length());
                                        logger.error(sBuffer
                                                .append("Parse Heartbeat response error :")
                                                .append(ee.getMessage()).toString());
                                        sBuffer.delete(0, sBuffer.length());
                                    }
                                }
                            }
                        } else {
                            clientStatsInfo.bookHB2BrokerException();
                            if (response.getErrCode() == TErrCodeConstants.CERTIFICATE_FAILURE) {
                                for (Partition partition : partitions) {
                                    clientRmtDataCache.removePartition(partition);
                                }
                                logger.warn(sBuffer
                                        .append("[heart2broker error] certificate failure, ")
                                        .append(brokerInfo.getBrokerStrInfo())
                                        .append("'s partitions area released, ")
                                        .append(response.getErrMsg()).toString());
                                sBuffer.delete(0, sBuffer.length());
                            }
                        }
                    }
                } catch (Throwable ee) {
                    // If there's error in the heartbeat, collect the log and print out.
                    // Release the log string buffer.
                    if (!isShutdown()) {
                        samplePrintCtrl.printExceptionCaught(ee);
                        if (!partStrSet.isEmpty()) {
                            sBuffer.delete(0, sBuffer.length());
                            for (String partitionStr : partStrSet) {
                                Partition tmpPartition = new Partition(partitionStr);
                                clientRmtDataCache.removePartition(tmpPartition);
                                logger.warn(sBuffer
                                        .append("[heart2broker Throwable] release partition:")
                                        .append(partitionStr).toString());
                                sBuffer.delete(0, sBuffer.length());
                            }
                        }
                    }
                }
            }
        }
    }

    private void unregisterPartition(Partition partition,
                                     boolean isLastConsumed, StringBuilder sBuffer) {
        try {
            getBrokerService(partition.getBroker())
                    .consumerRegisterC2B(createBrokerUnregisterRequest(partition,
                            isLastConsumed),
                            AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
            logger.info(sBuffer.append("Unregister partition: consumer is ")
                    .append(consumerId).append(", partition=")
                    .append(partition.toString()).append(", isLastPackConsumed=")
                    .append(isLastConsumed).toString());
        } catch (Throwable e) {
            logger.error(sBuffer.append("Disconnect to Broker error! broker:")
                    .append(partition.getBroker().toString()).toString(), e);
            sBuffer.delete(0, sBuffer.length());
        }
    }

    /**
     * Unregister partitions.
     *
     * @param unRegisterInfoMap partitions to be unregister
     */
    private void unregisterPartitions(
            Map<BrokerInfo, List<PartitionSelectResult>> unRegisterInfoMap) {
        StringBuilder strBuffer = new StringBuilder(512);
        strBuffer.append("Unregister info:");
        for (Map.Entry<BrokerInfo, List<PartitionSelectResult>> entry
                : unRegisterInfoMap.entrySet()) {
            for (PartitionSelectResult partResult : entry.getValue()) {
                try {
                    getBrokerService(partResult.getPartition().getBroker())
                            .consumerRegisterC2B(createBrokerUnregisterRequest(partResult.getPartition(),
                                    partResult.isLastPackConsumed()),
                                    AddressUtils.getLocalAddress(), consumerConfig.isTlsEnable());
                } catch (Throwable e) {
                    logger.error(new StringBuilder(512)
                            .append("Disconnect to Broker error! broker:")
                            .append(partResult.getPartition().getBroker().toString()).toString(), e);
                }
                strBuffer.append(partResult.getPartition().toString());
                strBuffer.append("\n");
            }
        }
        logger.info(strBuffer.toString());
    }

    private ClientMaster.RegisterRequestC2MV2 createMasterRegisterRequest() throws Exception {
        ClientMaster.RegisterRequestC2MV2.Builder builder =
                ClientMaster.RegisterRequestC2MV2.newBuilder();
        builder.setClientId(this.consumerId);
        builder.setHostName(AddressUtils.getLocalAddress());
        builder.setSourceCount(this.sourceCount);
        builder.setNodeId(this.nodeId);
        builder.setJdkVersion(MixedUtils.getJavaVersion());
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.addAllTopicList(this.consumeSubInfo.getSubscribedTopics());
        builder.addAllTopicCondition(formatTopicCondInfo(
                this.consumeSubInfo.getTopicCondRegistry()));
        builder.setSubRepInfo(this.clientRmtDataCache.buildClientSubRepInfo());
        ClientMaster.OpsTaskInfo opsTaskInfo =
                this.clientRmtDataCache.buildOpsTaskInfo();
        if (opsTaskInfo != null) {
            builder.setOpsTaskInfo(opsTaskInfo);
        }
        ClientMaster.MasterCertificateInfo authInfo = genMasterCertificateInfo(true);
        if (authInfo != null) {
            builder.setAuthInfo(authInfo);
        }
        return builder.build();
    }

    private ClientMaster.HeartRequestC2MV2 createMasterHeartBeatRequest() {
        ClientMaster.HeartRequestC2MV2.Builder builder =
                ClientMaster.HeartRequestC2MV2.newBuilder();
        builder.setClientId(consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setSubRepInfo(this.clientRmtDataCache.buildClientSubRepInfo());
        ClientMaster.OpsTaskInfo opsTaskInfo = clientRmtDataCache.buildOpsTaskInfo();
        if (opsTaskInfo != null) {
            builder.setOpsTaskInfo(opsTaskInfo);
        }
        ClientMaster.MasterCertificateInfo authInfo = genMasterCertificateInfo(false);
        if (authInfo != null) {
            builder.setAuthInfo(authInfo);
        }
        return builder.build();
    }

    private ClientMaster.GetPartMetaRequestC2M createMasterGetPartMetaRequest() {
        ClientMaster.GetPartMetaRequestC2M.Builder builder =
                ClientMaster.GetPartMetaRequestC2M.newBuilder();
        builder.setClientId(consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setBrokerConfigId(this.clientRmtDataCache.getLastBrokerConfigId());
        builder.setTopicMetaInfoId(this.clientRmtDataCache.getlastTopicMetaInfoId());
        ClientMaster.MasterCertificateInfo authInfo = genMasterCertificateInfo(false);
        if (authInfo != null) {
            builder.setAuthInfo(authInfo);
        }
        return builder.build();
    }

    private ClientMaster.CloseRequestC2M createMasterCloseRequest() {
        ClientMaster.CloseRequestC2M.Builder builder =
                ClientMaster.CloseRequestC2M.newBuilder();
        builder.setClientId(this.consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        ClientMaster.MasterCertificateInfo authInfo =
                genMasterCertificateInfo(false);
        if (authInfo != null) {
            builder.setAuthInfo(authInfo);
        }
        return builder.build();
    }

    private ClientBroker.RegisterRequestC2B createBrokerRegisterRequest(Partition partition,
                                                                        long boostrapOffset) {
        ClientBroker.RegisterRequestC2B.Builder builder =
                ClientBroker.RegisterRequestC2B.newBuilder();
        builder.setClientId(consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setOpType(RpcConstants.MSG_OPTYPE_REGISTER);
        builder.setTopicName(partition.getTopic());
        builder.setPartitionId(partition.getPartitionId());
        builder.setQryPriorityId(clientRmtDataCache.getQryPriorityId());
        builder.setReadStatus(getGroupInitReadStatus(
                clientRmtDataCache.bookPartition(partition.getPartitionKey())));
        TopicProcessor topicProcessor =
                this.consumeSubInfo.getTopicProcessor(partition.getTopic());
        if (topicProcessor != null && topicProcessor.getFilterConds() != null) {
            builder.addAllFilterCondStr(topicProcessor.getFilterConds());
        }
        if (boostrapOffset >= 0) {
            builder.setCurrOffset(boostrapOffset);
        }
        builder.setAuthInfo(genBrokerAuthenticInfo(partition.getBrokerId(), false));
        return builder.build();
    }

    private ClientBroker.RegisterRequestC2B createBrokerUnregisterRequest(Partition partition,
                                                                          boolean isLastConsumered) {
        ClientBroker.RegisterRequestC2B.Builder builder =
                ClientBroker.RegisterRequestC2B.newBuilder();
        builder.setClientId(consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setOpType(RpcConstants.MSG_OPTYPE_UNREGISTER);
        builder.setTopicName(partition.getTopic());
        builder.setPartitionId(partition.getPartitionId());
        if (isLastConsumered) {
            builder.setReadStatus(0);
        } else {
            builder.setReadStatus(1);
        }
        builder.setAuthInfo(genBrokerAuthenticInfo(partition.getBrokerId(), true));
        return builder.build();
    }

    private ClientBroker.HeartBeatRequestC2B createBrokerHeartBeatRequest(
            int brokerId, List<String> partitionList) {
        ClientBroker.HeartBeatRequestC2B.Builder builder =
                ClientBroker.HeartBeatRequestC2B.newBuilder();
        builder.setClientId(consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setReadStatus(getGroupInitReadStatus(false));
        builder.setQryPriorityId(clientRmtDataCache.getQryPriorityId());
        builder.addAllPartitionInfo(partitionList);
        builder.setAuthInfo(genBrokerAuthenticInfo(brokerId, false));
        return builder.build();
    }

    /**
     * Construct a get message request.
     *
     * @param partition      message partition
     * @param isLastConsumed if the last package consumed
     * @return message request
     */
    protected ClientBroker.GetMessageRequestC2B createBrokerGetMessageRequest(
            Partition partition, boolean isLastConsumed) {
        ClientBroker.GetMessageRequestC2B.Builder builder =
                ClientBroker.GetMessageRequestC2B.newBuilder();
        builder.setClientId(this.consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setTopicName(partition.getTopic());
        builder.setEscFlowCtrl(clientRmtDataCache.isCurGroupInFlowCtrl());
        builder.setPartitionId(partition.getPartitionId());
        builder.setLastPackConsumed(isLastConsumed);
        builder.setManualCommitOffset(false);
        return builder.build();
    }

    /**
     * Create a commit request.
     *
     * @param partition  partition to be commit
     * @param isConsumed if the last package consumed
     * @return commit request
     */
    protected ClientBroker.CommitOffsetRequestC2B createBrokerCommitRequest(
            Partition partition, boolean isConsumed) {
        ClientBroker.CommitOffsetRequestC2B.Builder builder =
                ClientBroker.CommitOffsetRequestC2B.newBuilder();
        builder.setClientId(this.consumerId);
        builder.setGroupName(this.consumerConfig.getConsumerGroup());
        builder.setTopicName(partition.getTopic());
        builder.setPartitionId(partition.getPartitionId());
        builder.setLastPackConsumed(isConsumed);
        return builder.build();
    }

    private List<String> formatTopicCondInfo(
            final ConcurrentHashMap<String, TopicProcessor> topicCondMap) {
        final StringBuilder strBuffer = new StringBuilder(512);
        List<String> strTopicCondList = new ArrayList<>();
        if ((topicCondMap != null) && (!topicCondMap.isEmpty())) {
            for (Map.Entry<String, TopicProcessor> entry : topicCondMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                Set<String> condSet = entry.getValue().getFilterConds();
                if (condSet != null && !condSet.isEmpty()) {
                    int i = 0;
                    strBuffer.append(entry.getKey()).append(TokenConstants.SEGMENT_SEP);
                    for (String condStr : condSet) {
                        if (i++ > 0) {
                            strBuffer.append(TokenConstants.ARRAY_SEP);
                        }
                        strBuffer.append(condStr);
                    }
                    strTopicCondList.add(strBuffer.toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
        }
        return strTopicCondList;
    }

    private void clearUnSubscribablePartitions() throws Exception {
        ProcessResult tmpResult = new ProcessResult();
        Set<String> regPartSet = clientRmtDataCache.getCurRegisteredPartSet();
        for (String partKey : regPartSet) {
            if (!clientRmtDataCache.isPartSubscribable(partKey)) {
                if (!disconnectFromPartition(partKey, tmpResult)
                        && tmpResult.getErrCode() == TErrCodeConstants.CLIENT_SHUTDOWN) {
                    break;
                }
            }
        }
    }

    private ClientBroker.AuthorizedInfo genBrokerAuthenticInfo(int brokerId, boolean force) {
        ClientBroker.AuthorizedInfo.Builder authInfoBuilder =
                ClientBroker.AuthorizedInfo.newBuilder();
        authInfoBuilder.setVisitAuthorizedToken(visitToken.get());
        if (this.consumerConfig.isEnableUserAuthentic()) {
            if (clientRmtDataCache.markAndGetBrokerAuthStatus(brokerId, force)) {
                authInfoBuilder.setAuthAuthorizedToken(authenticateHandler
                        .genBrokerAuthenticateToken(consumerConfig.getUsrName(),
                                consumerConfig.getUsrPassWord()));
            }
        }
        return authInfoBuilder.build();
    }

    private ClientMaster.MasterCertificateInfo genMasterCertificateInfo(boolean force) {
        ClientMaster.MasterCertificateInfo.Builder authInfoBuilder = null;
        if (this.consumerConfig.isEnableUserAuthentic()) {
            authInfoBuilder = ClientMaster.MasterCertificateInfo.newBuilder();
            if (clientRmtDataCache.markAndGetAuthStatus(force)) {
                authInfoBuilder.setAuthInfo(authenticateHandler
                        .genMasterAuthenticateToken(consumerConfig.getUsrName(),
                                consumerConfig.getUsrPassWord()));
            } else {
                authInfoBuilder.setAuthorizedToken(authAuthorizedTokenRef.get());
            }
        }
        if (authInfoBuilder != null) {
            return authInfoBuilder.build();
        }
        return null;
    }

    private void processRegAuthorizedToken(ClientMaster.RegisterResponseM2CV2 response) {
        if (response.hasAuthorizedInfo()) {
            processAuthorizedToken(response.getAuthorizedInfo());
        }
    }

    private void processHeartBeatAuthorizedToken(ClientMaster.HeartResponseM2CV2 response) {
        if (response.hasAuthorizedInfo()) {
            processAuthorizedToken(response.getAuthorizedInfo());
        }
    }

    private void processAuthorizedToken(ClientMaster.MasterAuthorizedInfo inAuthorizedTokenInfo) {
        if (inAuthorizedTokenInfo != null) {
            visitToken.set(inAuthorizedTokenInfo.getVisitAuthorizedToken());
            if (inAuthorizedTokenInfo.hasAuthAuthorizedToken()) {
                String inAuthAuthorizedToken = inAuthorizedTokenInfo.getAuthAuthorizedToken();
                if (TStringUtils.isNotBlank(inAuthAuthorizedToken)) {
                    String curAuthAuthorizedToken = authAuthorizedTokenRef.get();
                    if (!inAuthAuthorizedToken.equals(curAuthAuthorizedToken)) {
                        authAuthorizedTokenRef.set(inAuthAuthorizedToken);
                    }
                }
            }
        }
    }

    private int getGroupInitReadStatus(boolean isFistReg) {
        int readStatus = TBaseConstants.CONSUME_MODEL_READ_NORMAL;
        switch (consumerConfig.getConsumePosition()) {
            case CONSUMER_FROM_LATEST_OFFSET: {
                if (isFistReg) {
                    readStatus = TBaseConstants.CONSUME_MODEL_READ_FROM_MAX;
                    logger.info("[Consume From Max Offset]" + consumerId);
                }
                break;
            }
            case CONSUMER_FROM_MAX_OFFSET_ALWAYS: {
                if (isFistReg) {
                    readStatus = TBaseConstants.CONSUME_MODEL_READ_FROM_MAX_ALWAYS;
                    logger.info("[Consume From Max Offset Always]" + consumerId);
                }
                break;
            }
            default: {
                readStatus = TBaseConstants.CONSUME_MODEL_READ_NORMAL;
            }
        }
        return readStatus;
    }

    /**
     * Get the broker read service.
     *
     * @param brokerInfo broker information
     * @return broker read service
     */
    protected BrokerReadService getBrokerService(BrokerInfo brokerInfo) {
        return rpcServiceFactory.getService(BrokerReadService.class, brokerInfo, rpcConfig);
    }

    /**
     * Generate consumer id.
     *
     * @return consumer id
     * @throws Exception
     */
    private String generateConsumerID() throws Exception {
        String pidName = ManagementFactory.getRuntimeMXBean().getName();
        if (pidName != null && pidName.contains("@")) {
            pidName = pidName.split("@")[0];
        }
        return new StringBuilder(256)
                .append(this.consumerConfig.getConsumerGroup())
                .append("_").append(AddressUtils.getLocalAddress())
                .append("-").append(pidName)
                .append("-").append(System.currentTimeMillis())
                .append("-").append(consumerCounter.incrementAndGet())
                .append("-Balance-")
                .append(TubeClientVersion.CONSUMER_VERSION).toString();
    }
}
