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

package com.qlangtech.tis.plugins.datax.rabbitmq.writer;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-25 19:57
 **/
public class DataXRabbitMQWriter extends DataxWriter {
    @FormField(ordinal = 7 , type = FormFieldType.INPUTTEXT, validate = {})
    public String username;
    @FormField(ordinal = 0 , type = FormFieldType.INPUTTEXT, validate = {})
    public String password;

    @FormField(ordinal = 1 , type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer port;
    @FormField(ordinal = 2 , type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String host;
    @FormField(ordinal = 5 , type = FormFieldType.ENUM, validate = {})
    public Boolean ssl;

    @FormField(ordinal = 3 , type = FormFieldType.INPUTTEXT, validate = {})
    public String exchange;

    @FormField(ordinal = 4 , type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String routingKey;



    @FormField(ordinal = 6 , type = FormFieldType.INPUTTEXT, validate = {})
    public String virtualHost;



    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        return null;
    }

    @TISExtension
    public static class DefaultDescriptor extends BaseDataxWriterDescriptor {


        public DefaultDescriptor() {
            super();
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
            return EndType.RabbitMQ;
        }


        @Override
        public String getDisplayName() {
            return getEndType().name();
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {


            return true;
//            DataXRabbitMQWriter dataxWriter
//                    = (DataXRabbitMQWriter) postFormVals.newInstance(this, msgHandler);
//            if (StringUtils.isEmpty(dataxWriter.testTopic)) {
//                msgHandler.addFieldError(context, "testTopic", ValidatorCommons.MSG_EMPTY_INPUT_ERROR);
//                return false;
//            }
//            KafkaProducer<byte[], byte[]> producer = null;
//            try {
//                final String testTopic = dataxWriter.testTopic; //config.has("test_topic") ? config.get("test_topic").asText() : "";
//
//                final KafkaProducerFactory kafkaFactory = KafkaProducerFactory.getKafkaConfig(dataxWriter, true);
//                producer = kafkaFactory.getProducer();
//
//                final String key = UUID.randomUUID().toString();
//                final Map<String, Object> value = createRowVals("test", DTO.EventType.ADD
//                        , ImmutableMap.of("test-key", "test-value"));
//
//                final RecordMetadata metadata = producer.send(new ProducerRecord<>(
//                        testTopic, key.getBytes(TisUTF8.get())
//                        , JsonUtil.toString(value).getBytes(TisUTF8.get()))).get();
//                producer.flush();
//
//                LOGGER.info("Successfully connected to Kafka brokers for topic '{}'.", metadata.topic());
//
//                return true;
//
//            } catch (final Exception e) {
//
//                if (ExceptionUtils.indexOfThrowable(e, org.apache.kafka.common.errors.TimeoutException.class) > -1) {
//                    throw TisException.create("Kafka服务端连接超时，请检查相关配置是否正确", e);
//                }
//
//                LOGGER.error("Exception attempting to connect to the Kafka brokers: ", e);
//                msgHandler.addErrorMessage(context, "Could not connect to the Kafka brokers with provided configuration. \n" + e.getMessage());
//
//                return false;
//            } finally {
//                try {
//                    if (producer != null) {
//                        producer.close();
//                    }
//                } catch (Throwable e) {
//                }
//            }

        }
    }

}
