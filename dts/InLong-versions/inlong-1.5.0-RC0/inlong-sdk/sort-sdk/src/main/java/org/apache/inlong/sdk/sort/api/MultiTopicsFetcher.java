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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Basic class of multi topic fetchers.
 * The main differences between this and {@link SingleTopicFetcher} is that:
 * 1. MultiTopicFetcher maintains a list of topics while {@link SingleTopicFetcher} only maintains one;
 * 2. All topics share the same properties;
 * 3. The joining and removing of topics will result in new creation of consumer,
 *      and the old ones will be put in a list, waiting to be cleaned by a scheduled thread.
 */
public abstract class MultiTopicsFetcher implements TopicFetcher {

    protected final ReentrantReadWriteLock mainLock = new ReentrantReadWriteLock(true);
    protected final ScheduledExecutorService executor;
    protected final String fetchKey;
    protected Map<String, InLongTopic> onlineTopics;
    protected List<InLongTopic> newTopics;
    protected ClientContext context;
    protected Deserializer deserializer;
    protected volatile Thread fetchThread;
    protected volatile boolean closed = false;
    protected volatile boolean stopConsume = false;
    // use for empty topic to sleep
    protected long sleepTime = 0L;
    protected int emptyFetchTimes = 0;
    // for rollback
    protected Interceptor interceptor;
    protected Seeker seeker;

    public MultiTopicsFetcher(
            List<InLongTopic> topics,
            ClientContext context,
            Interceptor interceptor,
            Deserializer deserializer,
            String fetchKey) {
        this.onlineTopics = topics.stream()
                .filter(Objects::nonNull)
                .collect((Collectors.toMap(InLongTopic::getTopic, t -> t)));
        this.context = context;
        this.interceptor = interceptor;
        this.deserializer = deserializer;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.fetchKey = fetchKey;
    }

    @Override
    public String getFetchKey() {
        return fetchKey;
    }

    protected boolean needUpdate(Collection<InLongTopic> newTopics) {
        if (newTopics.size() != onlineTopics.size()) {
            return true;
        }
        // all topic should share the same properties in one task
        if (Objects.equals(newTopics.stream().findFirst(), onlineTopics.values().stream().findFirst())) {
            return true;
        }
        for (InLongTopic topic : newTopics) {
            if (!onlineTopics.containsKey(topic.getTopic())) {
                return true;
            }
        }
        return false;
    }

}
