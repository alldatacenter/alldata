/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hive.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.schema.Schema;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade which encapsulates caching and provides centralized API to access cached values.
 */
final class HiveMetadataCache {

  private static final Logger logger = LoggerFactory.getLogger(HiveMetadataCache.class);

  /**
   * Storage plugin option for altering cached values expiration time.
   * See {@link #getHmsCacheTTL(HiveConf)} for details.
   */
  private static final String HIVE_METASTORE_CACHE_TTL = "hive.metastore.cache-ttl-seconds";

  /**
   * Storage plugin option for altering cache expiration policy.
   * See {@link #isExpireAfterWrite(HiveConf)} for details.
   */
  private static final String HIVE_METASTORE_CACHE_EXPIRE = "hive.metastore.cache-expire-after";

  /**
   * One of possible values for option {@link #HIVE_METASTORE_CACHE_EXPIRE}.
   * See {@link #isExpireAfterWrite(HiveConf)} for details.
   */
  private static final String HIVE_METASTORE_CACHE_EXPIRE_AFTER_ACCESS = "access";

  /**
   * Cache of Hive database names which contains only one key ({@link #getDbNames()})
   * mapped to list of names.
   */
  private final LoadingCache<String, List<String>> dbNamesCache;

  /**
   * Cache where each key is name of database and value is map
   * where each key is table or view name and value indicates type
   * {@link Schema.TableType#TABLE} or {@link Schema.TableType#VIEW}
   * accordingly.
   */
  private final LoadingCache<String, Map<String, Schema.TableType>> tableNamesCache;

  /**
   * Cache where key is combination of db and table names and value
   * is entry containing Hive table metadata.
   */
  private final LoadingCache<TableName, HiveReadEntry> tableEntryCache;

  HiveMetadataCache(DrillHiveMetaStoreClient client, HiveConf hiveConf) {
    final CacheBuilder<Object, Object> cacheBuilder = isExpireAfterWrite(hiveConf)
        ? CacheBuilder.newBuilder().expireAfterWrite(getHmsCacheTTL(hiveConf), TimeUnit.SECONDS)
        : CacheBuilder.newBuilder().expireAfterAccess(getHmsCacheTTL(hiveConf), TimeUnit.SECONDS);

    dbNamesCache = cacheBuilder.build(new DatabaseNameCacheLoader(client));
    tableNamesCache = cacheBuilder.build(new TableNameCacheLoader(client));
    tableEntryCache = cacheBuilder.build(new TableEntryCacheLoader(client));
  }

  /**
   * List db names defined in Hive.
   *
   * @return names of databases defined in Hive
   * @throws TException when loading failed
   */
  List<String> getDbNames() throws TException {
    try {
      return dbNamesCache.get("databases");
    } catch (ExecutionException e) {
      throw new TException(e);
    }
  }

  /**
   * Gets collection of names and types of table objects
   * defined in requested database.
   *
   * @param dbName database name
   * @return  collection of names and types of table objects
   * @throws TException when loading failed
   */
  Map<String, Schema.TableType> getTableNamesAndTypes(String dbName) throws TException {
    try {
      return tableNamesCache.get(dbName);
    } catch (ExecutionException e) {
      throw new TException(e);
    }
  }

  /**
   * Get entry metadata for conrete table or view.
   *
   * @param dbName database name
   * @return set of table and view names
   * @throws TException when loading failed
   */
  HiveReadEntry getHiveReadEntry(String dbName, String tableName) throws TException {
    try {
      return tableEntryCache.get(TableName.of(dbName, tableName));
    } catch (final ExecutionException e) {
      throw new TException(e);
    }
  }

  /**
   * By default cache expiring policy is expire after write,
   * but user can change it to expire after access by setting
   * {@link HiveMetadataCache#HIVE_METASTORE_CACHE_EXPIRE} option
   * value to 'access' for Drill's Hive storage plugin.
   *
   * @param hiveConf hive conf for the storage plugin
   * @return flag defining expiration policy
   */
  private boolean isExpireAfterWrite(HiveConf hiveConf) {
    boolean expireAfterWrite = true; // default is expire after write.
    final String expiry = hiveConf.get(HIVE_METASTORE_CACHE_EXPIRE);
    if (HIVE_METASTORE_CACHE_EXPIRE_AFTER_ACCESS.equalsIgnoreCase(expiry)) {
      expireAfterWrite = false;
      logger.warn("Hive metastore cache expire policy is set to {}", "expireAfterAccess");
    }
    return expireAfterWrite;
  }

  /**
   * By default cache entry TTL is set to 60 seconds,
   * but user can change it using  {@link HiveMetadataCache#HIVE_METASTORE_CACHE_TTL}
   * property of Drill's Hive storage plugin.
   *
   * @param hiveConf hive conf for the storage plugin
   * @return cache entry TTL in seconds
   */
  private int getHmsCacheTTL(HiveConf hiveConf) {
    int hmsCacheTTL = 60; // default is 60 seconds
    final String ttl = hiveConf.get(HIVE_METASTORE_CACHE_TTL);
    if (!Strings.isNullOrEmpty(ttl)) {
      hmsCacheTTL = Integer.valueOf(ttl);
      logger.warn("Hive metastore cache ttl is set to {} seconds.", hmsCacheTTL);
    }
    return hmsCacheTTL;
  }

}
