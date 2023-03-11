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

import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.data.file.DeleteFileWithSequence;
import com.netease.arctic.io.reader.GenericCombinedIcebergDataReader;
import com.netease.arctic.io.writer.IcebergFanoutPosDeleteWriter;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.scan.CombinedIcebergScanTask;
import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.MetricsModes;
import org.apache.iceberg.Schema;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.iceberg.TableProperties.DEFAULT_FILE_FORMAT;
import static org.apache.iceberg.TableProperties.DEFAULT_FILE_FORMAT_DEFAULT;
import static org.apache.iceberg.TableProperties.DELETE_DEFAULT_FILE_FORMAT;

public class IcebergExecutor extends AbstractExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(IcebergExecutor.class);

  public IcebergExecutor(NodeTask nodeTask, ArcticTable table, long startTime, OptimizerConfig config) {
    super(nodeTask, table, startTime, config);
  }

  @Override
  public OptimizeTaskResult execute() throws Exception {
    LOG.info("Start processing iceberg table optimize task {} of {}: {}", task.getTaskId(), task.getTableIdentifier(),
        task);


    List<? extends ContentFile<?>> targetFiles;

    if (task.getOptimizeType().equals(OptimizeType.Minor) && task.icebergDataFiles().size() > 0) {
      // optimize iceberg delete files only in minor process
      targetFiles = table.io().doAs(this::optimizeDeleteFiles);
    } else {
      // optimize iceberg data files.
      targetFiles = table.io().doAs(this::optimizeDataFiles);
    }

    return buildOptimizeResult(targetFiles);
  }

  private List<? extends ContentFile<?>> optimizeDeleteFiles() throws Exception {
    Schema requiredSchema = new Schema(MetadataColumns.FILE_PATH, MetadataColumns.ROW_POSITION);

    GenericCombinedIcebergDataReader icebergDataReader = new GenericCombinedIcebergDataReader(
            table.io(), table.schema(), requiredSchema, table.properties().get(TableProperties.DEFAULT_NAME_MAPPING),
            false, IdentityPartitionConverters::convertConstant, false, structLikeCollections);

    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(table.schema(), table.spec());
    appenderFactory.setAll(table.properties());
    appenderFactory.set(
        org.apache.iceberg.TableProperties.METRICS_MODE_COLUMN_CONF_PREFIX + MetadataColumns.DELETE_FILE_PATH.name(),
        MetricsModes.Full.get().toString());
    appenderFactory.set(
        org.apache.iceberg.TableProperties.METRICS_MODE_COLUMN_CONF_PREFIX + MetadataColumns.DELETE_FILE_POS.name(),
        MetricsModes.Full.get().toString());
    String deleteFileFormatName =
        table.properties().getOrDefault(DELETE_DEFAULT_FILE_FORMAT, DEFAULT_FILE_FORMAT_DEFAULT);
    FileFormat deleteFileFormat = FileFormat.valueOf(deleteFileFormatName.toUpperCase());

    IcebergFanoutPosDeleteWriter<Record> icebergPosDeleteWriter = new IcebergFanoutPosDeleteWriter<>(
        appenderFactory, deleteFileFormat, task.getPartition(), table.io(), table.asUnkeyedTable().encryption(),
        String.format("%s-%d", task.getTaskId().traceId, task.getAttemptId()));

    AtomicLong insertCount = new AtomicLong();
    try (CloseableIterator<Record> iterator = icebergDataReader.readDeleteData(buildIcebergScanTask()).iterator()) {
      while (iterator.hasNext()) {
        checkIfTimeout(icebergPosDeleteWriter);
        Record record = iterator.next();
        String filePath = (String) record.getField(MetadataColumns.FILE_PATH.name());
        Long rowPosition = (Long) record.getField(MetadataColumns.ROW_POSITION.name());
        icebergPosDeleteWriter.delete(filePath, rowPosition);

        insertCount.incrementAndGet();
        if (insertCount.get() % SAMPLE_DATA_INTERVAL == 1) {
          LOG.info("task {} of {} insert records number {} and data sampling path:{}, pos:{}",
              task.getTaskId(), task.getTableIdentifier(), insertCount.get(), filePath, rowPosition);
        }
      }
    }

    LOG.info("task {} of {} insert records number {}", task.getTaskId(), task.getTableIdentifier(), insertCount.get());

    return icebergPosDeleteWriter.complete();
  }

  private CombinedIcebergScanTask buildIcebergScanTask() {
    return new CombinedIcebergScanTask(task.allIcebergDataFiles().toArray(new DataFileWithSequence[0]),
        task.allIcebergDeleteFiles().toArray(new DeleteFileWithSequence[0]),
        table.spec(), task.getPartition());
  }

  private List<? extends ContentFile<?>> optimizeDataFiles() throws Exception {
    List<DataFile> result = Lists.newArrayList();
    GenericCombinedIcebergDataReader icebergDataReader = new GenericCombinedIcebergDataReader(
        table.io(), table.schema(), table.schema(), table.properties().get(TableProperties.DEFAULT_NAME_MAPPING),
        false, IdentityPartitionConverters::convertConstant, false, structLikeCollections);

    String formatAsString = table.properties().getOrDefault(DEFAULT_FILE_FORMAT, DEFAULT_FILE_FORMAT_DEFAULT);
    long targetSizeByBytes = PropertyUtil.propertyAsLong(table.properties(),
        com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_TARGET_SIZE,
        com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT);

    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(table.asUnkeyedTable(), table.spec().specId(),
        task.getAttemptId()).build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile(task.getPartition());

    GenericAppenderFactory appenderFactory = new GenericAppenderFactory(table.schema(), table.spec());
    appenderFactory.setAll(table.properties());
    DataWriter<Record> writer = appenderFactory
        .newDataWriter(outputFile, FileFormat.valueOf(formatAsString.toUpperCase()), task.getPartition());

    long insertCount = 0;
    try (CloseableIterator<Record> records =  icebergDataReader.readData(buildIcebergScanTask()).iterator()) {
      while (records.hasNext()) {
        checkIfTimeout(writer);
        if (writer.length() > targetSizeByBytes) {
          writer.close();
          result.add(writer.toDataFile());
          outputFile = outputFileFactory.newOutputFile(task.getPartition());
          writer = appenderFactory
              .newDataWriter(outputFile, FileFormat.valueOf(formatAsString.toUpperCase()), task.getPartition());
        }
        Record record = records.next();
        writer.write(record);

        insertCount++;
        if (insertCount % SAMPLE_DATA_INTERVAL == 1) {
          LOG.info("task {} of {} insert records number {} and data sampling {}",
              task.getTaskId(), task.getTableIdentifier(), insertCount, record);
        }
      }
    }

    if (writer.length() > 0) {
      writer.close();
      result.add(writer.toDataFile());
    }

    LOG.info("task {} of {} insert records number {}", task.getTaskId(), task.getTableIdentifier(), insertCount);
    return result;
  }

  @Override
  public void close() {

  }
}
