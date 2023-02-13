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

package org.apache.inlong.sdk.sort.fetcher.tube;

import org.apache.inlong.sdk.sort.api.AbstractTopicFetcherBuilder;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.api.TopicFetcherBuilder;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;
import org.apache.inlong.sdk.sort.interceptor.MsgTimeInterceptor;

import java.util.Collection;
import java.util.Optional;

/**
 * Builder of tube single topic fetcher.
 */
public class TubeTopicFetcherBuilderImpl extends AbstractTopicFetcherBuilder {

    private TubeConsumerCreator tubeConsumerCreator;

    public TubeTopicFetcherBuilderImpl tubeConsumerCreater(TubeConsumerCreator tubeConsumerCreator) {
        this.tubeConsumerCreator = tubeConsumerCreator;
        return this;
    }

    @Override
    public TopicFetcherBuilder topic(Collection<InLongTopic> topics) {
        throw new IllegalArgumentException("tube topic fetcher do not support multi topics, "
                + "plz call the single topic method");
    }

    @Override
    public TopicFetcher subscribe() {
        Optional.ofNullable(topic)
                .orElseThrow(() -> new IllegalStateException("subscribe tube single topic, but never assign topic"));
        Optional.ofNullable(context)
                .orElseThrow(() -> new IllegalStateException("context is null"));
        Optional.ofNullable(tubeConsumerCreator)
                .orElseThrow(() -> new IllegalStateException("tube consumer creator is null"));
        interceptor = Optional.ofNullable(interceptor).orElse(new MsgTimeInterceptor());
        interceptor.configure(topic);
        deserializer = Optional.ofNullable(deserializer).orElse(new MessageDeserializer());
        TubeSingleTopicFetcher fetcher =
                new TubeSingleTopicFetcher(topic, context, interceptor, deserializer, tubeConsumerCreator);
        if (!fetcher.init()) {
            throw new IllegalStateException("init tube single topic fetcher failed");
        }
        return fetcher;
    }
}
