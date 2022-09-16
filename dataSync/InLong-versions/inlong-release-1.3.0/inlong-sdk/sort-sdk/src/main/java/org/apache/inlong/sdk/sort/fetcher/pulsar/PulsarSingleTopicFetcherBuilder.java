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

package org.apache.inlong.sdk.sort.fetcher.pulsar;

import org.apache.inlong.sdk.sort.api.SingleTopicFetcherBuilder;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;
import org.apache.inlong.sdk.sort.interceptor.MsgTimeInterceptor;
import org.apache.pulsar.client.api.PulsarClient;

import java.util.Optional;

/**
 * Builder of pulsar single topic fetcher.
 */
public class PulsarSingleTopicFetcherBuilder extends SingleTopicFetcherBuilder {

    PulsarClient pulsarClient;

    public PulsarSingleTopicFetcherBuilder pulsarClient(PulsarClient pulsarClient) {
        this.pulsarClient = pulsarClient;
        return this;
    }

    @Override
    public TopicFetcher subscribe() {
        Optional.ofNullable(topic)
                .orElseThrow(() -> new IllegalStateException("subscribe pulsar single topic, but never assign topic"));
        Optional.ofNullable(context)
                .orElseThrow(() -> new IllegalStateException("context is null"));
        Optional.ofNullable(pulsarClient)
                .orElseThrow(() -> new IllegalStateException("pulsar client is null"));
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(topic);
        deserializer = Optional.ofNullable(deserializer).orElse(new MessageDeserializer());
        PulsarSingleTopicFetcher fetcher =
                new PulsarSingleTopicFetcher(topic, context, interceptor, deserializer, pulsarClient);
        if (!fetcher.init()) {
            throw new IllegalStateException("init pulsar single topic fetcher failed");
        }
        return fetcher;
    }

}
