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

package org.apache.inlong.sort.redis.common.config;

import org.apache.flink.annotation.Internal;

/**
 * Redis data type Enum.
 * See detail: @see <a href="https://redis.io/topics/data-types-intro">redis data type</a>
 */
@Internal
public enum RedisDataType {
    /**
     * Redis strings commands are used for managing string values in Redis.
     */
    PLAIN,
    /**
     * A Redis hash is a data type that represents a mapping between a string field and a string value.<br/>
     * There are two members in hash DataType: <br/>
     * the first member is Redis hash field.<br/>
     * the second member is Redis hash value.
     */
    HASH,
    /**
     * Redis Sets are an unordered collection of unique strings.
     * Unique means sets does not allow repetition of data in a key.
     */
    SET,

    /**
     * Bitmaps are not an actual data type, but a set of bit-oriented operations defined on the String type. <br/>
     * Since strings are binary safe blobs and their maximum length is 512 MB, <br/>
     * they are suitable to set up to 2^32 different bits.
     */
    BITMAP,
    ;
}
