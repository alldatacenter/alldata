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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.calcite.schema.Schema.TableType;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.hive.metastore.TableType.VIRTUAL_VIEW;

/**
 * CacheLoader that synchronized on client and tries to reconnect when
 * client fails. Used by {@link HiveMetadataCache}.
 */
final class TableNameCacheLoader extends CacheLoader<String, Map<String, TableType>> {

  private static final Logger logger = LoggerFactory.getLogger(TableNameCacheLoader.class);

  private final DrillHiveMetaStoreClient client;

  TableNameCacheLoader(DrillHiveMetaStoreClient client) {
    this.client = client;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Map<String, TableType> load(String dbName) throws Exception {
    List<String> tableAndViewNames;
    final Set<String> viewNames = new HashSet<>();
    synchronized (client) {
      try {
        tableAndViewNames = client.getAllTables(dbName);
        viewNames.addAll(client.getTables(dbName, "*", VIRTUAL_VIEW));
      } catch (MetaException e) {
      /*
         HiveMetaStoreClient is encapsulating both the MetaException/TExceptions inside MetaException.
         Since we don't have good way to differentiate, we will close older connection and retry once.
         This is only applicable for getAllTables and getAllDatabases method since other methods are
         properly throwing correct exceptions.
      */
        logger.warn("Failure while attempting to get hive tables. Retries once.", e);
        AutoCloseables.closeSilently(client::close);
        client.reconnect();
        tableAndViewNames = client.getAllTables(dbName);
        viewNames.addAll(client.getTables(dbName, "*", VIRTUAL_VIEW));
      }
    }
    Map<String, TableType> result = tableAndViewNames.stream()
        .collect(Collectors.toMap(Function.identity(), getValueMapper(viewNames)));
    return Collections.unmodifiableMap(result);
  }

  /**
   * Creates function used to map table or view name to appropriate
   * {@link TableType} value based on set of view names.
   *
   * @param viewNames set of view names
   * @return mapping function
   */
  private Function<String, TableType> getValueMapper(Set<String> viewNames) {
    if (viewNames.isEmpty()) {
      return tableName -> TableType.TABLE;
    } else {
      return tableName -> viewNames.contains(tableName) ? TableType.VIEW : TableType.TABLE;
    }
  }

}
