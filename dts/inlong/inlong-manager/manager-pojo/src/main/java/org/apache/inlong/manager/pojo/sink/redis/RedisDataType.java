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

package org.apache.inlong.manager.pojo.sink.redis;

import java.util.Arrays;
import java.util.HashSet;

/**
 * The data type of Redis.
 */
public enum RedisDataType {

    HASH(
            RedisSchemaMapMode.DYNAMIC,
            RedisSchemaMapMode.STATIC_KV_PAIR,
            RedisSchemaMapMode.STATIC_PREFIX_MATCH),
    BITMAP(
            RedisSchemaMapMode.DYNAMIC),
    PLAIN(
            RedisSchemaMapMode.STATIC_PREFIX_MATCH);

    private final HashSet<RedisSchemaMapMode> mapModes;

    private RedisDataType(RedisSchemaMapMode... modes) {
        this.mapModes = new HashSet<>(Arrays.asList(modes));
    }

    public HashSet<RedisSchemaMapMode> getMapModes() {
        return mapModes;
    }
}
