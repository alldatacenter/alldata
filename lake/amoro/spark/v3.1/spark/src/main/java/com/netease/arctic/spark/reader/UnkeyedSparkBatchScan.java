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

package com.netease.arctic.spark.reader;

import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.spark.util.Stats;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SnapshotSummary;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.read.Batch;
import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.connector.read.PartitionReader;
import org.apache.spark.sql.connector.read.PartitionReaderFactory;
import org.apache.spark.sql.connector.read.Scan;
import org.apache.spark.sql.connector.read.Statistics;
import org.apache.spark.sql.connector.read.SupportsReportStatistics;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnkeyedSparkBatchScan implements Scan, Batch, SupportsReportStatistics {
  private static final Logger LOG = LoggerFactory.getLogger(UnkeyedSparkBatchScan.class);

  public static final String FILE_NAME_COL = "_file";
  public static final String ROW_POS_COL = "_pos";
  public static final List<String> rowIdColumns = Lists.newArrayList(FILE_NAME_COL, ROW_POS_COL);

  private final UnkeyedTable table;
  private final boolean caseSensitive;
  private final Schema expectedSchema;
  private final List<Expression> filterExpressions;

  private StructType readSchema = null;
  private List<CombinedScanTask> tasks = null;

  UnkeyedSparkBatchScan(
      UnkeyedTable table, boolean caseSensitive,
      Schema expectedSchema, List<Expression> filters, CaseInsensitiveStringMap options) {
    this.table = table;
    this.caseSensitive = caseSensitive;
    this.expectedSchema = expectedSchema;
    this.filterExpressions = filters;
  }

  @Override
  public InputPartition[] planInputPartitions() {
    List<CombinedScanTask> scanTasks = tasks();
    ArcticInputPartition[] readTasks = new ArcticInputPartition[scanTasks.size()];
    for (int i = 0; i < scanTasks.size(); i++) {
      readTasks[i] = new ArcticInputPartition(scanTasks.get(i), table, expectedSchema,
          caseSensitive);
    }
    return readTasks;
  }

  @Override
  public PartitionReaderFactory createReaderFactory() {
    return new ReaderFactory();
  }

  @Override
  public Statistics estimateStatistics() {
    if (table.currentSnapshot() == null) {
      return new Stats(0L, 0L);
    }
    if (!table.spec().isUnpartitioned() &&
        (filterExpressions == null || filterExpressions.isEmpty())) {
      LOG.debug("using table metadata to estimate table statistics");
      long totalRecords =
          PropertyUtil.propertyAsLong(
              table.currentSnapshot().summary(),
              SnapshotSummary.TOTAL_RECORDS_PROP,
              Long.MAX_VALUE);
      return new Stats(SparkSchemaUtil.estimateSize(readSchema(), totalRecords), totalRecords);
    }

    long numRows = 0L;

    for (CombinedScanTask task : tasks()) {
      for (FileScanTask file : task.files()) {
        double fractionOfFileScanned = ((double) file.length()) / file.file().fileSizeInBytes();
        numRows += (fractionOfFileScanned * file.file().recordCount());
      }
    }
    long sizeInBytes = SparkSchemaUtil.estimateSize(readSchema(), numRows);
    return new Stats(sizeInBytes, numRows);
  }

  @Override
  public StructType readSchema() {
    if (readSchema == null) {
      this.readSchema = SparkSchemaUtil.convert(expectedSchema);
    }
    return readSchema;
  }

  @Override
  public Batch toBatch() {
    return this;
  }

  private List<CombinedScanTask> tasks() {
    if (tasks == null) {
      TableScan scan = table.newScan();

      if (filterExpressions != null) {
        for (Expression filter : filterExpressions) {
          scan = scan.filter(filter);
        }
      }
      long startTime = System.currentTimeMillis();
      LOG.info("mor statistics plan task start");
      try (CloseableIterable<CombinedScanTask> tasksIterable = scan.planTasks()) {
        this.tasks = Lists.newArrayList(tasksIterable);
        LOG.info("mor statistics plan task end, cost time {}, tasks num {}",
            System.currentTimeMillis() - startTime, tasks.size());
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to close table scan: %s", e);
      }
    }
    return tasks;
  }

  private static class ReaderFactory implements PartitionReaderFactory {
    @Override
    public PartitionReader<InternalRow> createReader(InputPartition partition) {
      if (partition instanceof ArcticInputPartition) {
        return new RowReader((ArcticInputPartition) partition);
      } else {
        throw new UnsupportedOperationException("Incorrect input partition type: " + partition);
      }
    }
  }

  private static class RowReader implements PartitionReader<InternalRow> {

    ArcticSparkUnkeyedDataReader reader;
    Iterator<FileScanTask> scanTasks;
    FileScanTask currentScanTask;
    CloseableIterator<InternalRow> currentIterator = CloseableIterator.empty();
    InternalRow current;

    RowReader(ArcticInputPartition task) {
      reader = new ArcticSparkUnkeyedDataReader(
          task.io, task.tableSchema, task.expectedSchema,
          task.nameMapping, task.caseSensitive
      );
      scanTasks = task.combinedScanTask.files().iterator();
    }

    @Override
    public boolean next() throws IOException {
      while (true) {
        if (currentIterator.hasNext()) {
          this.current = currentIterator.next();
          return true;
        } else if (scanTasks.hasNext()) {
          this.currentIterator.close();
          this.currentScanTask = scanTasks.next();
          this.currentIterator = reader.readData(this.currentScanTask).iterator();
        } else {
          this.currentIterator.close();
          return false;
        }
      }
    }

    @Override
    public InternalRow get() {
      return this.current;
    }

    @Override
    public void close() throws IOException {
      this.currentIterator.close();
      while (scanTasks.hasNext()) {
        scanTasks.next();
      }
    }
  }

  private static class ArcticInputPartition implements InputPartition, Serializable {
    final CombinedScanTask combinedScanTask;
    final ArcticFileIO io;
    final boolean caseSensitive;
    final Schema expectedSchema;
    final Schema tableSchema;
    final String nameMapping;

    ArcticInputPartition(
        CombinedScanTask combinedScanTask,
        UnkeyedTable table,
        Schema expectedSchema,
        boolean caseSensitive) {
      this.combinedScanTask = combinedScanTask;
      this.expectedSchema = expectedSchema;
      this.tableSchema = table.schema();
      this.caseSensitive = caseSensitive;
      this.io = table.io();
      this.nameMapping = table.properties().get(TableProperties.DEFAULT_NAME_MAPPING);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnkeyedSparkBatchScan that = (UnkeyedSparkBatchScan) o;
    return table.id().equals(that.table.id()) &&
        readSchema().equals(that.readSchema()) &&
        filterExpressions.toString().equals(that.filterExpressions.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        table.id(), readSchema(), filterExpressions.toString());
  }

  @Override
  public String description() {
    if (filterExpressions != null) {
      String filters = filterExpressions.stream().map(Spark3Util::describe).collect(Collectors.joining(", "));
      return String.format("%s [filters=%s]", table, filters);
    }
    return "";
  }

  @Override
  public String toString() {
    return String.format(
        "IcebergScan(table=%s, type=%s, filters=%s, caseSensitive=%s)",
        table, expectedSchema.asStruct(), filterExpressions, caseSensitive);
  }
}
