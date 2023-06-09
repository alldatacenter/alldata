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

import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.function.Supplier;

public class KafkaProducerFactory {

    private final String topicPattern;
    private final boolean sync;
    private final KafkaProducer<byte[], byte[]> producer;


    private KafkaProducerFactory(final String topicPattern, final boolean sync, final DataXKafkaWriter config, boolean isTest) {
        this.topicPattern = topicPattern;
        this.sync = sync;

        this.producer = withCurrentThreadSetter(() -> {
            return new KafkaProducer<byte[], byte[]>(config.buildKafkaConfig(isTest));
        });
    }


    public static <T> T withCurrentThreadSetter(Supplier<T> instanceCreator) {
        final ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 创建KafkaProducer过程中，创建 org.apache.kafka.common.serialization.ByteArraySerializer需要使用当前的classloader，不然会加载不到
            Thread.currentThread().setContextClassLoader(KafkaProducerFactory.class.getClassLoader());
            return instanceCreator.get();
        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }
    }


    public static KafkaProducerFactory getKafkaConfig(final DataXKafkaWriter config, boolean isTest) {
        return new KafkaProducerFactory(
                config.topic,
                config.syncProducer,
                config, isTest);
    }

    //
//
//    public String getTopicPattern() {
//        return topicPattern;
//    }
//
//    public boolean isSync() {
//        return sync;
//    }
//
    public KafkaProducer<byte[], byte[]> getProducer() {
        return this.producer;
    }

}
