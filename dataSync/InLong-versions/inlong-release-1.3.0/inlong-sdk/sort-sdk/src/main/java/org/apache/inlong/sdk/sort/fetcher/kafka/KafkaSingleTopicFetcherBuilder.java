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
 *
 */

package org.apache.inlong.sdk.sort.fetcher.kafka;

import org.apache.inlong.sdk.sort.api.SingleTopicFetcherBuilder;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;
import org.apache.inlong.sdk.sort.interceptor.MsgTimeInterceptor;

import java.util.Optional;

/**
 * Builder of kafka single topic fetcher.
 */
public class KafkaSingleTopicFetcherBuilder extends SingleTopicFetcherBuilder {

    private String bootstrapServers;

    public KafkaSingleTopicFetcherBuilder bootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    @Override
    public TopicFetcher subscribe() {
        Optional.ofNullable(topic)
                .orElseThrow(() -> new IllegalStateException("subscribe kafka single topic, but never assign topic"));
        Optional.ofNullable(context)
                .orElseThrow(() -> new IllegalStateException("context is null"));
        Optional.ofNullable(bootstrapServers)
                .orElseThrow(() -> new IllegalStateException("kafka bootstrapServers is null"));
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(topic);
        deserializer = Optional.ofNullable(deserializer).orElse(new MessageDeserializer());
        TopicFetcher kafkaSingleTopicFetcher =
                new KafkaSingleTopicFetcher(topic, context, interceptor, deserializer, bootstrapServers);
        if (!kafkaSingleTopicFetcher.init()) {
            throw new IllegalStateException("init kafka single topic fetcher failed");
        }
        return kafkaSingleTopicFetcher;
    }

}
