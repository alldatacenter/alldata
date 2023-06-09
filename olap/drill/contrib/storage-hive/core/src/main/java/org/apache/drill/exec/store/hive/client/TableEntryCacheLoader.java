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
import java.util.stream.Collectors;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.store.hive.ColumnListsCache;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.drill.exec.store.hive.HiveTableWithColumnCache;
import org.apache.drill.exec.store.hive.HiveTableWrapper;
import org.apache.drill.exec.store.hive.HiveUtilities;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.UnknownTableException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CacheLoader that synchronized on client and tries to reconnect when
 * client fails. Used by {@link HiveMetadataCache}.
 */
final class TableEntryCacheLoader extends CacheLoader<TableName, HiveReadEntry> {

  private static final Logger logger = LoggerFactory.getLogger(TableEntryCacheLoader.class);

  private final DrillHiveMetaStoreClient client;

  TableEntryCacheLoader(DrillHiveMetaStoreClient client) {
    this.client = client;
  }


  @Override
  @SuppressWarnings("NullableProblems")
  public HiveReadEntry load(TableName key) throws Exception {
    Table table;
    List<Partition> partitions;
    synchronized (client) {
      table = getTable(key);
      partitions = getPartitions(key);
    }
    HiveTableWithColumnCache hiveTable = new HiveTableWithColumnCache(table, new ColumnListsCache(table));
    List<HiveTableWrapper.HivePartitionWrapper> partitionWrappers = getPartitionWrappers(partitions, hiveTable);
    return new HiveReadEntry(new HiveTableWrapper(hiveTable), partitionWrappers);
  }

  private List<HiveTableWrapper.HivePartitionWrapper> getPartitionWrappers(List<Partition> partitions, HiveTableWithColumnCache hiveTable) {
    if (partitions.isEmpty()) {
      return null;
    }
    return partitions.stream()
        .map(partition -> HiveUtilities.createPartitionWithSpecColumns(hiveTable, partition))
        .collect(Collectors.toList());
  }

  private List<Partition> getPartitions(TableName key) throws TException {
    List<Partition> partitions;
    try {
      partitions = client.listPartitions(key.getDbName(), key.getTableName(), (short) -1);
    } catch (NoSuchObjectException | MetaException e) {
      throw e;
    } catch (TException e) {
      logger.warn("Failure while attempting to get hive partitions. Retries once. ", e);
      AutoCloseables.closeSilently(client::close);
      client.reconnect();
      partitions = client.listPartitions(key.getDbName(), key.getTableName(), (short) -1);
    }
    return partitions;
  }

  private Table getTable(TableName key) throws TException {
    Table table;
    try {
      table = client.getTable(key.getDbName(), key.getTableName());
    } catch (MetaException | NoSuchObjectException e) {
      throw e;
    } catch (TException e) {
      logger.warn("Failure while attempting to get hive table. Retries once. ", e);
      AutoCloseables.closeSilently(client::close);
      client.reconnect();
      table = client.getTable(key.getDbName(), key.getTableName());
    }

    if (table == null) {
      throw new UnknownTableException(String.format("Unable to find table '%s'.", key.getTableName()));
    }
    return table;
  }

}
