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
import java.util.Set;
import java.util.TreeSet;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentCallback;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This demo shows how to produce message normally.
 *
 *Producer supports publish one or more topics via {@link MessageProducer#publish(String)}
 * or {@link MessageProducer#publish(Set)}. Note that topic publish asynchronously.
 */
public final class MessageProducerExample {

    private static final Logger logger =
            LoggerFactory.getLogger(MessageProducerExample.class);
    private static final MsgSendReceiveStats msgSendStats =
            new MsgSendReceiveStats(true);

    private static MessageSessionFactory sessionFactory;
    private static MessageProducer messageProducer;

    /**
     * Produce messages through a single-session factory instance.
     *
     * @param args   Startup parameter array, including the following parts:
     *               The 1st parameter masterServers is the master address(es) to connect to,
     *                       format is master1_ip:port[,master2_ip:port];
     *               The 2nd parameter pubTopicAndFilterItems is the topic(s) (and filter condition set) to publish to,
     *                       format is topic_1[[:filterCond_1.1[;filterCond_1.2]][,topic_2]];
     *               The 3rd parameter msgCount is the message amount that needs to be sent;
     *               The 4th parameter pkgSize is the message's body size that needs to be sent.
     */
    public static void main(String[] args) throws Throwable {
        // 1. get and initial parameters
        final String masterServers = args[0];
        final String pubTopicAndFilterItems = args[1];
        final long msgCount = Long.parseLong(args[2]);
        int pkgSize = 1024;
        if (args.length > 3) {
            pkgSize = MixedUtils.mid(Integer.parseInt(args[3]), 1, 1024 * 1024);
        }
        final Map<String, TreeSet<String>> topicAndFiltersMap =
                MixedUtils.parseTopicParam(pubTopicAndFilterItems);

        // 2. initial configure, session factory object, and producer object
        TubeClientConfig clientConfig =
                new TubeClientConfig(masterServers);
        sessionFactory = new TubeSingleSessionFactory(clientConfig);
        messageProducer = sessionFactory.createProducer();
        messageProducer.publish(topicAndFiltersMap.keySet());

        // 3. initial and statistic thread
        Thread statisticThread =
                new Thread(msgSendStats, "Sent Statistic Thread");
        statisticThread.start();

        // 4. build the content of the message to be sent
        //    include message body, attributes, and time information template
        final byte[] bodyData =
                MixedUtils.buildTestData(pkgSize);
        List<Tuple2<String, String>> buildTopicFilterTuples =
                MixedUtils.buildTopicFilterTupleList(topicAndFiltersMap);

        // 5. send message to server
        long sentCount = 0;
        int roundIndex = 0;
        int targetCnt = buildTopicFilterTuples.size();
        while (msgCount < 0 || sentCount < msgCount) {
            // 5.1 Rotate to get the attribute information to be sent
            roundIndex = (int) (sentCount++ % targetCnt);
            Tuple2<String, String> target = buildTopicFilterTuples.get(roundIndex);

            // 5.2 send message
            try {
                // 5.2.1 Send data asynchronously, recommended
                messageProducer.sendMessage(MixedUtils.buildMessage(target.getF0(),
                        target.getF1(), bodyData, sentCount), new DefaultSendCallback());
                // Or
                // 5.2.2 Send message synchronous, not recommended
                // MessageSentResult result = messageProducer.sendMessage(message);
                // if (!result.isSuccess()) {
                //    logger.error("Sync-send message failed!" + result.getErrMsg());
                // }
            } catch (TubeClientException | InterruptedException e) {
                logger.error("Send message failed!", e);
            }

            // 5.3 Cool sending
            //     Attention: only used in the test link, to solve the problem of
            //                frequent sending failures caused by insufficient test resources.
            MixedUtils.coolSending(sentCount);
        }

        // 6. Clean up resources and exit the service after the task is completed
        //    Attention: TubeMQ client is suitable for serving as a resident service,
        //               not suitable for creating Producer objects message by message
        messageProducer.shutdown();
        sessionFactory.shutdown();
        msgSendStats.stopStats();
        logger.info("The message sending task has been completed!");
    }

    private static class DefaultSendCallback implements MessageSentCallback {

        @Override
        public void onMessageSent(MessageSentResult result) {
            if (result.isSuccess()) {
                msgSendStats.addMsgCount(result.getMessage().getTopic(), 1);
            } else {
                logger.error("Send message failed!" + result.getErrMsg());
            }
        }

        @Override
        public void onException(Throwable e) {
            logger.error("Send message error!", e);
        }
    }

}
