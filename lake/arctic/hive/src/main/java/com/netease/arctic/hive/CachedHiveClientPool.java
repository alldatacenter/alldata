/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.table.TableMetaStore;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.UnknownDBException;
import org.apache.hadoop.hive.metastore.api.UnknownTableException;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache {@link ArcticHiveClientPool} with {@link TableMetaStore} key.
 */
public class CachedHiveClientPool implements HMSClientPool, Serializable {

  private static Cache<TableMetaStore, ArcticHiveClientPool> clientPoolCache;

  private final TableMetaStore tableMetaStore;
  private final int clientPoolSize;
  private final long evictionInterval;

  public CachedHiveClientPool(TableMetaStore tableMetaStore, Map<String, String> properties) {
    this.tableMetaStore = tableMetaStore;
    this.clientPoolSize = PropertyUtil.propertyAsInt(properties,
        CatalogMetaProperties.CLIENT_POOL_SIZE,
        CatalogMetaProperties.CLIENT_POOL_SIZE_DEFAULT);
    this.evictionInterval = PropertyUtil.propertyAsLong(properties,
        CatalogMetaProperties.CLIENT_POOL_CACHE_EVICTION_INTERVAL_MS,
        CatalogMetaProperties.CLIENT_POOL_CACHE_EVICTION_INTERVAL_MS_DEFAULT);
    init();
  }

  private ArcticHiveClientPool clientPool() {
    return clientPoolCache.get(tableMetaStore, k -> new ArcticHiveClientPool(tableMetaStore, clientPoolSize));
  }

  private synchronized void init() {
    if (clientPoolCache == null) {
      clientPoolCache = Caffeine.newBuilder().expireAfterAccess(evictionInterval, TimeUnit.MILLISECONDS)
          .removalListener((key, value, cause) -> ((ArcticHiveClientPool) value).close())
          .build();
    }
  }

  @Override
  public <R> R run(Action<R, HMSClient, TException> action) throws TException, InterruptedException {
    try {
      return tableMetaStore.doAs(() -> clientPool().run(action));
    } catch (RuntimeException e) {
      throw throwTException(e);
    }
  }

  @Override
  public <R> R run(Action<R, HMSClient, TException> action, boolean retry) throws TException, InterruptedException {
    try {
      return tableMetaStore.doAs(() -> clientPool().run(action, retry));
    } catch (RuntimeException e) {
      throw throwTException(e);
    }
  }

  public RuntimeException throwTException(RuntimeException e) throws TException {
    if (e.getCause() instanceof NoSuchObjectException) {
      throw (NoSuchObjectException) e.getCause();
    } else if (e.getCause() instanceof AlreadyExistsException) {
      throw (AlreadyExistsException) e.getCause();
    } else if (e.getCause() instanceof InvalidOperationException) {
      throw (InvalidOperationException) e.getCause();
    } else if (e.getCause() instanceof InvalidObjectException) {
      throw (InvalidObjectException) e.getCause();
    } else if (e.getCause() instanceof MetaException) {
      throw (MetaException) e.getCause();
    } else if (e.getCause() instanceof UnknownDBException) {
      throw (UnknownDBException) e.getCause();
    } else if (e.getCause() instanceof UnknownTableException) {
      throw (UnknownTableException) e.getCause();
    } else {
      throw e;
    }
  }
}
