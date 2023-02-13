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

package org.apache.inlong.sdk.sort.fetcher.pulsar;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.sdk.sort.api.AbstractTopicFetcherBuilder;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;
import org.apache.inlong.sdk.sort.interceptor.MsgTimeInterceptor;
import org.apache.pulsar.client.api.PulsarClient;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Builder of pulsar single topic fetcher.
 */
public class PulsarTopicFetcherBuilderImpl extends AbstractTopicFetcherBuilder {

    PulsarClient pulsarClient;

    public PulsarTopicFetcherBuilderImpl pulsarClient(PulsarClient pulsarClient) {
        this.pulsarClient = pulsarClient;
        return this;
    }

    @Override
    public TopicFetcher subscribe() {
        Optional.ofNullable(context)
                .orElseThrow(() -> new IllegalStateException("context is null"));
        Optional.ofNullable(pulsarClient)
                .orElseThrow(() -> new IllegalStateException("pulsar client is null"));
        deserializer = Optional.ofNullable(deserializer).orElse(new MessageDeserializer());
        if (CollectionUtils.isNotEmpty(topics)) {
            return subscribeMultiTopic();
        } else if (Objects.nonNull(topic)) {
            return subscribeSingleTopic();
        } else {
            throw new IllegalArgumentException("subscribe pulsar fetcher, but never assign any topic");
        }
    }

    private TopicFetcher subscribeSingleTopic() {
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(topic);
        TopicFetcher fetcher =
                new PulsarSingleTopicFetcher(topic, context, interceptor, deserializer, pulsarClient);
        if (!fetcher.init()) {
            throw new IllegalStateException("init pulsar single topic fetcher failed");
        }
        return fetcher;
    }

    private TopicFetcher subscribeMultiTopic() {
        InLongTopic firstTopic = topics.stream().findFirst().get();
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(firstTopic);
        String key = Optional.ofNullable(fetchKey)
                .orElse(firstTopic.getInLongCluster().getClusterId() + new Random().nextLong());
        TopicFetcher fetcher =
                new PulsarMultiTopicsFetcher(topics, context, interceptor, deserializer, pulsarClient, key);
        if (!fetcher.init()) {
            throw new IllegalStateException("init pulsar multi topic fetcher failed");
        }
        return fetcher;
    }

}
