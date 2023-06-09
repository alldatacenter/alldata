/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.datax.kafka.writer;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.ImmutableMap;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ValidatorCommons;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugins.datax.kafka.writer.protocol.KafkaProtocol;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/connectors/datastream/kafka/
 */
public class DataXKafkaWriter extends DataxWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataXKafkaWriter.class);


    @FormField(ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String bootstrapServers;

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String topic;

    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {})
    public String testTopic;

    @FormField(ordinal = 3, validate = {Validator.require})
    public KafkaProtocol protocol;

    @FormField(ordinal = 8, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer batchSize;

    @FormField(ordinal = 17, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String socketConnectionSetupTimeoutMaxMs;

    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer deliveryTimeoutMs;

    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {Validator.require})
    public String acks;

    @FormField(ordinal = 5, type = FormFieldType.ENUM, validate = {Validator.require})
    public String compressionType;

    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer sendBufferBytes;

    @FormField(ordinal = 6, type = FormFieldType.ENUM, validate = {Validator.require}, advance = true)
    public String clientDnsLookup;

    @FormField(ordinal = 7, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer}, advance = true)
    public Integer requestTimeoutMs;


    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {Validator.require}, advance = true)
    public String lingerMs;

    @FormField(ordinal = 10, type = FormFieldType.INPUTTEXT, validate = {}, advance = true)
    public String clientId;

    @FormField(ordinal = 11, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer}, advance = true)
    public Integer maxRequestSize;

    @FormField(ordinal = 12, type = FormFieldType.ENUM, validate = {Validator.require}, advance = true)
    public Boolean enableIdempotence;

    @FormField(ordinal = 13, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer}, advance = true)
    public Integer maxInFlightRequestsPerConnection;

    @FormField(ordinal = 14, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer}, advance = true)
    public Integer retries;

    @FormField(ordinal = 15, type = FormFieldType.INPUTTEXT, validate = {Validator.require}, advance = true)
    public String socketConnectionSetupTimeoutMs;


    @FormField(ordinal = 18, type = FormFieldType.INPUTTEXT, validate = {Validator.require}, advance = true)
    public String bufferMemory;

    @FormField(ordinal = 19, type = FormFieldType.INPUTTEXT, validate = {Validator.require}, advance = true)
    public String maxBlockMs;


    @FormField(ordinal = 21, type = FormFieldType.ENUM, validate = {}, advance = true)
    public Boolean syncProducer;

    @FormField(ordinal = 22, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer}, advance = true)
    public Integer receiveBufferBytes;


    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        return null;
    }

//  @Override
//  public AirbyteConnectionStatus check(final JsonNode config) {
//    try {
//      final String testTopic = config.has("test_topic") ? config.get("test_topic").asText() : "";
//      if (!testTopic.isBlank()) {
//        final KafkaDestinationConfig kafkaDestinationConfig = KafkaDestinationConfig.getKafkaDestinationConfig(config);
//        final KafkaProducer<String, JsonNode> producer = kafkaDestinationConfig.getProducer();
//        final String key = UUID.randomUUID().toString();
//        final JsonNode value = Jsons.jsonNode(ImmutableMap.of(
//            COLUMN_NAME_AB_ID, key,
//            COLUMN_NAME_STREAM, "test-topic-stream",
//            COLUMN_NAME_EMITTED_AT, System.currentTimeMillis(),
//            COLUMN_NAME_DATA, Jsons.jsonNode(ImmutableMap.of("test-key", "test-value"))));
//
//        final RecordMetadata metadata = producer.send(new ProducerRecord<>(
//            namingResolver.getIdentifier(testTopic), key, value)).get();
//        producer.flush();
//
//        LOGGER.info("Successfully connected to Kafka brokers for topic '{}'.", metadata.topic());
//      }
//      return new AirbyteConnectionStatus().withStatus(Status.SUCCEEDED);
//    } catch (final Exception e) {
//      LOGGER.error("Exception attempting to connect to the Kafka brokers: ", e);
//      return new AirbyteConnectionStatus()
//          .withStatus(Status.FAILED)
//          .withMessage("Could not connect to the Kafka brokers with provided configuration. \n" + e.getMessage());
//    }
//  }


//    public void getConsumer() {
//        return new KafkaRecordConsumer(KafkaProducerFactory.getKafkaConfig(this),
//                catalog,
//                outputRecordCollector,
//                null);
//    }

    public Map<String, Object> buildKafkaConfig() {
        return buildKafkaConfig(false);
    }

    public Map<String, Object> buildKafkaConfig(boolean isTest) {
        final Map<String, Object> props = ImmutableMap.<String, Object>builder()
                .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                .putAll(protocol.propertiesByProtocol())
                .put(ProducerConfig.CLIENT_ID_CONFIG,
                        StringUtils.isNotBlank(clientId) ? clientId : StringUtils.EMPTY)
                .put(ProducerConfig.ACKS_CONFIG, acks)
                .put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, (enableIdempotence))
                .put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType)
                .put(ProducerConfig.BATCH_SIZE_CONFIG, (batchSize))
                .put(ProducerConfig.LINGER_MS_CONFIG, lingerMs)
                .put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection)
                .put(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG, clientDnsLookup)
                .put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory)
                .put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize)
                .put(ProducerConfig.RETRIES_CONFIG, retries)
                .put(ProducerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, socketConnectionSetupTimeoutMs)
                .put(ProducerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG, socketConnectionSetupTimeoutMaxMs)
                .put(ProducerConfig.MAX_BLOCK_MS_CONFIG, isTest ? "3000" : this.maxBlockMs)
                .put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs)
                .put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs)
                .put(ProducerConfig.SEND_BUFFER_CONFIG, sendBufferBytes)
                .put(ProducerConfig.RECEIVE_BUFFER_CONFIG, receiveBufferBytes)
//                .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName())
//                .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName())//
                .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName())//   .StringSerializer.class.getName())
                .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName())// JsonSerializer.class.getName())
                .build();
        return props.entrySet().stream()
                .filter(entry -> entry.getValue() != null && StringUtils.isNotBlank(entry.getValue().toString()))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> String.valueOf(e.getValue())));
    }

//  public static void main(final String[] args) throws Exception {
//    final Destination destination = new KafkaDestination();
//    LOGGER.info("Starting destination: {}", KafkaDestination.class);
//    new IntegrationRunner(destination).run(args);
//    LOGGER.info("Completed destination: {}", KafkaDestination.class);
//  }


    @TISExtension
    public static class DefaultDescriptor extends BaseDataxWriterDescriptor implements DataxWriter.IRewriteSuFormProperties {

        private transient SuFormProperties rewriteSubFormProperties;

        public DefaultDescriptor() {
            super();
        }

        @Override
        public SuFormProperties overwriteSubPluginFormPropertyTypes(SuFormProperties subformProps) throws Exception {

            if (rewriteSubFormProperties != null) {
                return rewriteSubFormProperties;
            }

            Descriptor<SelectedTab> newSubDescriptor = getRewriterSelectTabDescriptor();
            rewriteSubFormProperties = SuFormProperties.copy(
                    filterFieldProp(buildPropertyTypes(Optional.of(newSubDescriptor), newSubDescriptor.clazz))
                    , newSubDescriptor.clazz
                    , newSubDescriptor
                    , subformProps);
            return rewriteSubFormProperties;
        }

        @Override
        public Descriptor<SelectedTab> getRewriterSelectTabDescriptor() {
            Class targetClass = KafkaSelectedTab.class;
            return Objects.requireNonNull(TIS.get().getDescriptor(targetClass)
                    , "subForm clazz:" + targetClass + " can not find relevant Descriptor");
        }


        @Override
        public boolean isRdbms() {
            return false;
        }

        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public boolean isSupportBatch() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.Kafka;
        }


        @Override
        public String getDisplayName() {
            return getEndType().name();
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

            DataXKafkaWriter dataxWriter
                    = (DataXKafkaWriter) postFormVals.newInstance(this, msgHandler);
            if (StringUtils.isEmpty(dataxWriter.testTopic)) {
                msgHandler.addFieldError(context, "testTopic", ValidatorCommons.MSG_EMPTY_INPUT_ERROR);
                return false;
            }
            KafkaProducer<byte[], byte[]> producer = null;
            try {
                final String testTopic = dataxWriter.testTopic; //config.has("test_topic") ? config.get("test_topic").asText() : "";

                final KafkaProducerFactory kafkaFactory = KafkaProducerFactory.getKafkaConfig(dataxWriter, true);
                producer = kafkaFactory.getProducer();

                final String key = UUID.randomUUID().toString();
                final Map<String, Object> value = createRowVals("test", DTO.EventType.ADD
                        , ImmutableMap.of("test-key", "test-value"));

                final RecordMetadata metadata = producer.send(new ProducerRecord<>(
                        testTopic, key.getBytes(TisUTF8.get())
                        , JsonUtil.toString(value).getBytes(TisUTF8.get()))).get();
                producer.flush();

                LOGGER.info("Successfully connected to Kafka brokers for topic '{}'.", metadata.topic());

                return true;

            } catch (final Exception e) {

                if (ExceptionUtils.indexOfThrowable(e, org.apache.kafka.common.errors.TimeoutException.class) > -1) {
                    throw TisException.create("Kafka服务端连接超时，请检查相关配置是否正确", e);
                }

                LOGGER.error("Exception attempting to connect to the Kafka brokers: ", e);
                msgHandler.addErrorMessage(context, "Could not connect to the Kafka brokers with provided configuration. \n" + e.getMessage());

                return false;
            } finally {
                try {
                    if (producer != null) {
                        producer.close();
                    }
                } catch (Throwable e) {
                }
            }

        }
    }

    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_TABLE_NAME = "tableName";
    public static final String COLUMN_NAME_EMITTED_AT = "occure_time";
    public static final String COLUMN_NAME_DATA = "data";
    public static final String COLUMN_EVENT_TYPE = "event";

    public static Map<String, Object> createRowVals(String tableName, DTO.EventType event, Map<String, Object> data) {
        final String key = UUID.randomUUID().toString();
        final Map<String, Object> value = (ImmutableMap.of(
                COLUMN_NAME_ID, key,
                COLUMN_EVENT_TYPE, event.getCollapse(),
                COLUMN_TABLE_NAME, tableName,
                COLUMN_NAME_EMITTED_AT, System.currentTimeMillis(),
                COLUMN_NAME_DATA, data));
        return value;
    }


    public static void main(String[] args) {

    }


}
