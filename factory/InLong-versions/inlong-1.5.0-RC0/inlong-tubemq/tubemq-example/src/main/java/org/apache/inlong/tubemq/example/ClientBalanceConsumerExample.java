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

package org.apache.inlong.tubemq.example;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.inlong.tubemq.client.common.ConfirmResult;
import org.apache.inlong.tubemq.client.common.ConsumeResult;
import org.apache.inlong.tubemq.client.common.QueryMetaResult;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ClientBalanceConsumer;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This demo shows how to consume message by client balance consumer.
 *
 * Consume message in client balance mode achieved by
 * {@link ClientBalanceConsumer#getMessage(ConsumeResult)}.
 * Note that whenever {@link ClientBalanceConsumer#getMessage(ConsumeResult)}
 * returns successfully, the return value(whether or not to be {@code null})
 * should be processed by {@link ClientBalanceConsumer#confirmConsume(
 * String, boolean, ConfirmResult)}.
 */
public final class ClientBalanceConsumerExample {

    private static final Logger logger =
            LoggerFactory.getLogger(ClientBalanceConsumerExample.class);

    private static final MsgSendReceiveStats msgRcvStats =
            new MsgSendReceiveStats(false);
    private static ClientBalanceConsumer consumer;
    private static MessageSessionFactory messageSessionFactory;
    // 0. Map of partitionKey and last success offset
    // You can persist the information and use it when restarting or
    // re-rolling to keep the current consumption to start from
    // the offset required in the last round
    private static final ConcurrentHashMap<String, Long> partitionOffsetMap =
            new ConcurrentHashMap<>();

    /**
     * Consume messages in Pull mode by assigning partitions by the client.
     *
     * @param args   Startup parameter array, including the following parts:
     *               The 1st parameter masterServers is the master address(es) to connect to,
     *                       format is master1_ip:port[,master2_ip:port];
     *               The 2nd parameter subTopicAndFiterItems the topic(s) (and filter condition set) to consume on,
     *                       format is topic_1[[:filterCond_1.1[;filterCond_1.2]][,topic_2]];
     *               The 3rd parameter groupName is the name of consumer group;
     *               The 4th parameter consumeCount is the amount of messages that need to be consumed;
     *               The 5th  parameter totalGroupNodeCnt is the total number of clients started by the consumer group;
     *               The 6th  parameter fetchThreadCnt is the count of fetch thread.
     */
    public static void main(String[] args) throws Throwable {
        // 1. get and initial parameters
        final String masterServers = args[0];
        final String topics = args[1];
        final String group = args[2];
        final int msgCount = Integer.parseInt(args[3]);
        final int totalGroupNodeCnt =
                (args.length > 4) ? Integer.parseInt(args[4]) : 1;
        final int nodeIndexId =
                (args.length > 5) ? Integer.parseInt(args[5]) : 0;

        // 2. initial consumer object
        ConsumerConfig consumerConfig = new ConsumerConfig(masterServers, group);
        consumerConfig.setConsumePosition(ConsumePosition.CONSUMER_FROM_LATEST_OFFSET);
        messageSessionFactory = new TubeSingleSessionFactory(consumerConfig);
        consumer = messageSessionFactory.createBalanceConsumer(consumerConfig);
        final Map<String, TreeSet<String>> topicAndFiltersMap =
                MixedUtils.parseTopicParam(topics);
        final long metaInfoFetchInterval =
                consumer.getConsumerConfig().getPartMetaInfoCheckPeriodMs();
        // 3. start consumer
        ProcessResult procResult = new ProcessResult();
        if (!consumer.start(topicAndFiltersMap, totalGroupNodeCnt, nodeIndexId, procResult)) {
            logger.info("Initial balance consumer failure, errcode is {}, errMsg is {}",
                    procResult.getErrCode(), procResult.getErrMsg());
            return;
        }

        // 4. initial partition assign thread
        Thread metaInfoUpdater = new Thread(new Runnable() {

            @Override
            public void run() {
                QueryMetaResult qryResult = new QueryMetaResult();
                ProcessResult procResult = new ProcessResult();
                do {
                    try {
                        // 4.1 judge consumer is shutdown
                        if (consumer.isShutdown()) {
                            logger.info("Consumer is shutdown!");
                            break;
                        }
                        // 4.2 get partition meta info
                        if (!consumer.getPartitionMetaInfo(qryResult)) {
                            // 4.2.1 judge consumer is shutdown
                            if (qryResult.getErrCode() == TErrCodeConstants.CLIENT_SHUTDOWN) {
                                logger.info("Consumer is shutdown!");
                                break;
                            }
                        } else {
                            // 4.2.2.1 get latest partition meta info
                            Map<String, Boolean> partMetaInfoMap = qryResult.getPartStatusMap();
                            if (partMetaInfoMap != null && !partMetaInfoMap.isEmpty()) {
                                // 4.2.2.2 assign partitions to current node
                                // by totalGroupNodeCnt and nodeIndexId parameters
                                Set<String> configuredTopicPartitions = partMetaInfoMap.keySet();
                                Set<String> assignedPartitions =
                                        configuredTopicPartitions.stream()
                                                .filter(p -> (((((long) p.hashCode()) & 0xffffffffL)
                                                        % consumer.getSourceCount()) == consumer.getNodeId()))
                                                .collect(Collectors.toCollection(TreeSet::new));
                                Set<String> rsvRegisteredPartSet = new TreeSet<>();
                                // 4.2.2.3 get current registered partition set
                                Set<String> curRegisteredPartSet =
                                        consumer.getCurRegisteredPartSet();
                                // 4.2.2.4 remove unassigned or unsubscribable partitions
                                for (String partKey : curRegisteredPartSet) {
                                    if (!assignedPartitions.contains(partKey)
                                            || partMetaInfoMap.get(partKey) == Boolean.FALSE) {
                                        if (!consumer.disconnectFromPartition(partKey, procResult)
                                                && procResult.getErrCode() == TErrCodeConstants.CLIENT_SHUTDOWN) {
                                            logger.info("Consumer is shutdown!");
                                            break;
                                        }
                                        logger.info("Unregister " + partKey
                                                + ", process result is " + procResult.isSuccess()
                                                + ", err info is " + procResult.getErrMsg());
                                    } else {
                                        rsvRegisteredPartSet.add(partKey);
                                    }
                                }
                                // 4.2.2.5 add assigned and subscribable partitions
                                for (String partKey : assignedPartitions) {
                                    if (!rsvRegisteredPartSet.contains(partKey)
                                            && partMetaInfoMap.get(partKey) == Boolean.TRUE) {
                                        // Note: if you do not need to reset the boostrap
                                        // consumption offset value, please set it to
                                        // a negative number
                                        Long boostrapOffset = partitionOffsetMap.get(partKey);
                                        if (!consumer.connect2Partition(partKey,
                                                boostrapOffset == null ? -1L : boostrapOffset,
                                                procResult)) {
                                            // 4.2.2.5.1 if client shutdown, the thread need exit!
                                            if (procResult.getErrCode() == TErrCodeConstants.CLIENT_SHUTDOWN) {
                                                logger.info("Consumer is shutdown!");
                                                break;
                                            }
                                        }
                                        logger.info("Register " + partKey
                                                + ", process result is " + procResult.isSuccess()
                                                + ", err info is " + procResult.getErrMsg());
                                    }
                                }
                            }
                        }
                        // 4.3 wait next assign interval
                        ThreadUtils.sleep(metaInfoFetchInterval);
                    } catch (Throwable e) {
                        logger.error("Consume messages failed!", e);
                    }
                } while (true);
                logger.info("Consumer existed client balance thread!");
            }
        }, "partition_assigner");

        // 5. initial fetch threads
        Thread[] fetchRunners = new Thread[3];
        for (int i = 0; i < fetchRunners.length; i++) {
            fetchRunners[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        int getCount = msgCount;
                        ConsumeResult csmResult = new ConsumeResult();
                        ConfirmResult cfmResult = new ConfirmResult();
                        // wait partition status ready
                        do {
                            if (consumer.isPartitionsReady(5000)
                                    || consumer.isShutdown()) {
                                break;
                            }
                        } while (true);
                        // consume messages
                        do {
                            // 5.1 judge consumer is shutdown
                            if (consumer.isShutdown()) {
                                logger.info("Consumer is shutdown!");
                                break;
                            }
                            // 5.2 get messages
                            if (consumer.getMessage(csmResult)) {
                                // 5.2.1.1 process messages if success
                                List<Message> messageList = csmResult.getMessageList();
                                if (messageList != null && !messageList.isEmpty()) {
                                    msgRcvStats.addMsgCount(
                                            csmResult.getTopicName(), messageList.size());
                                }
                                // 5.2.1.2 store current offset
                                partitionOffsetMap.put(
                                        csmResult.getPartitionKey(), csmResult.getCurrOffset());
                                // 5.2.1.3 confirm messages to server
                                if (consumer.confirmConsume(
                                        csmResult.getConfirmContext(), true, cfmResult)) {
                                    // store confirmed offset
                                    partitionOffsetMap.put(
                                            cfmResult.getPartitionKey(), cfmResult.getCurrOffset());
                                }
                            } else {
                                // 5.2.2.1 print unexpected error
                                if (csmResult.getErrCode() == TErrCodeConstants.CLIENT_SHUTDOWN) {
                                    logger.info("Found that the client has shutdown, exit!");
                                }
                            }
                            // judge reached required message count
                            if (msgCount > 0) {
                                if (--getCount <= 0) {
                                    break;
                                }
                            }
                        } while (true);
                    } catch (Throwable e) {
                        logger.error("Consume messages failed!", e);
                    }
                    msgRcvStats.stopStats();
                    logger.info("Fetch runner exit!");
                }
            }, "_fetch_runner_" + i);
        }

        // 6. start threads
        // 6.1 start partition assign thread
        metaInfoUpdater.start();
        // 6.2 start fetch threads
        for (Thread thread : fetchRunners) {
            thread.start();
        }
        // 6.3 initial statistic thread
        Thread statisticThread =
                new Thread(msgRcvStats, "Receive Statistic Thread");
        statisticThread.start();
    }
}
