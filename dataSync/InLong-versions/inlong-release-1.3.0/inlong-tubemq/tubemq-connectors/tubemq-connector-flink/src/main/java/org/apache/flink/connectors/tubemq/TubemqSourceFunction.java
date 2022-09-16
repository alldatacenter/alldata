/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.connectors.tubemq;

import static org.apache.flink.api.common.typeinfo.BasicTypeInfo.LONG_TYPE_INFO;
import static org.apache.flink.api.common.typeinfo.BasicTypeInfo.STRING_TYPE_INFO;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.TimeUtils.parseDuration;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.OperatorStateStore;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Flink TubeMQ Consumer.
 *
 * @param <T> The type of records produced by this data source
 */
public class TubemqSourceFunction<T>
    extends RichParallelSourceFunction<T> implements CheckpointedFunction {

    private static final Logger LOG =
        LoggerFactory.getLogger(TubemqSourceFunction.class);

    private static final String TUBE_OFFSET_STATE = "tube-offset-state";

    private static final String SPLIT_COMMA = ",";
    private static final String SPLIT_COLON = ":";

    /**
     * The address of TubeMQ master, format eg: 127.0.0.1:8080,127.0.0.2:8081.
     */
    private final String masterAddress;

    /**
     * The topic name.
     */
    private final String topic;

    /**
     * The tubemq consumers use this tid set to filter records reading from server.
     */
    private final TreeSet<String> tidSet;

    /**
     * The consumer group name.
     */
    private final String consumerGroup;

    /**
     * The deserializer for records.
     */
    private final DeserializationSchema<T> deserializationSchema;

    /**
     * The random key for TubeMQ consumer group when startup.
     */
    private final String sessionKey;

    /**
     * True if consuming message from max offset.
     */
    private final boolean consumeFromMax;

    /**
     * The time to wait if tubemq broker returns message not found.
     */
    private final Duration messageNotFoundWaitPeriod;

    /**
     * The max time to marked source idle.
     */
    private final Duration maxIdleTime;

    /**
     * Flag indicating whether the consumer is still running.
     **/
    private volatile boolean running;

    /**
     * The state for the offsets of queues.
     */
    private transient ListState<Tuple2<String, Long>> offsetsState;

    /**
     * The current offsets of partitions which are stored in {@link #offsetsState}
     * once a checkpoint is triggered.
     *
     *NOTE: The offsets are populated in the main thread and saved in the
     * checkpoint thread. Its usage must be guarded by the checkpoint lock.</p>
     */
    private transient Map<String, Long> currentOffsets;

    /**
     * The TubeMQ session factory.
     */
    private transient TubeSingleSessionFactory messageSessionFactory;

    /**
     * The TubeMQ pull consumer.
     */
    private transient PullMessageConsumer messagePullConsumer;

    /**
     * Build a TubeMQ source function
     *
     * @param masterAddress            the master address of TubeMQ
     * @param topic                    the topic name
     * @param tidSet                   the  topic's filter condition items
     * @param consumerGroup            the consumer group name
     * @param deserializationSchema    the deserialize schema
     * @param configuration            the configure
     */
    public TubemqSourceFunction(
        String masterAddress,
        String topic,
        TreeSet<String> tidSet,
        String consumerGroup,
        DeserializationSchema<T> deserializationSchema,
        Configuration configuration
    ) {
        checkNotNull(masterAddress,
            "The master address must not be null.");
        checkNotNull(topic,
            "The topic must not be null.");
        checkNotNull(tidSet,
            "The tid set must not be null.");
        checkNotNull(consumerGroup,
            "The consumer group must not be null.");
        checkNotNull(deserializationSchema,
            "The deserialization schema must not be null.");
        checkNotNull(configuration,
            "The configuration must not be null.");

        this.masterAddress = masterAddress;
        this.topic = topic;
        this.tidSet = tidSet;
        this.consumerGroup = consumerGroup;
        this.deserializationSchema = deserializationSchema;

        this.sessionKey =
            configuration.getString(TubemqOptions.SESSION_KEY);
        this.consumeFromMax =
            configuration.getBoolean(TubemqOptions.BOOTSTRAP_FROM_MAX);
        this.messageNotFoundWaitPeriod =
            parseDuration(
                configuration.getString(
                    TubemqOptions.MESSAGE_NOT_FOUND_WAIT_PERIOD));
        this.maxIdleTime =
            parseDuration(
                configuration.getString(
                    TubemqOptions.SOURCE_MAX_IDLE_TIME));
    }

    @Override
    public void initializeState(
        FunctionInitializationContext context
    ) throws Exception {

        TypeInformation<Tuple2<String, Long>> typeInformation =
            new TupleTypeInfo<>(STRING_TYPE_INFO, LONG_TYPE_INFO);
        ListStateDescriptor<Tuple2<String, Long>> stateDescriptor =
            new ListStateDescriptor<>(TUBE_OFFSET_STATE, typeInformation);

        OperatorStateStore stateStore = context.getOperatorStateStore();
        offsetsState = stateStore.getListState(stateDescriptor);

        currentOffsets = new HashMap<>();
        if (context.isRestored()) {
            for (Tuple2<String, Long> tubeOffset : offsetsState.get()) {
                currentOffsets.put(tubeOffset.f0, tubeOffset.f1);
            }

            LOG.info("Successfully restore the offsets {}.", currentOffsets);
        } else {
            LOG.info("No restore offsets.");
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        ConsumerConfig consumerConfig =
            new ConsumerConfig(masterAddress, consumerGroup);
        consumerConfig.setConsumePosition(consumeFromMax
                ? ConsumePosition.CONSUMER_FROM_MAX_OFFSET_ALWAYS
                : ConsumePosition.CONSUMER_FROM_LATEST_OFFSET);
        consumerConfig.setMsgNotFoundWaitPeriodMs(messageNotFoundWaitPeriod.toMillis());

        final int numTasks = getRuntimeContext().getNumberOfParallelSubtasks();

        messageSessionFactory = new TubeSingleSessionFactory(consumerConfig);

        messagePullConsumer =
            messageSessionFactory.createPullConsumer(consumerConfig);
        messagePullConsumer
            .subscribe(topic, tidSet);
        messagePullConsumer
            .completeSubscribe(sessionKey, numTasks, true, currentOffsets);

        running = true;
    }

    @Override
    public void run(SourceContext<T> ctx) throws Exception {

        Instant lastConsumeInstant = Instant.now();

        while (running) {

            ConsumerResult consumeResult = messagePullConsumer.getMessage();
            if (!consumeResult.isSuccess()) {
                if (!(consumeResult.getErrCode() == 400
                        || consumeResult.getErrCode() == 404
                        || consumeResult.getErrCode() == 405
                        || consumeResult.getErrCode() == 406
                        || consumeResult.getErrCode() == 407
                        || consumeResult.getErrCode() == 408)) {
                    LOG.info("Could not consume messages from tubemq (errcode: {}, "
                                    + "errmsg: {}).", consumeResult.getErrCode(),
                            consumeResult.getErrMsg());
                }

                Duration idleTime =
                    Duration.between(lastConsumeInstant, Instant.now());
                if (idleTime.compareTo(maxIdleTime) > 0) {
                    ctx.markAsTemporarilyIdle();
                }

                continue;
            }

            List<Message> messageList = consumeResult.getMessageList();

            List<T> records = new ArrayList<>();
            if (messageList != null) {
                lastConsumeInstant = Instant.now();

                for (Message message : messageList) {
                    T record =
                        deserializationSchema.deserialize(message.getData());
                    records.add(record);
                }
            }

            synchronized (ctx.getCheckpointLock()) {

                for (T record : records) {
                    ctx.collect(record);
                }

                currentOffsets.put(
                    consumeResult.getPartitionKey(),
                    consumeResult.getCurrOffset()
                );
            }

            ConsumerResult confirmResult =
                messagePullConsumer
                    .confirmConsume(consumeResult.getConfirmContext(), true);
            if (!confirmResult.isSuccess()) {
                if (!(confirmResult.getErrCode() == 400
                        || confirmResult.getErrCode() == 404
                        || confirmResult.getErrCode() == 405
                        || confirmResult.getErrCode() == 406
                        || confirmResult.getErrCode() == 407
                        || confirmResult.getErrCode() == 408)) {
                    LOG.warn("Could not confirm messages to tubemq (errcode: {}, "
                                    + "errmsg: {}).", confirmResult.getErrCode(),
                            confirmResult.getErrMsg());
                }
            }
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {

        offsetsState.clear();
        for (Map.Entry<String, Long> entry : currentOffsets.entrySet()) {
            offsetsState.add(new Tuple2<>(entry.getKey(), entry.getValue()));
        }

        LOG.info("Successfully save the offsets in checkpoint {}: {}.",
            context.getCheckpointId(), currentOffsets);
    }

    @Override
    public void cancel() {
        running = false;
    }

    @Override
    public void close() throws Exception {

        cancel();

        if (messagePullConsumer != null) {
            try {
                messagePullConsumer.shutdown();
            } catch (Throwable t) {
                LOG.warn("Could not properly shutdown the tubemq pull consumer.", t);
            }
        }

        if (messageSessionFactory != null) {
            try {
                messageSessionFactory.shutdown();
            } catch (Throwable t) {
                LOG.warn("Could not properly shutdown the tubemq session factory.", t);
            }
        }

        super.close();

        LOG.info("Closed the tubemq source.");
    }
}
