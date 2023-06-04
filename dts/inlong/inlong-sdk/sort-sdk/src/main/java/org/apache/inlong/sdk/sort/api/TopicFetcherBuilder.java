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

package org.apache.inlong.sdk.sort.api;

import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.fetcher.kafka.KafkaTopicFetcherBuilderImpl;
import org.apache.inlong.sdk.sort.fetcher.pulsar.PulsarTopicFetcherBuilderImpl;
import org.apache.inlong.sdk.sort.fetcher.tube.TubeTopicFetcherBuilderImpl;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;

import java.util.Collection;

/**
 * Interface of topic fetcher builder.
 */
public interface TopicFetcherBuilder {

    /**
     * Subscribe topics and build the {@link TopicFetcher}
     *
     * @return The prepared topic fetcher
     */
    TopicFetcher subscribe();

    /**
     * Specify the interceptor of TopicFetcher.
     * The interceptor is used to filter or modify the message fetched from MQ.
     * The default interceptor is {@link MessageInterceptor}.
     *
     * @param interceptor Interceptor
     * @return TopicFetcherBuilder
     */
    TopicFetcherBuilder interceptor(Interceptor interceptor);

    /**
     * Specify the topic to be subscribed.
     * Repeated call will replace the previous topic.
     *
     * @param topic Topic to be subscribed.
     * @return TopicFetcherBuilder
     */
    TopicFetcherBuilder topic(InLongTopic topic);

    /**
     * Specify the topics to be subscribed.
     * Repeated call will replace the previous topics.
     * <p>
     *     This method will removed the topic which is added though TopicFetcherBuilder::topic(InLongTopic topic)
     * </p>
     *
     * @param topics Topics to be subscribed.
     * @return TopicFetcherBuilder
     */
    TopicFetcherBuilder topic(Collection<InLongTopic> topics);

    /**
     * Specify the deserializer of fetcher.
     * Deserializer is used to decode the messages fetched from MQ, and arrange them to {@link InLongMessage} format.
     * The default deserializer is {@link MessageDeserializer}
     *
     * @param deserializer Deserializer.
     * @return TopicFetcherBuilder
     */
    TopicFetcherBuilder deserializer(Deserializer deserializer);

    /**
     * Specify the clientContext of topic fetcher
     *
     * @param context ClientContext.
     * @return TopicFetcherBuilder
     */
    TopicFetcherBuilder context(ClientContext context);

    /**
     * The fetchKey to specify that which one fetcher this message belongs to.
     * @param fetchKey Key of fetcher.
     * @return TopicFetcherBuilder
     */
    TopicFetcherBuilder fetchKey(String fetchKey);

    /**
     * Got a kafka topic fetcher builder
     * @return KafkaTopicFetcherBuilderImpl
     */
    static KafkaTopicFetcherBuilderImpl newKafkaBuilder() {
        return new KafkaTopicFetcherBuilderImpl();
    }

    /**
     * Got a pulsar topic fetcher builder
     * @return KafkaTopicFetcherBuilderImpl
     */
    static PulsarTopicFetcherBuilderImpl newPulsarBuilder() {
        return new PulsarTopicFetcherBuilderImpl();
    }

    /**
     * Got a tube topic fetcher builder
     * @return KafkaTopicFetcherBuilderImpl
     */
    static TubeTopicFetcherBuilderImpl newTubeBuilder() {
        return new TubeTopicFetcherBuilderImpl();
    }
}
