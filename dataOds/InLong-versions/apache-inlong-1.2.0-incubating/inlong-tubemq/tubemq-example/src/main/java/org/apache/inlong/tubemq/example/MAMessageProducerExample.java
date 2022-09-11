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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentCallback;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This demo shows how to use the multi-connected {@link TubeMultiSessionFactory} in the sample single process.
 * With {@link TubeMultiSessionFactory}, a single process can establish concurrent physical request connections
 * to improve throughput from client to broker.
 */
public class MAMessageProducerExample {
    private static final Logger logger =
            LoggerFactory.getLogger(MAMessageProducerExample.class);
    private static final MsgSendReceiveStats msgSendStats =
            new MsgSendReceiveStats(true);
    private static final Map<Integer, Tuple2<MessageSessionFactory, Set<MessageProducer>>>
            sessionFactoryProducerMap = new HashMap<>();
    private static final AtomicLong totalSentCnt = new AtomicLong(0);
    private static ExecutorService sendExecutorService;

    /**
     * Produce messages through multi-session factory instances.
     *
     * @param args   Startup parameter array, including the following parts:
     *               The 1st parameter masterServers is the master address(es) to connect to,
     *                       format is master1_ip:port[,master2_ip:port];
     *               The 2nd parameter pubTopicAndFilterItems is the topic(s) (and filter condition set) to publish to,
     *                       format is topic_1[[:filterCond_1.1[;filterCond_1.2]][,topic_2]];
     *               The 3rd parameter msgCount is the message amount that needs to be sent;
     *               The 4th parameter pkgSize is the message's body size that needs to be sent;
     *               The 5th parameter clientCount is the amount of producer;
     *               The 6th parameter sessionFactoryCnt is the amount of session factory.
     */
    public static void main(String[] args) throws Throwable {
        // 1. get call parameters
        final String masterServers = args[0];
        final String pubTopicAndFilterItems = args[1];
        final long msgCount = Long.parseLong(args[2]);
        int pkgSize = 1024;
        if (args.length > 3) {
            pkgSize = MixedUtils.mid(Integer.parseInt(args[3]), 1, 1024 * 1024);
        }
        int clientCnt = 2;
        if (args.length > 4) {
            clientCnt = MixedUtils.mid(Integer.parseInt(args[4]), 1, 100);
        }
        int sessionFactoryCnt = 10;
        if (args.length > 5) {
            sessionFactoryCnt = MixedUtils.mid(Integer.parseInt(args[5]), 1, 20);
        }
        final Map<String, TreeSet<String>> topicAndFiltersMap =
                MixedUtils.parseTopicParam(pubTopicAndFilterItems);

        // 2. build multi-session factory
        TubeClientConfig clientConfig = new TubeClientConfig(masterServers);
        for (int i = 0; i < sessionFactoryCnt; i++) {
            sessionFactoryProducerMap.put(i,
                    new Tuple2<>(new TubeMultiSessionFactory(clientConfig), new HashSet<>()));
        }

        // 3. build multi-thread message sender
        sendExecutorService =
                Executors.newFixedThreadPool(clientCnt, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        return new Thread(runnable);
                    }
                });

        // 4. initial and statistic thread
        Thread statisticThread =
                new Thread(msgSendStats, "Sent Statistic Thread");
        statisticThread.start();

        // 5. build the content of the message to be sent
        //    include message body, attributes, and time information template
        final byte[] bodyData =
                MixedUtils.buildTestData(pkgSize);
        List<Tuple2<String, String>> buildTopicFilterTuples =
                MixedUtils.buildTopicFilterTupleList(topicAndFiltersMap);

        // 6. Rotating build and start producers in the session factory list
        //    In the same process, different TubeMultiSessionFactory objects can create
        //    independent connections for the same Broker.
        //   We increase the concurrent throughput of the system by increasing the
        //   number of links. Here we distribute the clients evenly on
        //   different TubeMultiSessionFactory objects to
        //   improve data production performance in the single process
        for (int indexId = 0; indexId < clientCnt; indexId++) {
            Tuple2<MessageSessionFactory, Set<MessageProducer>> sessionProducerMap =
                    sessionFactoryProducerMap.get(indexId % sessionFactoryCnt);
            MessageProducer producer = sessionProducerMap.getF0().createProducer();
            producer.publish(topicAndFiltersMap.keySet());
            sessionProducerMap.getF1().add(producer);
            sendExecutorService.submit(new Sender(indexId,
                    producer, bodyData, buildTopicFilterTuples, msgCount));
        }

        // 7. wait all tasks finished
        try {
            // wait util sent message's count reachs required count
            long needSentCnt = msgCount * clientCnt;
            while (totalSentCnt.get() < needSentCnt) {
                logger.info("Sending task is running, total = {}, finished = {}",
                        needSentCnt, totalSentCnt.get());
                Thread.sleep(30000);
            }
        } catch (Throwable e) {
            logger.error("Throwable: ", e);
        }
        // clear resources
        sendExecutorService.shutdownNow();
        for (int i = 0; i < sessionFactoryCnt; i++) {
            sessionFactoryProducerMap.get(i).getF0().shutdown();
        }
        msgSendStats.stopStats();
        logger.info("Sending task is finished, total sent {} messages", totalSentCnt.get());
    }

    public static class Sender implements Runnable {

        private final int indexId;
        private final MessageProducer producer;
        private final byte[] bodyData;
        private final long msgCount;
        private final List<Tuple2<String, String>> topicFilterTuples;

        public Sender(int indexId, MessageProducer producer, byte[] bodyData,
                      List<Tuple2<String, String>> topicFilterTuples, long msgCount) {
            this.indexId = indexId;
            this.producer = producer;
            this.bodyData = bodyData;
            this.msgCount = msgCount;
            this.topicFilterTuples = topicFilterTuples;
        }

        @Override
        public void run() {
            // send message to server
            long sentCount = 0;
            int roundIndex = 0;
            int targetCnt = topicFilterTuples.size();
            while (msgCount < 0 || sentCount < msgCount) {
                // 1 Rotate to get the attribute information to be sent
                roundIndex = (int) (sentCount++ % targetCnt);
                Tuple2<String, String> target = topicFilterTuples.get(roundIndex);

                // 2 send message
                try {
                    // 2.1 Send data asynchronously, recommended
                    producer.sendMessage(MixedUtils.buildMessage(target.getF0(),
                            target.getF1(), bodyData, sentCount), new DefaultSendCallback());
                    // Or
                    // 2.2 Send message synchronous, not recommended
                    // MessageSentResult result = producer.sendMessage(message);
                    // totalSentCnt.incrementAndGet();
                    // if (!result.isSuccess()) {
                    //    logger.error("Sync-send message failed!" + result.getErrMsg());
                    // }
                } catch (TubeClientException | InterruptedException e) {
                    logger.error("Send message failed!", e);
                }
                // 3 Cool sending
                //     Attention: only used in the test link, to solve the problem of
                //                frequent sending failures caused by insufficient test resources.
                MixedUtils.coolSending(sentCount);
            }
            try {
                producer.shutdown();
            } catch (Throwable e) {
                logger.error("producer shutdown error: ", e);
            }
            logger.info("The message sending task(" + indexId + ") has been completed!");
        }
    }

    private static class DefaultSendCallback implements MessageSentCallback {

        @Override
        public void onMessageSent(MessageSentResult result) {
            totalSentCnt.incrementAndGet();
            if (result.isSuccess()) {
                msgSendStats.addMsgCount(result.getMessage().getTopic(), 1);
            } else {
                logger.error("Send message failed!" + result.getErrMsg());
            }
        }

        @Override
        public void onException(Throwable e) {
            totalSentCnt.incrementAndGet();
            logger.error("Send message error!", e);
        }
    }
}
