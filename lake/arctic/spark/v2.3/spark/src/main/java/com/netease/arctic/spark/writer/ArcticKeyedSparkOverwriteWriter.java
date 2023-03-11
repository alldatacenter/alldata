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

import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.op.OverwriteBaseFiles;
import com.netease.arctic.op.RewritePartitions;
import com.netease.arctic.spark.io.TaskWriters;
import com.netease.arctic.spark.source.SupportsDynamicOverwrite;
import com.netease.arctic.spark.source.SupportsOverwrite;
import com.netease.arctic.table.KeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.spark.SparkFilters;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.iceberg.util.Tasks;
import org.apache.spark.TaskContext;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.sources.Filter;
import org.apache.spark.sql.sources.v2.DataSourceOptions;
import org.apache.spark.sql.sources.v2.writer.DataSourceWriter;
import org.apache.spark.sql.sources.v2.writer.DataWriter;
import org.apache.spark.sql.sources.v2.writer.DataWriterFactory;
import org.apache.spark.sql.sources.v2.writer.SupportsWriteInternalRow;
import org.apache.spark.sql.sources.v2.writer.WriterCommitMessage;
import org.apache.spark.sql.types.StructType;

import java.io.Serializable;
import java.util.Map;

import static com.netease.arctic.spark.writer.WriteTaskCommit.files;
import static org.apache.iceberg.TableProperties.COMMIT_MAX_RETRY_WAIT_MS;
import static org.apache.iceberg.TableProperties.COMMIT_MAX_RETRY_WAIT_MS_DEFAULT;
import static org.apache.iceberg.TableProperties.COMMIT_MIN_RETRY_WAIT_MS;
import static org.apache.iceberg.TableProperties.COMMIT_MIN_RETRY_WAIT_MS_DEFAULT;
import static org.apache.iceberg.TableProperties.COMMIT_NUM_RETRIES;
import static org.apache.iceberg.TableProperties.COMMIT_NUM_RETRIES_DEFAULT;
import static org.apache.iceberg.TableProperties.COMMIT_TOTAL_RETRY_TIME_MS;
import static org.apache.iceberg.TableProperties.COMMIT_TOTAL_RETRY_TIME_MS_DEFAULT;

public class ArcticKeyedSparkOverwriteWriter implements SupportsWriteInternalRow,
    SupportsOverwrite, SupportsDynamicOverwrite {

  private final KeyedTable table;
  private final StructType dsSchema;
  private final long txId;
  private final String subDir;
  protected Expression overwriteExpr = null;

  private WriteMode writeMode = null;

  private DataSourceOptions options;

  public ArcticKeyedSparkOverwriteWriter(KeyedTable table, StructType dsSchema, DataSourceOptions options) {
    this.options = options;
    if (options != null && options.asMap().containsKey(WriteMode.WRITE_MODE_KEY)) {
      this.writeMode = WriteMode.getWriteMode(options.get(WriteMode.WRITE_MODE_KEY).get());
    }
    this.table = table;
    this.dsSchema = dsSchema;
    this.txId = table.beginTransaction(null);
    this.subDir = HiveTableUtil.newHiveSubdirectory(this.txId);
  }

  @Override
  public DataSourceWriter overwriteDynamicPartitions() {
    Preconditions.checkState(overwriteExpr == null, "Cannot overwrite dynamically and by filter: %s", overwriteExpr);
    writeMode = WriteMode.OVERWRITE_DYNAMIC;
    return this;
  }

  @Override
  public DataSourceWriter overwrite(Filter[] filters) {
    Expression expression = Expressions.alwaysTrue();
    for (Filter filter : filters) {
      Expression converted = SparkFilters.convert(filter);
      Preconditions.checkArgument(converted != null, "Cannot convert filter to Iceberg: %s", filter);
      expression = Expressions.and(expression, converted);
    }
    this.overwriteExpr = expression;
    writeMode = WriteMode.OVERWRITE_BY_FILTER;
    return this;
  }

  @Override
  public DataWriterFactory<InternalRow> createInternalRowWriterFactory() {
    return new WriterFactory(table, dsSchema, txId, subDir);
  }

  private static class WriterFactory implements DataWriterFactory, Serializable {
    private final KeyedTable table;
    private final StructType dsSchema;
    private final long transactionId;
    private final String subDir;

    private WriterFactory(KeyedTable table, StructType dsSchema, long transactionId, String subDir) {
      this.table = table;
      this.dsSchema = dsSchema;
      this.transactionId = transactionId;
      this.subDir = subDir;
    }

    @Override
    public DataWriter createDataWriter(int partitionId, int attemptNumber) {
      TaskWriter<InternalRow> writer = TaskWriters.of(table)
          .withTransactionId(transactionId)
          .withPartitionId(partitionId)
          .withTaskId(TaskContext.get().taskAttemptId())
          .withDataSourceSchema(dsSchema)
          .withSubDir(subDir)
          .newBaseWriter(true);
      return new SimpleInternalRowDataWriter(writer);
    }
  }

  @Override
  public void commit(WriterCommitMessage[] messages) {
    if (writeMode == WriteMode.OVERWRITE_DYNAMIC) {
      rewritePartition(messages);
    } else {
      overwriteByFilter(messages, overwriteExpr);
    }
  }


  @Override
  public void abort(WriterCommitMessage[] messages) {
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
  }

  private void rewritePartition(WriterCommitMessage[] messages) {
    RewritePartitions rewritePartitions = table.newRewritePartitions();
    rewritePartitions.updateOptimizedSequenceDynamically(txId);

    for (DataFile file : files(messages)) {
      rewritePartitions.addDataFile(file);
    }
    rewritePartitions.commit();
  }

  private void overwriteByFilter(WriterCommitMessage[] messages, Expression overwriteExpr) {
    OverwriteBaseFiles overwriteBaseFiles = table.newOverwriteBaseFiles();
    overwriteBaseFiles.overwriteByRowFilter(overwriteExpr);
    overwriteBaseFiles.updateOptimizedSequenceDynamically(txId);

    for (DataFile file : files(messages)) {
      overwriteBaseFiles.addFile(file);
    }
    overwriteBaseFiles.commit();
  }
}

