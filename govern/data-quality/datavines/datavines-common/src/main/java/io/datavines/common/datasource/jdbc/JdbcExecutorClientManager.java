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
package io.datavines.common.datasource.jdbc;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JdbcDataSourceManager
 */
@Slf4j
public class JdbcExecutorClientManager {

    private final ConcurrentHashMap<String, JdbcExecutorClient> clientMap = new ConcurrentHashMap<>();

    private static final class Singleton {
        private static final JdbcExecutorClientManager INSTANCE = new JdbcExecutorClientManager();
    }

    public static JdbcExecutorClientManager getInstance() {
        return Singleton.INSTANCE;
    }

    public JdbcExecutorClient getExecutorClient(BaseJdbcDataSourceInfo baseJdbcDataSourceInfo) {
        if (baseJdbcDataSourceInfo == null) {
            return null;
        }

        JdbcExecutorClient jdbcExecutorClient = clientMap.get(baseJdbcDataSourceInfo.getUniqueKey());

        if (jdbcExecutorClient == null) {
            jdbcExecutorClient = new JdbcExecutorClient(baseJdbcDataSourceInfo);
            clientMap.put(baseJdbcDataSourceInfo.getUniqueKey(), jdbcExecutorClient);
        }

        return jdbcExecutorClient;
    }
}
