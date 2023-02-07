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

import org.apache.inlong.sdk.sort.entity.InLongTopic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract Builder of topic fetcher
 */
public abstract class AbstractTopicFetcherBuilder implements TopicFetcherBuilder {

    protected Interceptor interceptor;
    protected Deserializer deserializer;
    protected ClientContext context;
    protected InLongTopic topic;
    protected List<InLongTopic> topics;
    protected String fetchKey;

    @Override
    public TopicFetcherBuilder interceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    @Override
    public TopicFetcherBuilder topic(InLongTopic topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public TopicFetcherBuilder topic(Collection<InLongTopic> topics) {
        this.topics = new ArrayList<>(topics);
        return this;
    }

    @Override
    public TopicFetcherBuilder deserializer(Deserializer deserializer) {
        this.deserializer = deserializer;
        return this;
    }

    @Override
    public TopicFetcherBuilder context(ClientContext context) {
        this.context = context;
        return this;
    }

    @Override
    public TopicFetcherBuilder fetchKey(String fetchKey) {
        this.fetchKey = fetchKey;
        return this;
    }

}
