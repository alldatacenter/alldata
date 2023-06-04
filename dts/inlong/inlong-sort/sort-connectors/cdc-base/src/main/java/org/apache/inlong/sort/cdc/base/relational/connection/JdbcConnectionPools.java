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

package org.apache.inlong.sort.cdc.base.relational.connection;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.sort.cdc.base.config.JdbcSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Jdbc Connection pools implementation.
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 * */
public class JdbcConnectionPools implements ConnectionPools<HikariDataSource, JdbcSourceConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcConnectionPools.class);

    private static JdbcConnectionPools instance;
    private final Map<ConnectionPoolId, HikariDataSource> pools = new HashMap<>();
    private static JdbcConnectionPoolFactory jdbcConnectionPoolFactory;

    private JdbcConnectionPools() {
    }

    public static synchronized JdbcConnectionPools getInstance(JdbcConnectionPoolFactory jdbcConnectionPoolFactory) {
        if (instance == null) {
            jdbcConnectionPoolFactory = jdbcConnectionPoolFactory;
            instance = new JdbcConnectionPools();
        }
        return instance;
    }

    @Override
    public HikariDataSource getOrCreateConnectionPool(
            ConnectionPoolId poolId, JdbcSourceConfig sourceConfig) {
        synchronized (pools) {
            if (!pools.containsKey(poolId)) {
                LOG.info("Create and register connection pool {}", poolId);
                pools.put(poolId, jdbcConnectionPoolFactory.createPooledDataSource(sourceConfig));
            }
            return pools.get(poolId);
        }
    }
}
