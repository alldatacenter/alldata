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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentCallback;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.fielddef.CliArgDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is use to process CLI Producer process for script #{bin/tubemq-producer-test.sh}.
 *
 *
 */
public class CliProducer extends CliAbstractBase {

    private static final Logger logger =
            LoggerFactory.getLogger(CliProducer.class);
    // start time
    private long startTime = System.currentTimeMillis();
    // statistic data index
    private static final AtomicLong TOTAL_COUNTER = new AtomicLong(0);
    private static final AtomicLong SENT_SUCC_COUNTER = new AtomicLong(0);
    private static final AtomicLong SENT_FAIL_COUNTER = new AtomicLong(0);
    private static final AtomicLong SENT_EXCEPT_COUNTER = new AtomicLong(0);
    // sent data content
    private static byte[] sentData;
    private final Map<String, TreeSet<String>> topicAndFiltersMap = new HashMap<>();
    private static List<Tuple2<String, String>> topicSendRounds = new ArrayList<>();
    private final List<MessageSessionFactory> sessionFactoryList = new ArrayList<>();
    private final Map<MessageProducer, MsgSender> producerMap = new HashMap<>();
    // cli parameters
    private String masterServers;
    private long msgCount = TBaseConstants.META_VALUE_UNDEFINED;
    private boolean useRandData = true;
    private int msgDataSize = 1000;
    private String payloadFilePath = null;
    private String payloadDelim = null;
    private long rpcTimeoutMs = TBaseConstants.META_VALUE_UNDEFINED;
    private boolean reuseConn = false;
    private int clientCount = 1;
    private int sendThreadCnt = 100;
    private long printIntervalMs = 5000;
    private boolean syncProduction = false;
    private boolean withoutDelay = false;
    private boolean isStarted = false;
    private ExecutorService sendExecutorService = null;

    public CliProducer() {
        super("tubemq-producer-test.sh");
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
        addCommandOption(CliArgDef.MSGDATASIZE);
        //addCommandOption(CliArgDef.PAYLOADFILE);
        //addCommandOption(CliArgDef.PAYLOADDELIM);
        addCommandOption(CliArgDef.PRDTOPIC);
        addCommandOption(CliArgDef.RPCTIMEOUT);
        addCommandOption(CliArgDef.CONNREUSE);
        addCommandOption(CliArgDef.CLIENTCOUNT);
        addCommandOption(CliArgDef.OUTPUTINTERVAL);
        addCommandOption(CliArgDef.SYNCPRODUCE);
        addCommandOption(CliArgDef.SENDTHREADS);
        addCommandOption(CliArgDef.WITHOUTDELAY);
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
        String topicStr = cli.getOptionValue(CliArgDef.PRDTOPIC.longOpt);
        if (TStringUtils.isBlank(topicStr)) {
            throw new Exception(CliArgDef.PRDTOPIC.longOpt + " is required!");
        }
        topicAndFiltersMap.putAll(MixedUtils.parseTopicParam(topicStr));
        if (topicAndFiltersMap.isEmpty()) {
            throw new Exception("Invalid " + CliArgDef.PRDTOPIC.longOpt + " parameter value!");
        }
        String msgCntStr = cli.getOptionValue(CliArgDef.MESSAGES.longOpt);
        if (TStringUtils.isNotBlank(msgCntStr)) {
            msgCount = Long.parseLong(msgCntStr);
        }
        String msgDataSizeStr = cli.getOptionValue(CliArgDef.MSGDATASIZE.longOpt);
        if (TStringUtils.isNotBlank(msgDataSizeStr)) {
            msgDataSize = Integer.parseInt(msgDataSizeStr);
        }
        String reuseConnStr = cli.getOptionValue(CliArgDef.CONNREUSE.longOpt);
        if (TStringUtils.isNotBlank(reuseConnStr)) {
            reuseConn = Boolean.parseBoolean(reuseConnStr);
        }
        String sendThreadCntStr = cli.getOptionValue(CliArgDef.SENDTHREADS.longOpt);
        if (TStringUtils.isNotBlank(sendThreadCntStr)) {
            int tmpThreadCnt = Integer.parseInt(sendThreadCntStr);
            tmpThreadCnt = MixedUtils.mid(tmpThreadCnt, 1, 200);
            sendThreadCnt = tmpThreadCnt;
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
        if (cli.hasOption(CliArgDef.SYNCPRODUCE.longOpt)) {
            syncProduction = true;
        }
        if (cli.hasOption(CliArgDef.WITHOUTDELAY.longOpt)) {
            withoutDelay = true;
        }
        return true;
    }

    /**
     * Initializes the TubeMQ producer client(s) with the specified requirements.
     */
    public void initTask() throws Exception {
        // initial client configure
        TubeClientConfig clientConfig = new TubeClientConfig(masterServers);
        clientConfig.setRpcTimeoutMs(rpcTimeoutMs);
        // initial sent data
        sentData = MixedUtils.buildTestData(msgDataSize);
        // initial topic send round
        topicSendRounds = MixedUtils.buildTopicFilterTupleList(topicAndFiltersMap);
        startTime = System.currentTimeMillis();
        // initial send thread service
        sendExecutorService =
                Executors.newFixedThreadPool(sendThreadCnt, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        return new Thread(runnable, "sender_" + producerMap.size());
                    }
                });
        // initial producer object
        if (reuseConn) {
            // if resue connection, use TubeSingleSessionFactory class
            MessageSessionFactory msgSessionFactory =
                    new TubeSingleSessionFactory(clientConfig);
            this.sessionFactoryList.add(msgSessionFactory);
            for (int i = 0; i < clientCount; i++) {
                MessageProducer producer = msgSessionFactory.createProducer();
                producer.publish(topicAndFiltersMap.keySet());
                producerMap.put(producer, new MsgSender(producer));
                // send send task
                sendExecutorService.submit(producerMap.get(producer));
            }
        } else {
            for (int i = 0; i < clientCount; i++) {
                // if not resue connection, use TubeMultiSessionFactory class
                MessageSessionFactory msgSessionFactory =
                        new TubeMultiSessionFactory(clientConfig);
                this.sessionFactoryList.add(msgSessionFactory);
                MessageProducer producer = msgSessionFactory.createProducer();
                producer.publish(topicAndFiltersMap.keySet());
                producerMap.put(producer, new MsgSender(producer));
                // send send task
                sendExecutorService.submit(producerMap.get(producer));
            }
        }
        isStarted = true;
    }

    public void shutdown() throws Throwable {
        // stop process
        if (sendExecutorService != null) {
            sendExecutorService.shutdownNow();
        }
        ThreadUtils.sleep(20);
        for (MessageProducer producer : producerMap.keySet()) {
            producer.shutdown();
        }
        for (MessageSessionFactory messageSessionFactory : sessionFactoryList) {
            messageSessionFactory.shutdown();
        }
    }

    // process message send
    public class MsgSender implements Runnable {

        private final MessageProducer producer;

        public MsgSender(MessageProducer producer) {
            this.producer = producer;
        }

        @Override
        public void run() {
            int topicAndCondCnt = topicSendRounds.size();
            long sentCount = 0;
            int roundIndex = 0;
            while (msgCount < 0 || sentCount < msgCount) {
                roundIndex = (int) (sentCount++ % topicAndCondCnt);
                try {
                    Tuple2<String, String> target = topicSendRounds.get(roundIndex);
                    Message message = MixedUtils.buildMessage(
                            target.getF0(), target.getF1(), sentData, sentCount);
                    // use sync or async process
                    if (syncProduction) {
                        MessageSentResult procResult =
                                producer.sendMessage(message);
                        TOTAL_COUNTER.incrementAndGet();
                        if (procResult.isSuccess()) {
                            SENT_SUCC_COUNTER.incrementAndGet();
                        } else {
                            SENT_FAIL_COUNTER.incrementAndGet();
                        }
                    } else {
                        producer.sendMessage(message, new DefaultSendCallback());
                    }
                } catch (Throwable e1) {
                    TOTAL_COUNTER.incrementAndGet();
                    SENT_EXCEPT_COUNTER.incrementAndGet();
                    logger.error("sendMessage exception: ", e1);
                }
                // Limit sending flow control to avoid frequent errors
                // caused by too many inflight messages being sent
                if (!withoutDelay) {
                    MixedUtils.coolSending(sentCount);
                }
            }
            // finished, close client
            try {
                producer.shutdown();
            } catch (Throwable e) {
                logger.error("producer shutdown error: ", e);
            }
        }
    }

    private class DefaultSendCallback implements MessageSentCallback {
        @Override
        public void onMessageSent(MessageSentResult result) {
            TOTAL_COUNTER.incrementAndGet();
            if (result.isSuccess()) {
                SENT_SUCC_COUNTER.incrementAndGet();
            } else {
                SENT_FAIL_COUNTER.incrementAndGet();
            }
        }

        @Override
        public void onException(Throwable e) {
            TOTAL_COUNTER.incrementAndGet();
            SENT_EXCEPT_COUNTER.incrementAndGet();
            logger.error("Send message error!", e);
        }
    }

    /**
     * Produce messages called by the tubemq-producer-test.sh script.
     * @param args     Call parameter array,
     *                 the relevant parameters are dynamic mode, which is parsed by CommandLine.
     */
    public static void main(String[] args) {
        CliProducer cliProducer = new CliProducer();
        try {
            boolean result = cliProducer.processParams(args);
            if (!result) {
                throw new Exception("Parse parameters failure!");
            }
            cliProducer.initTask();
            ThreadUtils.sleep(1000);
            while (cliProducer.msgCount < 0
                    || TOTAL_COUNTER.get() < cliProducer.msgCount * cliProducer.clientCount) {
                ThreadUtils.sleep(cliProducer.printIntervalMs);
                System.out.println("Continue, cost time: "
                        + (System.currentTimeMillis() - cliProducer.startTime)
                        + "ms, required count VS sent count = "
                        + (cliProducer.msgCount * cliProducer.clientCount)
                        + " : " + TOTAL_COUNTER.get()
                        + " (" + SENT_SUCC_COUNTER.get()
                        + ":" + SENT_FAIL_COUNTER.get()
                        + ":" + SENT_EXCEPT_COUNTER.get()
                        + ")");
            }
            cliProducer.shutdown();
            System.out.println("Finished, cost time: "
                    + (System.currentTimeMillis() - cliProducer.startTime)
                    + "ms, required count VS sent count = "
                    + (cliProducer.msgCount * cliProducer.clientCount)
                    + " : " + TOTAL_COUNTER.get()
                    + " (" + SENT_SUCC_COUNTER.get()
                    + ":" + SENT_FAIL_COUNTER.get()
                    + ":" + SENT_EXCEPT_COUNTER.get()
                    + ")");
        } catch (Throwable ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            cliProducer.help();
        }

    }
}
