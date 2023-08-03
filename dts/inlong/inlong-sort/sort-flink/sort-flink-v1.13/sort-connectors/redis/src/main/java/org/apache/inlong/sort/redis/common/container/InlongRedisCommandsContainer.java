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

package org.apache.inlong.sort.redis.common.container;

import org.apache.flink.streaming.connectors.redis.common.container.RedisCommandsContainer;

/**
 * The container interface expand from {@link RedisCommandsContainer}
 */
public interface InlongRedisCommandsContainer extends RedisCommandsContainer {

    /**
     * Delete value from specified key.
     *
     * @param key the key  to be delete
     */
    void del(String key);

    /**
     * Delete field in the hash stored .
     *
     * @param key Hash name
     * @param hashField Hash field
     */
    void hdel(String key, String hashField);

    /**
     * Get value from specified key
     *
     * @param key The specified key
     * @return The value of specified key
     */
    String get(String key);

    /**
     * Get value from specified key with hashField
     *
     * @param key The specified key
     * @param hashField The hash field
     * @return The value of specified key
     */
    String hget(String key, String hashField);

    /**
     * Get value from specified key with hashField
     *
     * @param key The specified key
     * @param member The member of sorted-set
     * @return The value of specified key
     */
    Double zscore(String key, String member);

    /**
     * Get value from specified key with member
     *
     * @param key The specified key
     * @param member The member of sorted-set
     * @return The value of specified key with member
     */
    Long zrevrank(String key, String member);

    void setBit(String key, Long offset, Boolean value);
}
