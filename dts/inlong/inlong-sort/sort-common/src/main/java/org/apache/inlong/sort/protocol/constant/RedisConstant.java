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

package org.apache.inlong.sort.protocol.constant;

/**
 * Redis option constant
 */
public class RedisConstant {

    /**
     * Connector key
     */
    public static final String CONNECTOR_KEY = "connector";
    /**
     * Redis connector name
     */
    public static final String CONNECTOR = "redis-inlong";
    /**
     * Redis command
     */
    public static final String COMMAND = "command";
    /**
     * Redis deploy mode, contains [standalone|cluster|sentinel]
     */
    public static final String REDIS_MODE = "redis-mode";
    /**
     * Redis cluster nodes used in cluster mode
     */
    public static final String CLUSTER_NODES = "cluster-nodes";
    /**
     * Redis master name used in sentinel mode
     */
    public static final String MASTER_NAME = "master.name";
    /**
     * Redis sentinels info used in sentinel mode
     */
    public static final String SENTINELS_INFO = "sentinels.info";
    /**
     * Redis database used in [standalone|sentinel] mode
     */
    public static final String DATABASE = "database";
    /**
     * Redis host used in standalone mode
     */
    public static final String HOST = "host";
    /**
     * Redis port used in standalone mode
     */
    public static final String PORT = "port";
    /**
     * Additional key used in [HSET|Sorted-Set] of redis data type
     */
    public static final String ADDITIONAL_KEY = "additional.key";
    /**
     * Password  for connect to redis
     */
    public static final String PASSWORD = "password";
    /**
     * Timeout for connect to redis
     */
    public static final String TIMEOUT = "timeout";
    /**
     * Sockt timeout for connect to redis
     */
    public static final String SOTIMEOUT = "soTimeout";
    /**
     * Max total for connect to redis
     */
    public static final String MAXTOTAL = "maxTotal";
    /**
     * Max idle for connect to redis
     */
    public static final String MAXIDLE = "maxIdle";
    /**
     * Min idle for connect to redis
     */
    public static final String MINIDLE = "minIdle";
    /**
     * Cache max rows for lookup
     */
    public static final String LOOKUP_CACHE_MAX_ROWS = "lookup.cache.max-rows";
    /**
     * Cache ttl for lookup
     */
    public static final String LOOKUP_CACHE_TTL = "lookup.cache.ttl";
    /**
     * Max retries for lookup
     */
    public static final String LOOKUP_MAX_RETRIES = "lookup.max-retries";
}
