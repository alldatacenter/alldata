/**
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

package org.apache.inlong.tubemq.example;

import static org.apache.inlong.tubemq.corebase.TErrCodeConstants.IGNORE_ERROR_SET;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This demo shows how to consume messages through TubeSingleSessionFactory + PullMessageConsumer
 *
 *Consume message in pull mode achieved by {@link PullMessageConsumer#getMessage()}.
 * Note that whenever {@link PullMessageConsumer#getMessage()} returns successfully, the
 * return value(whether or not to be {@code null}) must be processed by
 * {@link PullMessageConsumer#confirmConsume(String, boolean)}.
 */
public final class MessagePullConsumerExample {

    private static final Logger logger =
            LoggerFactory.getLogger(MessagePullConsumerExample.class);

    private static final MsgSendReceiveStats msgRcvStats =
            new MsgSendReceiveStats(false);
    private static PullMessageConsumer pullConsumer;
    private static MessageSessionFactory sessionFactory;

    /**
     * Consume messages in Pull mode through a single-session factory instance.
     *
     * @param args   Startup parameter array, including the following parts:
     *               The 1st parameter masterServers is the master address(es) to connect to,
     *                       format is master1_ip:port[,master2_ip:port];
     *               The 2nd parameter subTopicAndFiterItems the topic(s) (and filter condition set) to consume on,
     *                       format is topic_1[[:filterCond_1.1[;filterCond_1.2]][,topic_2]];
     *               The 3rd parameter groupName is the name of consumer group;
     *               The 4th parameter consumeCount is the amount of messages that need to be consumed;
     *               The 5th  parameter fetchThreadCnt is the count of fetch thread.
     */
    public static void main(String[] args) throws Throwable {
        // 1. get and initial parameters
        final String masterServers = args[0];
        final String subTopicAndFiterItems = args[1];
        final String groupName = args[2];
        final int consumeCount = Integer.parseInt(args[3]);
        int fetchThreadCnt = 3;
        if (args.length > 4) {
            fetchThreadCnt = MixedUtils.mid(Integer.parseInt(args[4]),
                    1, Runtime.getRuntime().availableProcessors());
        }
        final Map<String, TreeSet<String>> topicAndFiltersMap =
                MixedUtils.parseTopicParam(subTopicAndFiterItems);

        // 2. initial configure and build session factory object
        ConsumerConfig consumerConfig =
                new ConsumerConfig(masterServers, groupName);
        // 2.1 set consume from latest position if the consumer group is first consume
        consumerConfig.setConsumePosition(ConsumePosition.CONSUMER_FROM_LATEST_OFFSET);
        // 2.2 build session factory object
        //     Attention: here we are using the TubeSingleSessionFactory object(a
        //                singleton session factory can only create one object in a process,
        //                requiring all topics to be in one cluster.
        //                If the topics subscribed to by the objects in the process are
        //                in different clusters, then need to use the TubeMultiSessionFactory class,
        //                please refer to the example of TubeMultiSessionFactory usage)
        sessionFactory = new TubeSingleSessionFactory(consumerConfig);

        // 3 build consumer object
        //    we can construct multiple consumers after the creation of the session factory object
        pullConsumer = sessionFactory.createPullConsumer(consumerConfig);
        // 3.1 Set the Topic and the filter item set corresponding to the consumption
        //     if you not need filter consumption,
        //    set the parameter filterConds is null or empty set
        for (Map.Entry<String, TreeSet<String>> entry : topicAndFiltersMap.entrySet()) {
            pullConsumer.subscribe(entry.getKey(), entry.getValue());
        }
        // 3.2 start consumption
        pullConsumer.completeSubscribe();

        // 4. initial fetch threads
        Thread[] fetchRunners = new Thread[fetchThreadCnt];
        for (int i = 0; i < fetchRunners.length; i++) {
            fetchRunners[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    ConsumerResult csmResult;
                    int getCount = consumeCount;
                    // wait partition status ready
                    do {
                        if (pullConsumer.isPartitionsReady(5000)
                                || pullConsumer.isShutdown()) {
                            break;
                        }
                    } while (true);
                    // consume messages
                    do {
                        try {
                            // 4.1 judge consumer is shutdown
                            if (pullConsumer.isShutdown()) {
                                logger.warn("Consumer is shutdown!");
                                break;
                            }
                            // 4.2 get messages from server
                            csmResult = pullConsumer.getMessage();
                            if (csmResult.isSuccess()) {
                                // 4.2.1 process message if getMessage() return success
                                List<Message> messageList = csmResult.getMessageList();
                                if (messageList != null && !messageList.isEmpty()) {
                                    msgRcvStats.addMsgCount(csmResult.getTopicName(), messageList.size());
                                }
                                // 4.2.1.1 confirm consume result
                                // Notice:
                                //    1. If the processing fails, the parameter isConsumed can
                                //       be set to false, but this is likely to cause
                                //       an infinite loop of data consumption, so
                                //       it is strongly recommended to set this parameter
                                //       to true when using it, and the failed data can
                                //       be processed in other ways
                                //    2. The messageList returned by getMessage() may be empty,
                                //       and confirmConsume() is still required call in this case
                                pullConsumer.confirmConsume(csmResult.getConfirmContext(), true);
                            } else {
                                // 4.2.2 process failure when getMessage() return false
                                //       Any failure can be ignored
                                if (!IGNORE_ERROR_SET.contains(csmResult.getErrCode())) {
                                    logger.debug(
                                            "Receive messages errorCode is {}, Error message is {}",
                                            csmResult.getErrCode(), csmResult.getErrMsg());
                                }
                            }
                            // 4.3 Determine whether the consumed data reaches the goal
                            if (consumeCount > 0) {
                                if (--getCount <= 0) {
                                    logger.info("Consumer has consumed {} messages!", consumeCount);
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            // Any exceptions in running can be ignored
                        }
                    } while (true);
                    logger.info("The fetch thread has exited!");
                }
            }, "_fetch_runner_" + i);
        }

        // 5. start fetch threads
        for (Thread thread : fetchRunners) {
            thread.start();
        }

        // 6. initial and statistic thread
        Thread statisticThread =
                new Thread(msgRcvStats, "Receive Statistic Thread");
        statisticThread.start();

        // 7. Resource cleanup when exiting the service
        //
        // 7.1 shutdown consumers
        // pullConsumer.shutdown();
        // 7.2 shutdown session factory
        // sessionFactory.shutdown();
        // 7.3 shutdown statistic thread
        // msgRecvStats.stopStats();
    }
}

