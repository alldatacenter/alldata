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

package com.netease.arctic.optimizer.operator.executor;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.hive.io.reader.AdaptHiveGenericArcticDataReader;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.reader.BaseIcebergPosDeleteReader;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.BasicArcticFileScanTask;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.scan.NodeFileScanTask;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MinorExecutor extends AbstractExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(MinorExecutor.class);

  public MinorExecutor(NodeTask nodeTask, ArcticTable table, long startTime, OptimizerConfig config) {
    super(nodeTask, table, startTime, config);
  }

  @Override
  public OptimizeTaskResult execute() throws Exception {
    List<DeleteFile> targetFiles = new ArrayList<>();
    LOG.info("Start processing arctic table minor optimize task {} of {}: {}", task.getTaskId(),
        task.getTableIdentifier(), task);

    Map<DataTreeNode, List<PrimaryKeyedFile>> dataFileMap = groupDataFilesByNode(task.dataFiles());
    Map<DataTreeNode, List<DeleteFile>> deleteFileMap = groupDeleteFilesByNode(task.posDeleteFiles());
    KeyedTable keyedTable = table.asKeyedTable();

    AtomicLong insertCount = new AtomicLong();
    Schema requiredSchema = new Schema(MetadataColumns.FILE_PATH, MetadataColumns.ROW_POSITION);
    Types.StructType recordStruct = requiredSchema.asStruct();
    for (Map.Entry<DataTreeNode, List<PrimaryKeyedFile>> nodeFileEntry : dataFileMap.entrySet()) {
      DataTreeNode treeNode = nodeFileEntry.getKey();
      List<PrimaryKeyedFile> dataFiles = nodeFileEntry.getValue();
      dataFiles.addAll(task.deleteFiles());
      List<DeleteFile> posDeleteList = deleteFileMap.get(treeNode);

      SortedPosDeleteWriter<Record> posDeleteWriter = AdaptHiveGenericTaskWriterBuilder.builderFor(keyedTable)
          .withTransactionId(getMaxTransactionId(dataFiles))
          .withTaskId(task.getAttemptId())
          .buildBasePosDeleteWriter(treeNode.mask(), treeNode.index(), task.getPartition());

      table.io().doAs(() -> {

        try (CloseableIterator<Record> iterator =
                 openTask(dataFiles, posDeleteList, requiredSchema, task.getSourceNodes())) {
          while (iterator.hasNext()) {
            checkIfTimeout(posDeleteWriter);

            Record record = iterator.next();
            String filePath = (String) record.get(recordStruct.fields()
                .indexOf(recordStruct.field(MetadataColumns.FILE_PATH.name())));
            Long rowPosition = (Long) record.get(recordStruct.fields()
                .indexOf(recordStruct.field(MetadataColumns.ROW_POSITION.name())));
            posDeleteWriter.delete(filePath, rowPosition);
            insertCount.incrementAndGet();
            if (insertCount.get() % SAMPLE_DATA_INTERVAL == 1) {
              LOG.info("task {} of {} insert records number {} and data sampling path:{}, pos:{}",
                  task.getTaskId(), task.getTableIdentifier(), insertCount.get(), filePath, rowPosition);
            }
          }
        }

        return null;
      });

      // rewrite pos-delete content
      if (CollectionUtils.isNotEmpty(posDeleteList)) {
        BaseIcebergPosDeleteReader posDeleteReader = new BaseIcebergPosDeleteReader(table.io(), posDeleteList);
        table.io().doAs(() -> {
          CloseableIterable<Record> posDeleteIterable = posDeleteReader.readDeletes();
          try (CloseableIterator<Record> posDeleteIterator = posDeleteIterable.iterator()) {
            while (posDeleteIterator.hasNext()) {
              checkIfTimeout(posDeleteWriter);

              Record record = posDeleteIterator.next();
              String filePath = posDeleteReader.readPath(record);
              Long rowPosition = posDeleteReader.readPos(record);
              posDeleteWriter.delete(filePath, rowPosition);
            }
          }

          return null;
        });
      }

      targetFiles.addAll(posDeleteWriter.complete());
    }
    LOG.info("task {} of {} insert records number {}", task.getTaskId(), task.getTableIdentifier(), insertCount);

    return buildOptimizeResult(targetFiles);
  }

  @Override
  public void close() {

  }

  private CloseableIterator<Record> openTask(List<PrimaryKeyedFile> dataFiles, List<DeleteFile> posDeleteList,
                                             Schema requiredSchema, Set<DataTreeNode> sourceNodes) {
    if (CollectionUtils.isEmpty(dataFiles)) {
      return CloseableIterator.empty();
    }

    List<ArcticFileScanTask> fileScanTasks = dataFiles.stream()
        .map(file -> new BasicArcticFileScanTask(file, posDeleteList, table.spec()))
        .collect(Collectors.toList());

    PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.noPrimaryKey();
    if (table.isKeyedTable()) {
      KeyedTable keyedTable = table.asKeyedTable();
      primaryKeySpec = keyedTable.primaryKeySpec();
    }

    AdaptHiveGenericArcticDataReader arcticDataReader =
        new AdaptHiveGenericArcticDataReader(table.io(), table.schema(), requiredSchema,
            primaryKeySpec, table.properties().get(TableProperties.DEFAULT_NAME_MAPPING),
            false, IdentityPartitionConverters::convertConstant, sourceNodes, false, structLikeCollections);
    KeyedTableScanTask keyedTableScanTask = new NodeFileScanTask(fileScanTasks);
    return arcticDataReader.readDeletedData(keyedTableScanTask);
  }
}