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

import static org.apache.flink.connectors.tubemq.TubemqOptions.MAX_RETRIES;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.MessageSentResult;
import org.apache.inlong.tubemq.corebase.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TubemqSinkFunction<T> extends RichSinkFunction<T> implements CheckpointedFunction {

    private static final Logger LOG = LoggerFactory.getLogger(TubemqSinkFunction.class);

    private static final String SYSTEM_HEADER_TIME_FORMAT = "yyyyMMddHHmm";

    /**
     * The address of tubemq master, format eg: 127.0.0.1:8080,127.0.0.2:8081.
     */
    private final String masterAddress;

    /**
     * The topic name.
     */
    private final String topic;

    /**
     * The tid of this topic
     */
    private final String tid;
    /**
     * The serializer for the records sent to pulsar.
     */
    private final SerializationSchema<T> serializationSchema;

    /**
     * The tubemq producer.
     */
    private transient MessageProducer producer;

    /**
     * The tubemq session factory.
     */
    private transient MessageSessionFactory sessionFactory;

    /**
     * The maximum number of retries.
     */
    private final int maxRetries;

    public TubemqSinkFunction(String topic,
                              String masterAddress,
                              SerializationSchema<T> serializationSchema,
                              Configuration configuration) {
        Preconditions.checkNotNull(topic,
            "The topic must not be null.");
        Preconditions.checkNotNull(masterAddress,
            "The master address must not be null.");
        Preconditions.checkNotNull(serializationSchema,
            "The serialization schema must not be null.");
        Preconditions.checkNotNull(configuration,
            "The configuration must not be null.");

        this.topic = topic;
        this.masterAddress = masterAddress;
        this.serializationSchema = serializationSchema;
        this.tid = configuration.getString(TubemqOptions.TID);
        this.maxRetries = configuration.getInteger(MAX_RETRIES);
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) {
        // Nothing to do.
    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) {
        // Nothing to do.
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        TubeClientConfig tubeClientConfig = new TubeClientConfig(masterAddress);
        this.sessionFactory = new TubeSingleSessionFactory(tubeClientConfig);
        this.producer = sessionFactory.createProducer();
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add(topic);
        producer.publish(hashSet);
    }

    @Override
    public void invoke(T in, Context context) throws Exception {

        int retries = 0;
        Exception exception = null;

        while (maxRetries <= 0 || retries < maxRetries) {

            try {
                byte[] body = serializationSchema.serialize(in);
                Message message = new Message(topic, body);
                if (StringUtils.isNotBlank(tid)) {
                    SimpleDateFormat sdf = new SimpleDateFormat(SYSTEM_HEADER_TIME_FORMAT);
                    long currTimeMillis = System.currentTimeMillis();
                    message.putSystemHeader(tid, sdf.format(new Date(currTimeMillis)));
                }

                MessageSentResult sendResult = producer.sendMessage(message);
                if (sendResult.isSuccess()) {
                    return;
                } else {
                    LOG.warn("Send msg fail, error code: {}, error message: {}",
                        sendResult.getErrCode(), sendResult.getErrMsg());
                }
            } catch (Exception e) {
                LOG.warn("Could not properly send the message to hippo "
                        + "(retries: {}).", retries, e);

                retries++;
                exception = ExceptionUtils.firstOrSuppressed(e, exception);
            }
        }

        throw new IOException("Could not properly send the message to hippo.", exception);
    }

    @Override
    public void close() throws Exception {

        try {
            if (producer != null) {
                producer.shutdown();
                producer = null;
            }
            if (sessionFactory != null) {
                sessionFactory.shutdown();
                sessionFactory = null;
            }
        } catch (Throwable e) {
            LOG.error("Shutdown producer error", e);
        } finally {
            super.close();
        }
    }
}
