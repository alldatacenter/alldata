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

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.InnerSessionFactory;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;

/**
 * An implementation of PullMessageConsumer
 */
public class SimplePullMessageConsumer implements PullMessageConsumer {

    private final BaseMessageConsumer baseConsumer;

    public SimplePullMessageConsumer(final InnerSessionFactory messageSessionFactory,
                                     final ConsumerConfig consumerConfig) throws TubeClientException {
        baseConsumer =
                new BaseMessageConsumer(messageSessionFactory, consumerConfig, true);
    }

    @Override
    public boolean isPartitionsReady(long maxWaitTime) {
        return baseConsumer.rmtDataCache.isPartitionsReady(maxWaitTime);
    }

    @Override
    public void shutdown() throws Throwable {
        baseConsumer.shutdown();
    }

    @Override
    public String getClientVersion() {
        return baseConsumer.getClientVersion();
    }

    @Override
    public String getConsumerId() {
        return baseConsumer.getConsumerId();
    }

    @Override
    public boolean isShutdown() {
        return baseConsumer.isShutdown();
    }

    @Override
    public ConsumerConfig getConsumerConfig() {
        return baseConsumer.getConsumerConfig();
    }

    @Override
    public boolean isFilterConsume(String topic) {
        return baseConsumer.isFilterConsume(topic);
    }

    @Override
    public Map<String, ConsumeOffsetInfo> getCurConsumedPartitions() throws TubeClientException {
        return baseConsumer.getCurConsumedPartitions();
    }

    @Override
    public void freezePartitions(List<String> partitionKeys) throws TubeClientException {
        baseConsumer.freezePartitions(partitionKeys);
    }

    @Override
    public void unfreezePartitions(List<String> partitionKeys) throws TubeClientException {
        baseConsumer.unfreezePartitions(partitionKeys);
    }

    @Override
    public void relAllFrozenPartitions() {
        this.baseConsumer.relAllFrozenPartitions();
    }

    @Override
    public Map<String, Long> getFrozenPartInfo() {
        return baseConsumer.getFrozenPartInfo();
    }

    @Override
    public PullMessageConsumer subscribe(String topic,
                                         TreeSet<String> filterConds) throws TubeClientException {
        baseConsumer.subscribe(topic, filterConds, null);
        return this;
    }

    @Override
    public void completeSubscribe() throws TubeClientException {
        baseConsumer.completeSubscribe();
    }

    @Override
    public void completeSubscribe(final String sessionKey,
                                  final int sourceCount,
                                  final boolean isSelectBig,
                                  final Map<String, Long> partOffsetMap) throws TubeClientException {
        baseConsumer.completeSubscribe(sessionKey, sourceCount, isSelectBig, partOffsetMap);
    }

    @Override
    public ConsumerResult getMessage() throws TubeClientException {
        baseConsumer.checkClientRunning();
        if (!baseConsumer.isSubscribed()) {
            throw new TubeClientException("Please complete topic's Subscribe call first!");
        }
        PartitionSelectResult selectResult = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            if (baseConsumer.isShutdown()) {
                return new ConsumerResult(TErrCodeConstants.BAD_REQUEST,
                        "Client instance has been shutdown!");
            }
            selectResult = baseConsumer.rmtDataCache.getCurrPartsStatus();
            if (selectResult.isSuccess()) {
                break;
            }
            if ((baseConsumer.getConsumerConfig().getPullConsumeReadyWaitPeriodMs() >= 0L)
                && (System.currentTimeMillis() - startTime
                    >= baseConsumer.getConsumerConfig().getPullConsumeReadyWaitPeriodMs())) {
                return new ConsumerResult(selectResult.getErrCode(), selectResult.getErrMsg());
            }
            if (baseConsumer.getConsumerConfig().getPullConsumeReadyChkSliceMs() > 0L) {
                ThreadUtils.sleep(baseConsumer.getConsumerConfig().getPullConsumeReadyChkSliceMs());
            }
        }
        StringBuilder sBuilder = new StringBuilder(512);
        // Check the data cache first
        selectResult = baseConsumer.rmtDataCache.pullSelect();
        if (!selectResult.isSuccess()) {
            return new ConsumerResult(selectResult.getErrCode(), selectResult.getErrMsg());
        }
        FetchContext taskContext = baseConsumer.fetchMessage(selectResult, sBuilder);
        return new ConsumerResult(taskContext);
    }

    @Override
    public ConsumerResult confirmConsume(final String confirmContext,
                                         boolean isConsumed) throws TubeClientException {
        baseConsumer.checkClientRunning();
        if (!baseConsumer.isSubscribed()) {
            throw new TubeClientException("Please complete topic's Subscribe call first!");
        }
        StringBuilder sBuilder = new StringBuilder(512);
        long currOffset = TBaseConstants.META_VALUE_UNDEFINED;
        long maxOffset = TBaseConstants.META_VALUE_UNDEFINED;
        // Verify if the confirmContext is valid
        if (TStringUtils.isBlank(confirmContext)) {
            throw new TubeClientException("ConfirmContext is null !");
        }
        String[] strConfirmContextItems =
                confirmContext.split(TokenConstants.ATTR_SEP);
        if (strConfirmContextItems.length != 4) {
            throw new TubeClientException(
                    "ConfirmContext format error: value must be aaaa:bbbb:cccc:ddddd !");
        }
        for (String itemStr : strConfirmContextItems) {
            if (TStringUtils.isBlank(itemStr)) {
                throw new TubeClientException(sBuilder
                        .append("ConfirmContext's format error: item (")
                        .append(itemStr).append(") is null !").toString());
            }
        }
        String keyId = sBuilder.append(strConfirmContextItems[0].trim())
                .append(TokenConstants.ATTR_SEP).append(strConfirmContextItems[1].trim())
                .append(TokenConstants.ATTR_SEP).append(strConfirmContextItems[2].trim()).toString();
        sBuilder.delete(0, sBuilder.length());
        String topicName = strConfirmContextItems[1].trim();
        long timeStamp = Long.parseLong(strConfirmContextItems[3]);
        if (!baseConsumer.rmtDataCache.isPartitionInUse(keyId, timeStamp)) {
            return new ConsumerResult(TErrCodeConstants.BAD_REQUEST,
                    "The confirmContext's value invalid!");
        }
        Partition curPartition =
                baseConsumer.rmtDataCache.getPartitionByKey(keyId);
        if (curPartition == null) {
            return new ConsumerResult(TErrCodeConstants.NOT_FOUND, sBuilder
                    .append("Not found the partition by confirmContext:")
                    .append(confirmContext).toString());
        }
        long midTime = System.currentTimeMillis();
        baseConsumer.clientStatsInfo.bookReturnDuration(keyId, midTime - timeStamp);
        if (this.baseConsumer.consumerConfig.isPullConfirmInLocal()) {
            baseConsumer.rmtDataCache.succRspRelease(keyId, topicName,
                timeStamp, isConsumed, isFilterConsume(topicName), currOffset, maxOffset);
            return new ConsumerResult(true, TErrCodeConstants.SUCCESS,
                    "OK!", topicName, curPartition, currOffset, maxOffset);
        } else {
            try {
                ClientBroker.CommitOffsetResponseB2C commitResponse =
                    baseConsumer.getBrokerService(curPartition.getBroker())
                        .consumerCommitC2B(baseConsumer.createBrokerCommitRequest(curPartition, isConsumed),
                            AddressUtils.getLocalAddress(), getConsumerConfig().isTlsEnable());
                if (commitResponse == null) {
                    return new ConsumerResult(TErrCodeConstants.BAD_REQUEST,
                            sBuilder.append("Confirm ").append(confirmContext)
                                    .append("'s offset failed!").toString());
                } else {
                    if (commitResponse.hasCurrOffset() && commitResponse.getCurrOffset() >= 0) {
                        currOffset = commitResponse.getCurrOffset();
                    }
                    if (commitResponse.hasMaxOffset() && commitResponse.getMaxOffset() >= 0) {
                        maxOffset = commitResponse.getMaxOffset();
                    }
                    return new ConsumerResult(commitResponse.getSuccess(),
                            commitResponse.getErrCode(), commitResponse.getErrMsg(),
                            topicName, curPartition, currOffset, maxOffset);
                }
            } catch (Throwable e) {
                sBuilder.delete(0, sBuilder.length());
                throw new TubeClientException(sBuilder.append("Confirm ")
                        .append(confirmContext).append("'s offset failed.").toString(), e);
            } finally {
                baseConsumer.rmtDataCache.succRspRelease(keyId, topicName,
                    timeStamp, isConsumed, isFilterConsume(topicName), currOffset, maxOffset);
                baseConsumer.clientStatsInfo.bookConfirmDuration(keyId,
                        System.currentTimeMillis() - midTime);
            }
        }
    }
}
