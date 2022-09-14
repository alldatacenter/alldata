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

package org.apache.inlong.sort.redis.common.descriptor;

import org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator;

/**
 * InLong redis validator expand from {@link RedisValidator}
 */
public class InlongRedisValidator extends RedisValidator {

    /**
     * The standalone of redis deploy mode
     */
    public static final String REDIS_STANDALONE = "standalone";
    /**
     * Redis additional key used in hash or sorted-set
     */
    public static final String REDIS_ADDITIONAL_KEY = "additional.key";
}
