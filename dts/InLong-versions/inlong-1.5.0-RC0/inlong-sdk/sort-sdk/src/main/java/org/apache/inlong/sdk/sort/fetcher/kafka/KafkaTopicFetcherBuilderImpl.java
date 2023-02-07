/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.sort.fetcher.kafka;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.sdk.sort.api.AbstractTopicFetcherBuilder;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;
import org.apache.inlong.sdk.sort.interceptor.MsgTimeInterceptor;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Builder of kafka topic fetcher.
 */
public class KafkaTopicFetcherBuilderImpl extends AbstractTopicFetcherBuilder {

    private String bootstrapServers;

    public KafkaTopicFetcherBuilderImpl bootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    @Override
    public TopicFetcher subscribe() {
        Optional.ofNullable(context)
                .orElseThrow(() -> new IllegalStateException("context is null"));
        Optional.ofNullable(bootstrapServers)
                .orElseThrow(() -> new IllegalStateException("kafka bootstrapServers is null"));
        deserializer = Optional.ofNullable(deserializer).orElse(new MessageDeserializer());
        if (CollectionUtils.isNotEmpty(topics)) {
            return subscribeMultiTopic();
        } else if (Objects.nonNull(topic)) {
            return subscribeSingleTopic();
        } else {
            throw new IllegalArgumentException("subscribe kafka fetcher, but never assign any topic");
        }
    }

    private TopicFetcher subscribeSingleTopic() {
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(topic);
        TopicFetcher kafkaSingleTopicFetcher =
                new KafkaSingleTopicFetcher(topic, context, interceptor, deserializer, bootstrapServers);
        if (!kafkaSingleTopicFetcher.init()) {
            throw new IllegalStateException("init kafka single topic fetcher failed");
        }
        return kafkaSingleTopicFetcher;
    }

    private TopicFetcher subscribeMultiTopic() {
        InLongTopic firstTopic = topics.stream().findFirst().get();
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(firstTopic);
        String key = Optional.ofNullable(fetchKey)
                .orElse(firstTopic.getInLongCluster().getClusterId() + new Random().nextLong());
        TopicFetcher kafkaMultiTopicFetcher =
                new KafkaMultiTopicsFetcher(topics, context, interceptor, deserializer, bootstrapServers, key);
        if (!kafkaMultiTopicFetcher.init()) {
            throw new IllegalStateException("init kafka multi topic fetcher failed");
        }
        return kafkaMultiTopicFetcher;
    }

}
