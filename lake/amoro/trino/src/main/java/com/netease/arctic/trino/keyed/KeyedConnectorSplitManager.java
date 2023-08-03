
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

package com.netease.arctic.trino.keyed;

import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScan;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.trino.ArcticSessionProperties;
import com.netease.arctic.trino.ArcticTransactionManager;
import com.netease.arctic.trino.util.MetricUtil;
import com.netease.arctic.trino.util.ObjectSerializerUtil;
import io.trino.plugin.iceberg.IcebergTableHandle;
import io.trino.plugin.iceberg.PartitionData;
import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.FixedSplitSource;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.TableNotFoundException;
import org.apache.iceberg.PartitionSpecParser;
import org.apache.iceberg.io.CloseableIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.trino.plugin.iceberg.ExpressionConverter.toIcebergExpression;

/**
 * ConnectorSplitManager for Keyed Table
 */
public class KeyedConnectorSplitManager implements ConnectorSplitManager {

  public static final int ARCTIC_DOMAIN_COMPACTION_THRESHOLD = 1000;

  private static final Logger LOG = LoggerFactory.getLogger(KeyedConnectorSplitManager.class);

  private ArcticTransactionManager arcticTransactionManager;

  @Inject
  public KeyedConnectorSplitManager(ArcticTransactionManager arcticTransactionManager) {
    this.arcticTransactionManager = arcticTransactionManager;
  }

  @Override
  public ConnectorSplitSource getSplits(
      ConnectorTransactionHandle transaction,
      ConnectorSession session,
      ConnectorTableHandle handle,
      DynamicFilter dynamicFilter,
      Constraint constraint) {
    KeyedTableHandle keyedTableHandle = (KeyedTableHandle) handle;
    IcebergTableHandle icebergTableHandle = keyedTableHandle.getIcebergTableHandle();
    KeyedTable arcticTable = (arcticTransactionManager.get(transaction))
        .getArcticTable(new SchemaTableName(
            icebergTableHandle.getSchemaName(),
            icebergTableHandle.getTableName())).asKeyedTable();
    if (arcticTable == null) {
      throw new TableNotFoundException(new SchemaTableName(
          icebergTableHandle.getSchemaName(),
          icebergTableHandle.getTableName()));
    }

    KeyedTableScan tableScan = arcticTable.newScan()
        .filter(toIcebergExpression(
            icebergTableHandle.getEnforcedPredicate().intersect(icebergTableHandle.getUnenforcedPredicate())));

    if (ArcticSessionProperties.enableSplitTaskByDeleteRatio(session)) {
      tableScan.enableSplitTaskByDeleteRatio(ArcticSessionProperties.splitTaskByDeleteRatio(session));
    }

    ClassLoader pluginClassloader = arcticTable.getClass().getClassLoader();

    try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(pluginClassloader)) {
      //优化
      CloseableIterable<CombinedScanTask> combinedScanTasks =
          MetricUtil.duration(() -> tableScan.planTasks(), "plan tasks");

      List<KeyedTableScanTask> fileScanTaskList = new ArrayList<>();
      for (CombinedScanTask combinedScanTask : combinedScanTasks) {
        for (KeyedTableScanTask fileScanTask : combinedScanTask.tasks()) {
          fileScanTaskList.add(fileScanTask);
        }
      }

      List<KeyedConnectorSplit> keyedConnectorSplits = fileScanTaskList.stream().map(
          s -> {
            ArcticFileScanTask arcticFileScanTask = s.dataTasks().get(0);
            KeyedConnectorSplit keyedConnectorSplit = new KeyedConnectorSplit(
                ObjectSerializerUtil.write(s),
                PartitionSpecParser.toJson(arcticFileScanTask.spec()),
                PartitionData.toJson(arcticFileScanTask.file().partition())
            );
            return keyedConnectorSplit;
          }
      ).collect(Collectors.toList());

      return new FixedSplitSource(keyedConnectorSplits);
    }
  }
}
