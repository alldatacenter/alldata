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

import java.util.List;

/**
 * Interface of all type of topic fetchers.
 */
public interface TopicFetcher {

    /**
     * Init topic fetcher.
     * @return The result of init.
     */
    boolean init();

    /**
     * Ack message by the given msgOffset.
     * @param msgOffset Offset of message.
     * @throws Exception
     */
    void ack(String msgOffset) throws Exception;

    /**
     * Get the unique fetcher key to specify the fetcher who consume this message.
     * @return Message key.
     */
    String getFetchKey();

    /**
     * Pause the consuming
     */
    void pause();

    /**
     * Resume the consuming
     */
    void resume();

    /**
     * Close the consuming
     * @return Result of close
     */
    boolean close();

    /**
     * Get if the fetcher is closed or not.
     * @return Closed or not.
     */
    boolean isClosed();

    /**
     * Set stop consume flag.
     * @param stopConsume Stop consume flag.
     */
    void setStopConsume(boolean stopConsume);

    /**
     * Get stop consume flag.
     * @return stop consume flag.
     */
    boolean isStopConsume();

    /**
     * Get the topics maintain by current fetcher.
     * @return topic list.
     */
    List<InLongTopic> getTopics();

    /**
     * Update list of topics to fetcher.
     * @param topics Topics to be updated.
     * @return The result of update.
     */
    boolean updateTopics(List<InLongTopic> topics);
}
