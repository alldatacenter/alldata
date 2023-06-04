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
import java.util.Set;

/**
 * A manager to maintain different type of fetchers
 */
public abstract class TopicManager implements Cleanable {

    protected ClientContext context;
    protected QueryConsumeConfig queryConsumeConfig;

    public TopicManager(ClientContext context, QueryConsumeConfig queryConsumeConfig) {
        this.context = context;
        this.queryConsumeConfig = queryConsumeConfig;
    }

    /**
     * Add topic and return the fetcher that maintain this topic.
     * @param topic Topic to be consumed.
     * @return The fetcher that maintain this topic.
     */
    public abstract TopicFetcher addTopic(InLongTopic topic);

    /**
     * Remove topic and return the fetcher that has maintained this topic.
     * @param topic Topic to be removed.
     * @param closeFetcher Should close this fetcher or not.
     * @return The fetcher that has maintained this topic.
     */
    public abstract TopicFetcher removeTopic(InLongTopic topic, boolean closeFetcher);

    /**
     * Get the specified fetcher by the given fetch key.
     * @param fetchKey Unique fetch key.
     * @return Related fetcher.
     */
    public abstract TopicFetcher getFetcher(String fetchKey);

    /**
     * Get all fetchers.
     * @return All fetchers.
     */
    public abstract Collection<TopicFetcher> getAllFetchers();

    /**
     * Get all topics that under this manager.
     * @return All topics.
     */
    public abstract Set<String> getManagedInLongTopics();

    /**
     * Offline all topics and their partitions if they exist.
     */
    public abstract void offlineAllTopicsAndPartitions();

    /**
     * Close manager.
     */
    public abstract void close();
}
