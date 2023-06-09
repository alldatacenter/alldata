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

package com.qlangtech.async.message.client.kafka;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;

import java.util.Optional;

/**
 * @author: baisui 百岁
 * @create: 2020-12-09 11:43
 **/
@Public
public class TiKVKafkaMQListenerFactory extends MQListenerFactory {
//    public static void main(String[] args) {
//        List<KafkaMessage> kafkaMessages = Lists.newArrayList();
//        TicdcEventFilter filter = new TicdcEventFilter();
//        for (KafkaMessage kafkaMessage : kafkaMessages) {
//            parseKafkaMessage(filter, kafkaMessage);
//        }
//
//    }

//    public static final String MQ_ADDRESS_COLLECTION = "192.168.28.201:9092";			//kafka地址
//    public static final String CONSUMER_TOPIC = "baisui";						//消费者连接的topic
//    // public static final String PRODUCER_TOPIC = "topicDemo";						//生产者连接的topic
//    public static final String CONSUMER_GROUP_ID = "1";

    @FormField(ordinal = 0, validate = {Validator.require, Validator.host})
    public String mqAddress;
    @FormField(ordinal = 1, validate = {Validator.require, Validator.identity})
    public String topic;
    @FormField(ordinal = 2, validate = {Validator.require, Validator.identity})
    public String groupId;
    @FormField(ordinal = 3, type = FormFieldType.ENUM, validate = {Validator.require})
    //earliest,latest,和none
    public String offsetResetStrategy;


    @Override
    public IMQConsumerStatus createConsumerStatus() {
        return null;
    }

    @Override
    public IMQListener create() {
        KafkaMQListener mqListener = new KafkaMQListener(this);
        return mqListener;
    }

    @TISExtension()
    public static class DefaultDescriptor extends BaseDescriptor {
        @Override
        public String getDisplayName() {
            return "TiCDC-Kafka";
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }

        @Override
        public IEndTypeGetter.EndType getEndType() {
            return IEndTypeGetter.EndType.TiDB;
        }

        @Override
        public Optional<IEndTypeGetter.EndType> getTargetType() {
            return Optional.empty();
        }
    }
}
