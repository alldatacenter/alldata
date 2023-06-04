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

package org.apache.inlong.sort.cdc.mongodb.source.connection;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.sort.cdc.mongodb.source.config.MongoDBSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A MongoDB Client pools.
 * Copy from com.ververica:flink-connector-mongodb-cdc:2.3.0.
 */
public class MongoClientPool {

    private static final Logger LOG = LoggerFactory.getLogger(MongoClientPool.class);

    private static final MongoClientPool INSTANCE = new MongoClientPool();
    private final Map<String, MongoClient> pools = new ConcurrentHashMap<>();

    private MongoClientPool() {
    }

    public static MongoClientPool getInstance() {
        return INSTANCE;
    }

    public MongoClient getOrCreateMongoClient(MongoDBSourceConfig sourceConfig) {
        return pools.computeIfAbsent(
                sourceConfig.getConnectionString(),
                rawConnectionString -> {
                    ConnectionString connectionString = new ConnectionString(rawConnectionString);
                    LOG.info(
                            "Create and register mongo client {}@{}",
                            connectionString.getUsername(),
                            connectionString.getHosts());
                    return MongoClients.create(connectionString);
                });
    }
}
