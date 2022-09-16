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

package org.apache.inlong.tubemq.server.tools.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.inlong.tubemq.client.common.PeerInfo;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.MessageConsumer;
import org.apache.inlong.tubemq.client.consumer.MessageListener;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.consumer.PushMessageConsumer;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.server.common.fielddef.CliArgDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is use to process CLI Consumer process for script #{bin/tubemq-consumer-test.sh}.
 */
public class CliConsumer extends CliAbstractBase {

    private static final Logger logger =
            LoggerFactory.getLogger(CliConsumer.class);
    // statistic data index
    private static final AtomicLong TOTAL_COUNTER = new AtomicLong(0);
    private static final ConcurrentHashMap<String, AtomicLong> TOPIC_COUNT_MAP =
            new ConcurrentHashMap();
    private long startTime = System.currentTimeMillis();
    // sent data content
    private final Map<String, TreeSet<String>> topicAndFiltersMap = new HashMap<>();
    private final List<MessageSessionFactory> sessionFactoryList = new ArrayList<>();
    private final Map<MessageConsumer, TupleValue> consumerMap = new HashMap<>();
    // cli parameters
    private String masterServers;
    private String groupName = "test_consume";
    private ConsumePosition consumePos =
            ConsumePosition.CONSUMER_FROM_LATEST_OFFSET;
    private long msgCount = TBaseConstants.META_VALUE_UNDEFINED;
    private long rpcTimeoutMs = TBaseConstants.META_VALUE_UNDEFINED;
    private boolean reuseConn = false;
    private int clientCount = 1;
    private int fetchThreadCnt =
            Runtime.getRuntime().availableProcessors();
    private long printIntervalMs = 5000;
    private boolean isPushConsume = false;

    private boolean isStarted = false;

    public CliConsumer() {
        super("tubemq-consumer-test.sh");
        initCommandOptions();
    }

    /**
     * Init command options
     */
    @Override
    protected void initCommandOptions() {
        // add the cli required parameters
        addCommandOption(CliArgDef.MASTERSERVER);
        addCommandOption(CliArgDef.MESSAGES);
        addCommandOption(CliArgDef.CNSTOPIC);
        addCommandOption(CliArgDef.RPCTIMEOUT);
        addCommandOption(CliArgDef.GROUP);
        addCommandOption(CliArgDef.CONNREUSE);
        addCommandOption(CliArgDef.PUSHCONSUME);
        addCommandOption(CliArgDef.CONSUMEPOS);
        addCommandOption(CliArgDef.FETCHTHREADS);
        addCommandOption(CliArgDef.CLIENTCOUNT);
        addCommandOption(CliArgDef.OUTPUTINTERVAL);
    }

    @Override
    public boolean processParams(String[] args) throws Exception {
        // parse parameters and check value
        CommandLine cli = parser.parse(options, args);
        if (cli == null) {
            throw new ParseException("Parse args failure");
        }
        if (cli.hasOption(CliArgDef.VERSION.longOpt)) {
            version();
        }
        if (cli.hasOption(CliArgDef.HELP.longOpt)) {
            help();
        }
        masterServers = cli.getOptionValue(CliArgDef.MASTERSERVER.longOpt);
        if (TStringUtils.isBlank(masterServers)) {
            throw new Exception(CliArgDef.MASTERSERVER.longOpt + " is required!");
        }
        String topicStr = cli.getOptionValue(CliArgDef.CNSTOPIC.longOpt);
        if (TStringUtils.isBlank(topicStr)) {
            throw new Exception(CliArgDef.CNSTOPIC.longOpt + " is required!");
        }
        topicAndFiltersMap.putAll(MixedUtils.parseTopicParam(topicStr));
        if (topicAndFiltersMap.isEmpty()) {
            throw new Exception("Invalid " + CliArgDef.CNSTOPIC.longOpt + " parameter value!");
        }
        String msgCntStr = cli.getOptionValue(CliArgDef.MESSAGES.longOpt);
        if (TStringUtils.isNotBlank(msgCntStr)) {
            msgCount = Long.parseLong(msgCntStr);
        }
        String groupNameStr = cli.getOptionValue(CliArgDef.GROUP.longOpt);
        if (TStringUtils.isNotBlank(groupNameStr)) {
            groupName = cli.getOptionValue(CliArgDef.GROUP.longOpt);
        }
        String reuseConnStr = cli.getOptionValue(CliArgDef.CONNREUSE.longOpt);
        if (TStringUtils.isNotBlank(reuseConnStr)) {
            reuseConn = Boolean.parseBoolean(reuseConnStr);
        }
        String rpcTimeoutStr = cli.getOptionValue(CliArgDef.RPCTIMEOUT.longOpt);
        if (TStringUtils.isNotBlank(rpcTimeoutStr)) {
            rpcTimeoutMs = Long.parseLong(rpcTimeoutStr);
        }
        String clientCntStr = cli.getOptionValue(CliArgDef.CLIENTCOUNT.longOpt);
        if (TStringUtils.isNotBlank(clientCntStr)) {
            clientCount = Integer.parseInt(clientCntStr);
        }
        String printIntMsStr = cli.getOptionValue(CliArgDef.OUTPUTINTERVAL.longOpt);
        if (TStringUtils.isNotBlank(printIntMsStr)) {
            printIntervalMs = Long.parseLong(printIntMsStr);
            if (printIntervalMs < 5000) {
                throw new Exception("Invalid "
                        + CliArgDef.OUTPUTINTERVAL.longOpt
                        + " parameter value!");
            }
        }
        String consumePosStr = cli.getOptionValue(CliArgDef.CONSUMEPOS.longOpt);
        if (TStringUtils.isNotBlank(consumePosStr)) {
            int tmpPosId = Integer.parseInt(consumePosStr);
            if (tmpPosId > 0) {
                consumePos = ConsumePosition.CONSUMER_FROM_MAX_OFFSET_ALWAYS;
            } else if (tmpPosId < 0) {
                consumePos = ConsumePosition.CONSUMER_FROM_FIRST_OFFSET;
            } else {
                consumePos = ConsumePosition.CONSUMER_FROM_LATEST_OFFSET;
            }
        }
        if (cli.hasOption(CliArgDef.PUSHCONSUME.longOpt)) {
            isPushConsume = true;
        }
        String fetchThreadCntStr = cli.getOptionValue(CliArgDef.FETCHTHREADS.longOpt);
        if (TStringUtils.isNotBlank(fetchThreadCntStr)) {
            int tmpFetchThreadCnt = Integer.parseInt(fetchThreadCntStr);
            tmpFetchThreadCnt = MixedUtils.mid(tmpFetchThreadCnt, 1, 100);
            fetchThreadCnt = tmpFetchThreadCnt;
        }
        return true;
    }

    /**
     * Initializes the TubeMQ consumer client(s) with the specified requirements.
     */
    public void initTask() throws Exception {
        // initial consumer configure
        ConsumerConfig consumerConfig =
                new ConsumerConfig(masterServers, groupName);
        consumerConfig.setRpcTimeoutMs(rpcTimeoutMs);
        consumerConfig.setPushFetchThreadCnt(fetchThreadCnt);
        consumerConfig.setConsumePosition(consumePos);
        startTime = System.currentTimeMillis();
        // initial consumer object
        if (isPushConsume) {
            DefaultMessageListener msgListener =
                    new DefaultMessageListener();
            if (reuseConn) {
                // if reuse connection, need use TubeSingleSessionFactory class
                MessageSessionFactory msgSessionFactory =
                        new TubeSingleSessionFactory(consumerConfig);
                this.sessionFactoryList.add(msgSessionFactory);
                for (int i = 0; i < clientCount; i++) {
                    PushMessageConsumer consumer1 =
                            msgSessionFactory.createPushConsumer(consumerConfig);
                    for (Map.Entry<String, TreeSet<String>> entry
                            : topicAndFiltersMap.entrySet()) {
                        consumer1.subscribe(entry.getKey(), entry.getValue(), msgListener);
                        TOPIC_COUNT_MAP.put(entry.getKey(), new AtomicLong(0));
                    }
                    consumer1.completeSubscribe();
                    consumerMap.put(consumer1, null);
                }
            } else {
                for (int i = 0; i < clientCount; i++) {
                    MessageSessionFactory msgSessionFactory =
                            new TubeMultiSessionFactory(consumerConfig);
                    this.sessionFactoryList.add(msgSessionFactory);
                    PushMessageConsumer consumer1 =
                            msgSessionFactory.createPushConsumer(consumerConfig);
                    for (Map.Entry<String, TreeSet<String>> entry
                            : topicAndFiltersMap.entrySet()) {
                        consumer1.subscribe(entry.getKey(), entry.getValue(), msgListener);
                        TOPIC_COUNT_MAP.put(entry.getKey(), new AtomicLong(0));
                    }
                    consumer1.completeSubscribe();
                    consumerMap.put(consumer1, null);
                }
            }
        } else {
            if (reuseConn) {
                MessageSessionFactory msgSessionFactory =
                        new TubeSingleSessionFactory(consumerConfig);
                this.sessionFactoryList.add(msgSessionFactory);
                for (int i = 0; i < clientCount; i++) {
                    PullMessageConsumer consumer2 =
                            msgSessionFactory.createPullConsumer(consumerConfig);
                    for (Map.Entry<String, TreeSet<String>> entry
                            : topicAndFiltersMap.entrySet()) {
                        consumer2.subscribe(entry.getKey(), entry.getValue());
                        TOPIC_COUNT_MAP.put(entry.getKey(), new AtomicLong(0));
                    }
                    consumer2.completeSubscribe();
                    consumerMap.put(consumer2,
                            new TupleValue(consumer2, msgCount, fetchThreadCnt));
                }
            } else {
                for (int i = 0; i < clientCount; i++) {
                    MessageSessionFactory msgSessionFactory =
                            new TubeMultiSessionFactory(consumerConfig);
                    this.sessionFactoryList.add(msgSessionFactory);
                    PullMessageConsumer consumer2 =
                            msgSessionFactory.createPullConsumer(consumerConfig);
                    for (Map.Entry<String, TreeSet<String>> entry
                            : topicAndFiltersMap.entrySet()) {
                        consumer2.subscribe(entry.getKey(), entry.getValue());
                        TOPIC_COUNT_MAP.put(entry.getKey(), new AtomicLong(0));
                    }
                    consumer2.completeSubscribe();
                    consumerMap.put(consumer2,
                            new TupleValue(consumer2, msgCount, fetchThreadCnt));
                }
            }
        }
        isStarted = true;
    }

    public void shutdown() throws Throwable {
        // stop process
        ThreadUtils.sleep(20);
        for (MessageConsumer consumer : consumerMap.keySet()) {
            consumer.shutdown();
        }
        for (MessageSessionFactory messageSessionFactory : sessionFactoryList) {
            messageSessionFactory.shutdown();
        }
    }

    private static class TupleValue {
        public Thread[] fetchRunners = null;

        public TupleValue(PullMessageConsumer consumer, long msgCount, int fetchThreadCnt) {
            fetchRunners = new Thread[fetchThreadCnt];
            for (int i = 0; i < fetchRunners.length; i++) {
                fetchRunners[i] = new Thread(new FetchRequestRunner(consumer, msgCount));
                fetchRunners[i].setName("_fetch_runner_" + i);
            }
            for (Thread thread : fetchRunners) {
                thread.start();
            }
        }
    }

    // for push consumer callback process
    private static class DefaultMessageListener implements MessageListener {

        public DefaultMessageListener() {
        }

        @Override
        public void receiveMessages(PeerInfo peerInfo, List<Message> messages) {
            if (messages != null && !messages.isEmpty()) {
                int msgCnt = messages.size();
                Message message = messages.get(0);
                TOTAL_COUNTER.addAndGet(msgCnt);
                AtomicLong accCount = TOPIC_COUNT_MAP.get(message.getTopic());
                accCount.addAndGet(msgCnt);
            }
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void stop() {
        }
    }

    // for push consumer process
    private static class FetchRequestRunner implements Runnable {

        private final PullMessageConsumer messageConsumer;
        private final long msgConsumeCnt;

        FetchRequestRunner(PullMessageConsumer messageConsumer, long msgConsumeCnt) {
            this.messageConsumer = messageConsumer;
            this.msgConsumeCnt = msgConsumeCnt;
        }

        @Override
        public void run() {
            try {
                do {
                    ConsumerResult result = messageConsumer.getMessage();
                    if (result.isSuccess()) {
                        List<Message> messageList = result.getMessageList();
                        if (messageList != null && !messageList.isEmpty()) {
                            int msgCnt = messageList.size();
                            TOTAL_COUNTER.addAndGet(msgCnt);
                            AtomicLong accCount =
                                    TOPIC_COUNT_MAP.get(result.getTopicName());
                            accCount.addAndGet(msgCnt);
                        }
                        messageConsumer.confirmConsume(result.getConfirmContext(), true);
                    } else {
                        if (!TErrCodeConstants.IGNORE_ERROR_SET.contains(result.getErrCode())) {
                            logger.info(
                                    "Receive messages errorCode is {}, Error message is {}",
                                    result.getErrCode(),
                                    result.getErrMsg());
                            if (messageConsumer.isShutdown()) {
                                break;
                            }
                        }
                    }
                    if (msgConsumeCnt >= 0) {
                        if (TOTAL_COUNTER.get() >= msgConsumeCnt) {
                            break;
                        }
                    }
                } while (true);
            } catch (TubeClientException e) {
                logger.error("Create consumer failed!", e);
            }
        }
    }

    /**
     * Consume messages called by the tubemq-consumer-test.sh script.
     * @param args     Call parameter array,
     *                 the relevant parameters are dynamic mode, which is parsed by CommandLine.
     */
    public static void main(String[] args) {
        CliConsumer cliConsumer = new CliConsumer();
        try {
            boolean result = cliConsumer.processParams(args);
            if (!result) {
                throw new Exception("Parse parameters failure!");
            }
            cliConsumer.initTask();
            ThreadUtils.sleep(1000);
            while (cliConsumer.msgCount < 0
                    || TOTAL_COUNTER.get() < cliConsumer.msgCount * cliConsumer.clientCount) {
                ThreadUtils.sleep(cliConsumer.printIntervalMs);
                System.out.println("Continue, cost time: "
                        + (System.currentTimeMillis() - cliConsumer.startTime)
                        + " ms, required count VS received count = "
                        + (cliConsumer.msgCount * cliConsumer.clientCount)
                        + " : " + TOTAL_COUNTER.get());
                for (Map.Entry<String, AtomicLong> entry : TOPIC_COUNT_MAP.entrySet()) {
                    System.out.println("Topic Name = " + entry.getKey()
                            + ", count=" + entry.getValue().get());
                }
            }
            cliConsumer.shutdown();
            System.out.println("Finished, cost time: "
                    + (System.currentTimeMillis() - cliConsumer.startTime)
                    + " ms, required count VS received count = "
                    + (cliConsumer.msgCount * cliConsumer.clientCount)
                    + " : " + TOTAL_COUNTER.get());
            for (Map.Entry<String, AtomicLong> entry : TOPIC_COUNT_MAP.entrySet()) {
                System.out.println("Topic Name = " + entry.getKey()
                        + ", count=" + entry.getValue().get());
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            cliConsumer.help();
        }

    }

}
