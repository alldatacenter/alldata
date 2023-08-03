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

/**
 * The MappingMode between Sql Schema and {@link RedisDataType}.
 */
public enum SchemaMappingMode {

    /**
     * The DYNAMIC mode witch mapping a {@link java.util.Map} to {@link RedisDataType}.
     * There are two members in DYNAMIC mode: <br/>
     * the first member is Redis key. <br/>
     * the second member is a {@link java.util.Map} object, which will be iterated, <br/>
     * the entry key is redis key, and the entry value is redis value.
     */
    DYNAMIC,

    /**
     * The are at least two fields, the first member is redis key, <br/>
     * and each field from the second field represents a redis value.<br/>
     * ex: <br/>
     * <code>
     *   key, field, value1, value2, value3, [value]...
     * </code>
     */
    STATIC_PREFIX_MATCH,
    /**
     * There are two fields, the first field is key, <br/>
     * and the other fields are pairs of field and value. <br/>
     * ex:<br/>
     * <code>
     * key, field1, value1,field2,value2,field3,value3,[field,value]...
     * </code>
     */
    STATIC_KV_PAIR;
}
