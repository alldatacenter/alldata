/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metastore.hive;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
@Qualifier(value = "metastoreSvc")
@CacheConfig(cacheNames = "hive", keyGenerator = "cacheKeyGenerator")
public class HiveMetaStoreServiceImpl implements HiveMetaStoreService {

    private static final Logger LOGGER = LoggerFactory
        .getLogger(HiveMetaStoreService.class);

    @Autowired(required = false)
    private IMetaStoreClient client = null;

    @Value("${hive.metastore.dbname}")
    private String defaultDbName;


    public HiveMetaStoreServiceImpl() {
    }

    public void setClient(IMetaStoreClient client) {
        this.client = client;
    }

    @Override
    @Cacheable(unless = "#result==null")
    public Iterable<String> getAllDatabases() {
        Iterable<String> results = null;
        try {
            if (client == null) {
                LOGGER.warn("Hive client is null. " +
                    "Please check your hive config.");
                return new ArrayList<>();
            }
            results = client.getAllDatabases();
        } catch (Exception e) {
            reconnect();
            LOGGER.error("Can not get databases : {}", e);
        }
        return results;
    }


    @Override
    @Cacheable(unless = "#result==null")
    public Iterable<String> getAllTableNames(String dbName) {
        Iterable<String> results = null;
        try {
            if (client == null) {
                LOGGER.warn("Hive client is null. " +
                    "Please check your hive config.");
                return new ArrayList<>();
            }
            results = client.getAllTables(getUseDbName(dbName));
        } catch (Exception e) {
            reconnect();
            LOGGER.error("Exception fetching tables info: {}", e);
            return null;
        }
        return results;
    }


    @Override
    @Cacheable(unless = "#result==null || #result.isEmpty()")
    public List<Table> getAllTable(String db) {
        return getTables(db);
    }

    @Override
    @Cacheable(unless = "#result==null || #result.isEmpty()")
    public Map<String, List<String>> getAllTableNames() {
        Map<String, List<String>> result = new HashMap<>();
        for (String dbName : getAllDatabases()) {
            result.put(dbName, Lists.newArrayList(getAllTableNames(dbName)));
        }
        return result;
    }

    @Override
    @Cacheable(unless = "#result==null")
    public Map<String, List<Table>> getAllTable() {
        Map<String, List<Table>> results = new HashMap<>();
        Iterable<String> dbs;
        // if hive.metastore.uris in application.properties configs wrong,
        // client will be injected failure and will be null.
        if (client == null) {
            LOGGER.warn("Hive client is null. Please check your hive config.");
            return results;
        }
        dbs = getAllDatabases();
        if (dbs == null) {
            return results;
        }
        for (String db : dbs) {
            // TODO: getAllTable() is not reusing caches of getAllTable(db) and vise versa
            // TODO: getTables() can return empty values on metastore exception
            results.put(db, getTables(db));
        }
        return results;
    }


    @Override
    @Cacheable(unless = "#result==null")
    public Table getTable(String dbName, String tableName) {
        Table result = null;
        try {
            if (client == null) {
                LOGGER.warn("Hive client is null. " +
                    "Please check your hive config.");
                return null;
            }
            result = client.getTable(getUseDbName(dbName), tableName);
        } catch (Exception e) {
            reconnect();
            LOGGER.error("Exception fetching table info : {}. {}", tableName,
                e);
        }
        return result;
    }

    @Scheduled(fixedRateString =
        "${cache.evict.hive.fixedRate.in.milliseconds}")
    @CacheEvict(
        cacheNames = "hive",
        allEntries = true,
        beforeInvocation = true)
    public void evictHiveCache() {
        LOGGER.info("Evict hive cache");
        // TODO: calls within same bean are not cached -- this call is not populating anything
//        getAllTable();
//        LOGGER.info("After evict hive cache, " +
//                "automatically refresh hive tables cache.");
    }


    private List<Table> getTables(String db) {
        String useDbName = getUseDbName(db);
        List<Table> allTables = new ArrayList<>();
        try {
            if (client == null) {
                LOGGER.warn("Hive client is null. " +
                    "Please check your hive config.");
                return allTables;
            }
            Iterable<String> tables = client.getAllTables(useDbName);
            for (String table : tables) {
                Table tmp = client.getTable(db, table);
                allTables.add(tmp);
            }
        } catch (Exception e) {
            reconnect();
            LOGGER.error("Exception fetching tables info: {}", e);
        }
        return allTables;
    }

    private String getUseDbName(String dbName) {
        if (!StringUtils.hasText(dbName)) {
            return defaultDbName;
        } else {
            return dbName;
        }
    }

    private void reconnect() {
        try {
            client.reconnect();
        } catch (Exception e) {
            LOGGER.error("reconnect to hive failed: {}", e);
        }
    }
}
