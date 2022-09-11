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

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This demo shows how to consume messages with bootstrap offset through
 * TubeMultiSessionFactory + PullMessageConsumer
 *
 * First, we start a consumer for data consumption, and then we exit it, start another consumer
 * with the same group name, and set the partition's bootstrap offset to 0 for data consumption.
 *
 * The main difference from {@link MessagePullConsumerExample}
 * is that we call {@link PullMessageConsumer#completeSubscribe(String, int, boolean, Map)} instead of
 * {@link PullMessageConsumer#completeSubscribe()}. The former supports multiple options to configure
 * when to reset offset.
 */
public final class MessagePullSetConsumerExample {

    private static final Logger logger =
            LoggerFactory.getLogger(MessagePullSetConsumerExample.class);
    // Statistic object
    private static final MsgSendReceiveStats msgRcvStats =
            new MsgSendReceiveStats(false);
    // The map of the master cluster and Multiple session factory
    //    There may be multiple consumers in the same process and the topic sets subscribed
    //    by different consumers are in different clusters. In this case,
    //    we need to construct session factories by TubeMultiSessionFactory class.
    private static final ConcurrentHashMap<String, MessageSessionFactory> multSessFtyMap =
            new ConcurrentHashMap<>();

    /**
     * Consume messages with setting bootstrap offset in Pull mode through multi-session factory instances.
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

        // 2. initial and statistic thread
        Thread statisticThread =
                new Thread(msgRcvStats, "Receive Statistic Thread");
        statisticThread.start();

        // 3. Start the consumer group for the first consumption
        // 3.1. build consumer object
        logger.info("The first consumption begin!");
        PullMessageConsumer consumer1 =
                createPullConsumer(masterServers, groupName);
        // 3.2. Set the Topic and the filter item set corresponding to the consumption
        //     if you not need filter consumption,
        //    set the parameter filterConds is null or empty set
        for (Map.Entry<String, TreeSet<String>> entry : topicAndFiltersMap.entrySet()) {
            consumer1.subscribe(entry.getKey(), entry.getValue());
        }
        // 3.3. start consumption with
        String sessionKeyFst = "test_consume_first";
        int sourceCountFst = 1;
        boolean isSelectBig = false;
        // The map of partitionKey and last success offset
        //    You can persist the information and use it when restarting or
        //    re-rolling to keep the current consumption to start from
        //    the offset required in the last round
        ConcurrentHashMap<String, Long> partOffsetMapFst =
                new ConcurrentHashMap<>();
        consumer1.completeSubscribe(sessionKeyFst,
                sourceCountFst, isSelectBig, partOffsetMapFst);

        // 3.4. initial and start fetch threads
        Thread[] fetchRunners1 = new Thread[fetchThreadCnt];
        for (int i = 0; i < fetchRunners1.length; i++) {
            fetchRunners1[i] = new Thread(new FetchRequestRunner(
                    consumer1, partOffsetMapFst, consumeCount), "_fetch_runner_" + i);
        }
        for (Thread thread : fetchRunners1) {
            thread.start();
        }

        // 3.5. wait consume data
        ThreadUtils.sleep(2 * 60 * 1000);
        logger.info("The first consumption has finished!");

        // 3.6. shutdown consumer
        consumer1.shutdown();
        for (Thread thread : fetchRunners1) {
            thread.join();
        }

        // 4. Start the consumer group for the second consumption
        // 4.1 set the boostrap Offset, here we set consumption from 0
        logger.info("The second consumption begin!");
        String sessionKeySec = "test_consume_Second";
        int sourceCountSec = 1;
        ConcurrentHashMap<String, Long> partOffsetMapSec =
                new ConcurrentHashMap<>();
        for (String partKey : partOffsetMapFst.keySet()) {
            partOffsetMapSec.put(partKey, 0L);
        }
        // 4.2. build consumer object
        PullMessageConsumer consumer2 =
                createPullConsumer(masterServers, groupName);
        // 4.3. Set the Topic and the filter item set corresponding to the consumption
        for (Map.Entry<String, TreeSet<String>> entry : topicAndFiltersMap.entrySet()) {
            consumer2.subscribe(entry.getKey(), entry.getValue());
        }
        consumer2.completeSubscribe(sessionKeySec,
                sourceCountSec, isSelectBig, partOffsetMapSec);

        // 4.4. initial and start fetch threads
        Thread[] fetchRunners2 = new Thread[fetchThreadCnt];
        for (int i = 0; i < fetchRunners2.length; i++) {
            fetchRunners2[i] = new Thread(new FetchRequestRunner(
                    consumer2, partOffsetMapSec, consumeCount), "_fetch_runner_" + i);
        }
        for (Thread thread : fetchRunners2) {
            thread.start();
        }
    }

    //  consumer object creation function
    private static PullMessageConsumer createPullConsumer(
            String masterHostAndPorts, String groupName) throws Exception {
        // 1. initial configure and build session factory object
        ConsumerConfig consumerConfig =
                new ConsumerConfig(masterHostAndPorts, groupName);
        // 1.2 set consume from latest position if the consumer group is first consume
        consumerConfig.setConsumePosition(ConsumePosition.CONSUMER_FROM_LATEST_OFFSET);
        // 2. build session factory object
        //    find and initialize TubeMultiSessionFactory object according to the Master cluster information
        MasterInfo masterInfo = consumerConfig.getMasterInfo();

        MessageSessionFactory sessionFactory;
        synchronized (multSessFtyMap) {
            sessionFactory =
                    multSessFtyMap.get(masterInfo.getMasterClusterStr());
            if (sessionFactory == null) {
                sessionFactory = new TubeMultiSessionFactory(consumerConfig);
                multSessFtyMap.put(masterInfo.getMasterClusterStr(), sessionFactory);
            }
        }

        // 3. Create and get the PullMessageConsumer object
        return sessionFactory.createPullConsumer(consumerConfig);
    }

    // fetch message runner
    private static class FetchRequestRunner implements Runnable {

        final PullMessageConsumer pullConsumer;
        final ConcurrentHashMap<String, Long> partOffsetMap;
        final int consumeCount;

        FetchRequestRunner(PullMessageConsumer messageConsumer,
                           ConcurrentHashMap<String, Long> partOffsetMap,
                           int msgCount) {
            this.pullConsumer = messageConsumer;
            this.partOffsetMap = partOffsetMap;
            this.consumeCount = msgCount;
        }

        @Override
        public void run() {
            ConsumerResult csmResult;
            ConsumerResult cfmResult;
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
                    // 1 judge consumer is shutdown
                    if (pullConsumer.isShutdown()) {
                        logger.warn("Consumer is shutdown!");
                        break;
                    }
                    // 2 get messages from server
                    csmResult = pullConsumer.getMessage();
                    if (csmResult.isSuccess()) {
                        // 2.1 process message if getMessage() return success
                        List<Message> messageList = csmResult.getMessageList();
                        if (messageList != null && !messageList.isEmpty()) {
                            msgRcvStats.addMsgCount(csmResult.getTopicName(), messageList.size());
                        }
                        // 2.1.2 store the offset of processing message
                        //       the offset returned by GetMessage() represents the initial offset of this request
                        //       if consumer group is pure Pull mode, the initial offset can be saved;
                        //       if not, we have to use the return value of confirmConsume()
                        partOffsetMap.put(csmResult.getPartitionKey(), csmResult.getCurrOffset());
                        // 2.1.3 confirm consume result
                        cfmResult = pullConsumer.confirmConsume(
                                csmResult.getConfirmContext(), true);
                        if (cfmResult.isSuccess()) {
                            // store confirmed offset value
                            partOffsetMap.put(csmResult.getPartitionKey(), csmResult.getCurrOffset());
                        }
                    }
                    // 3. Determine whether the consumed data reaches the goal
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
    }
}

