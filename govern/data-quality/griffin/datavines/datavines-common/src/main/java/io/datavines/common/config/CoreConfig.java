/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.config;

public class CoreConfig {

    public static final String COORDINATOR_ACTIVE_DIR = "/datavines/master/active";

    public static final String JOB_RESPONSE_CACHE_LOCK_PATH = "/datavines/job/lock";

    public static final String JOB_RESPONSE_CACHE_PATH = "/datavines/executor/response/cache";

    public static final String JOB_COORDINATOR_RESPONSE_CACHE_LOCK_PATH = "/datavines/job/lock/coordinator";

    /**
     * default log cache rows num,output when reach the number
     */
    public static final String LOG_CACHE_ROW_NUM = "log.cache.row.num";

    public static final int LOG_CACHE_ROW_NUM_DEFAULT_VALUE = 4 * 16;

    public static final String LOG_FLUSH_INTERVAL = "log.flush.row.num";

    public static final int LOG_FLUSH_INTERVAL_DEFAULT_VALUE = 1000;

}
