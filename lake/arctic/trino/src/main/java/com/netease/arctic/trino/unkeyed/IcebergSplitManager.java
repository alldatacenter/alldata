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

package com.netease.arctic.trino.unkeyed;

import com.google.common.collect.ImmutableList;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.trino.ArcticTransactionManager;
import com.netease.arctic.trino.TableNameResolve;
import io.airlift.units.Duration;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorSplitSource;
import io.trino.plugin.iceberg.IcebergTableHandle;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.FixedSplitSource;
import io.trino.spi.type.TypeManager;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableScan;

import javax.inject.Inject;

import static io.trino.plugin.iceberg.IcebergSessionProperties.getDynamicFilteringWaitTimeout;
import static java.util.Objects.requireNonNull;

/**
 * Iceberg original IcebergSplitManager has some problems for arctic, such as iceberg version, table type.
 */
public class IcebergSplitManager implements ConnectorSplitManager {

  private final ArcticTransactionManager transactionManager;
  private final TypeManager typeManager;

  @Inject
  public IcebergSplitManager(ArcticTransactionManager transactionManager, TypeManager typeManager) {
    this.transactionManager = requireNonNull(transactionManager, "transactionManager is null");
    this.typeManager = requireNonNull(typeManager, "typeManager is null");
  }

  @Override
  public ConnectorSplitSource getSplits(
      ConnectorTransactionHandle transaction,
      ConnectorSession session,
      ConnectorTableHandle handle,
      SplitSchedulingStrategy splitSchedulingStrategy,
      DynamicFilter dynamicFilter,
      Constraint constraint) {
    IcebergTableHandle table = (IcebergTableHandle) handle;

    if (table.getSnapshotId().isEmpty()) {
      if (table.isRecordScannedFiles()) {
        return new FixedSplitSource(ImmutableList.of(), ImmutableList.of());
      }
      return new FixedSplitSource(ImmutableList.of());
    }

    Table icebergTable =
        transactionManager.get(transaction).getArcticTable(table.getSchemaTableName()).asUnkeyedTable();
    Duration dynamicFilteringWaitTimeout = getDynamicFilteringWaitTimeout(session);

    TableScan tableScan;
    if (icebergTable instanceof ChangeTable) {
      tableScan = ((ChangeTable)icebergTable).newChangeScan();
    } else {
      tableScan = icebergTable.newScan()
          .useSnapshot(table.getSnapshotId().get());
    }

    TableNameResolve resolve = new TableNameResolve(table.getTableName());
    IcebergSplitSource splitSource = new IcebergSplitSource(
        table,
        tableScan,
        table.getMaxScannedFileSize(),
        dynamicFilter,
        dynamicFilteringWaitTimeout,
        constraint,
        typeManager,
        table.isRecordScannedFiles(),
        resolve.withSuffix() ? !resolve.isBase() : false);

    return new ClassLoaderSafeConnectorSplitSource(splitSource, Thread.currentThread().getContextClassLoader());
  }
}
