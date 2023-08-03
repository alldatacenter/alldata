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

package org.apache.inlong.sort.redis.common.mapper;

import org.apache.flink.api.common.functions.Function;

import java.io.Serializable;
import java.util.Optional;

/**
 * Function that creates the description how the input data should be mapped to redis type.
 * Copy from {@link org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper}
 */
public interface RedisMapper<T> extends Function, Serializable {

    /**
     * Returns descriptor which defines data type.
     *
     * @return data type descriptor
     */
    RedisCommandDescription getCommandDescription();

    /**
     * Extracts key from data.
     *
     * @param data source data
     * @return key
     */
    default String getKeyFromData(T data) {
        return null;
    }

    /**
     * Extracts value from data.
     *
     * @param data source data
     * @return value
     */
    default String getValueFromData(T data) {
        return null;
    }

    /**
     * Extracts the additional key from data as an {@link Optional <String/>}.
     * The default implementation returns an empty Optional.
     *
     * @param data
     * @return Optional
     */
    default Optional<String> getAdditionalKey(T data) {
        return Optional.empty();
    }

    /**
     * Extracts the additional time to live (TTL) for data.
     * The default implementation returns an empty Optional.
     *
     * @param data
     * @return Optional
     */
    default Optional<Integer> getAdditionalTTL(T data) {
        return Optional.empty();
    }
}