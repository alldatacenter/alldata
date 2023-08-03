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

package com.netease.arctic.spark.writer;

import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.spark.io.TaskWriters;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.blocker.Blocker;
import com.netease.arctic.table.blocker.TableBlockerManager;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.iceberg.util.Tasks;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.BatchWrite;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.connector.write.DataWriterFactory;
import org.apache.spark.sql.connector.write.LogicalWriteInfo;
import org.apache.spark.sql.connector.write.PhysicalWriteInfo;
import org.apache.spark.sql.connector.write.Write;
import org.apache.spark.sql.connector.write.WriterCommitMessage;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;
import static com.netease.arctic.spark.writer.WriteTaskCommit.files;
import static org.apache.iceberg.TableProperties.COMMIT_MAX_RETRY_WAIT_MS;
import static org.apache.iceberg.TableProperties.COMMIT_MAX_RETRY_WAIT_MS_DEFAULT;
import static org.apache.iceberg.TableProperties.COMMIT_MIN_RETRY_WAIT_MS;
import static org.apache.iceberg.TableProperties.COMMIT_MIN_RETRY_WAIT_MS_DEFAULT;
import static org.apache.iceberg.TableProperties.COMMIT_NUM_RETRIES;
import static org.apache.iceberg.TableProperties.COMMIT_NUM_RETRIES_DEFAULT;
import static org.apache.iceberg.TableProperties.COMMIT_TOTAL_RETRY_TIME_MS;
import static org.apache.iceberg.TableProperties.COMMIT_TOTAL_RETRY_TIME_MS_DEFAULT;

public class UnkeyedSparkBatchWrite implements ArcticSparkWriteBuilder.ArcticWrite, Write {

  private final UnkeyedTable table;
  private final StructType dsSchema;
  private final String hiveSubdirectory = HiveTableUtil.newHiveSubdirectory();

  private final boolean orderedWriter;

  private final ArcticCatalog catalog;

  public UnkeyedSparkBatchWrite(UnkeyedTable table, LogicalWriteInfo info, ArcticCatalog catalog) {
    this.table = table;
    this.dsSchema = info.schema();
    this.orderedWriter = Boolean.parseBoolean(info.options().getOrDefault(
        "writer.distributed-and-ordered", "false"
    ));
    this.catalog = catalog;
  }

  @Override
  public BatchWrite asBatchAppend() {
    return new AppendWrite();
  }

  @Override
  public BatchWrite asDynamicOverwrite() {
    return new DynamicOverwrite();
  }

  @Override
  public BatchWrite asOverwriteByFilter(Expression overwriteExpr) {
    return new OverwriteByFilter(overwriteExpr);
  }

  @Override
  public BatchWrite asUpsertWrite() {
    return new UpsertWrite();
  }

  private abstract class BaseBatchWrite implements BatchWrite {

    protected TableBlockerManager tableBlockerManager;
    protected Blocker block;

    @Override
    public void abort(WriterCommitMessage[] messages) {
      try {
        Map<String, String> props = table.properties();
        Tasks.foreach(files(messages))
            .retry(PropertyUtil.propertyAsInt(props, COMMIT_NUM_RETRIES, COMMIT_NUM_RETRIES_DEFAULT))
            .exponentialBackoff(
                PropertyUtil.propertyAsInt(props, COMMIT_MIN_RETRY_WAIT_MS, COMMIT_MIN_RETRY_WAIT_MS_DEFAULT),
                PropertyUtil.propertyAsInt(props, COMMIT_MAX_RETRY_WAIT_MS, COMMIT_MAX_RETRY_WAIT_MS_DEFAULT),
                PropertyUtil.propertyAsInt(props, COMMIT_TOTAL_RETRY_TIME_MS, COMMIT_TOTAL_RETRY_TIME_MS_DEFAULT),
                2.0 /* exponential */)
            .throwFailureWhenFinished()
            .run(file -> {
              table.io().deleteFile(file.path().toString());
            });
      } finally {
        tableBlockerManager.release(block);
      }
    }

    public void checkBlocker(TableBlockerManager tableBlockerManager) {
      List<String> blockerIds = tableBlockerManager.getBlockers()
          .stream().map(Blocker::blockerId).collect(Collectors.toList());
      if (!blockerIds.contains(block.blockerId())) {
        throw new IllegalStateException("block is not in blockerManager");
      }
    }

    public void getBlocker() {
      this.tableBlockerManager = catalog.getTableBlockerManager(table.id());
      ArrayList<BlockableOperation> operations = Lists.newArrayList();
      operations.add(BlockableOperation.BATCH_WRITE);
      operations.add(BlockableOperation.OPTIMIZE);
      try {
        this.block = tableBlockerManager.block(operations);
      } catch (OperationConflictException e) {
        throw new IllegalStateException("failed to block table " + table.id() + " with " + operations, e);
      }
    }
  }

  private class AppendWrite extends BaseBatchWrite {

    @Override
    public DataWriterFactory createBatchWriterFactory(PhysicalWriteInfo info) {
      getBlocker();
      return new WriterFactory(table, dsSchema, false, null, orderedWriter);
    }

    @Override
    public void commit(WriterCommitMessage[] messages) {
      checkBlocker(tableBlockerManager);
      AppendFiles appendFiles = table.newAppend();
      for (DataFile file : files(messages)) {
        appendFiles.appendFile(file);
      }
      appendFiles.commit();
      tableBlockerManager.release(block);
    }
  }

  private class DynamicOverwrite extends BaseBatchWrite {

    @Override
    public DataWriterFactory createBatchWriterFactory(PhysicalWriteInfo info) {
      getBlocker();
      return new WriterFactory(table, dsSchema, true, hiveSubdirectory, orderedWriter);
    }

    @Override
    public void commit(WriterCommitMessage[] messages) {
      checkBlocker(tableBlockerManager);
      ReplacePartitions replacePartitions = table.newReplacePartitions();
      for (DataFile file : files(messages)) {
        replacePartitions.addFile(file);
      }
      replacePartitions.commit();
      tableBlockerManager.release(block);
    }
  }

  private class OverwriteByFilter extends BaseBatchWrite {
    private final Expression overwriteExpr;

    private OverwriteByFilter(Expression overwriteExpr) {
      this.overwriteExpr = overwriteExpr;
    }

    @Override
    public DataWriterFactory createBatchWriterFactory(PhysicalWriteInfo info) {
      getBlocker();
      return new WriterFactory(table, dsSchema, true, hiveSubdirectory, orderedWriter);
    }

    @Override
    public void commit(WriterCommitMessage[] messages) {
      checkBlocker(tableBlockerManager);
      OverwriteFiles overwriteFiles = table.newOverwrite();
      overwriteFiles.overwriteByRowFilter(overwriteExpr);
      overwriteFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
      for (DataFile file : files(messages)) {
        overwriteFiles.addFile(file);
      }
      overwriteFiles.commit();
      tableBlockerManager.release(block);
    }
  }

  private class UpsertWrite extends BaseBatchWrite {
    @Override
    public DataWriterFactory createBatchWriterFactory(PhysicalWriteInfo info) {
      getBlocker();
      return new DeltaUpsertWriteFactory(table, dsSchema, orderedWriter);
    }

    @Override
    public void commit(WriterCommitMessage[] messages) {
      checkBlocker(tableBlockerManager);
      RowDelta rowDelta = table.newRowDelta();
      if (WriteTaskCommit.deleteFiles(messages).iterator().hasNext()) {
        for (DeleteFile file : WriteTaskCommit.deleteFiles(messages)) {
          rowDelta.addDeletes(file);
        }
      }
      if (WriteTaskCommit.files(messages).iterator().hasNext()) {
        for (DataFile file : WriteTaskCommit.files(messages)) {
          rowDelta.addRows(file);
        }
      }
      rowDelta.commit();
      tableBlockerManager.release(block);
    }
  }

  private static class WriterFactory implements DataWriterFactory, Serializable {
    protected final UnkeyedTable table;
    protected final StructType dsSchema;

    protected final String hiveSubdirectory;

    protected final boolean isOverwrite;
    protected final boolean orderedWriter;

    WriterFactory(
        UnkeyedTable table,
        StructType dsSchema,
        boolean isOverwrite,
        String hiveSubdirectory,
        boolean orderedWrite) {
      this.table = table;
      this.dsSchema = dsSchema;
      this.isOverwrite = isOverwrite;
      this.hiveSubdirectory = hiveSubdirectory;
      this.orderedWriter = orderedWrite;
    }

    @Override
    public DataWriter<InternalRow> createWriter(int partitionId, long taskId) {
      TaskWriters builder = TaskWriters.of(table)
          .withPartitionId(partitionId)
          .withTaskId(taskId)
          .withOrderedWriter(orderedWriter)
          .withDataSourceSchema(dsSchema)
          .withHiveSubdirectory(hiveSubdirectory);

      TaskWriter<InternalRow> writer = builder.newBaseWriter(this.isOverwrite);
      return new SimpleInternalRowDataWriter(writer);
    }

    public TaskWriter<InternalRow> newWriter(int partitionId, long taskId, StructType schema) {
      return TaskWriters.of(table)
          .withPartitionId(partitionId)
          .withTaskId(taskId)
          .withDataSourceSchema(schema)
          .newUnkeyedUpsertWriter();
    }
  }

  private static class DeltaUpsertWriteFactory extends WriterFactory {

    DeltaUpsertWriteFactory(UnkeyedTable table, StructType dsSchema, boolean ordredWriter) {
      super(table, dsSchema, false, null, ordredWriter);
    }

    @Override
    public DataWriter<InternalRow> createWriter(int partitionId, long taskId) {
      StructType schema = new StructType(Arrays.stream(dsSchema.fields()).filter(f -> !f.name().equals("_file") &&
          !f.name().equals("_pos") && !f.name().equals("_arctic_upsert_op")).toArray(StructField[]::new));
      return new SimpleRowLevelDataWriter(newWriter(partitionId, taskId, schema),
          newWriter(partitionId, taskId, schema), dsSchema, table.isKeyedTable());
    }
  }
}
