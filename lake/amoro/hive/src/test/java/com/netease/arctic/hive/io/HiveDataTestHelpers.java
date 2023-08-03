/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive.io;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.hive.io.reader.AdaptHiveGenericArcticDataReader;
import com.netease.arctic.hive.io.reader.GenericAdaptHiveIcebergDataReader;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.BasicArcticFileScanTask;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.scan.NodeFileScanTask;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.ChangeLocationKind;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.LocationKind;
import com.netease.arctic.table.MetadataColumns;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.Files;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.AdaptHiveGenericParquetReaders;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.parquet.AdaptHiveParquet;
import org.apache.iceberg.relocated.com.google.common.collect.Iterators;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HiveDataTestHelpers {

  public static List<DataFile> writeChangeStore(
      KeyedTable keyedTable, Long txId, ChangeAction action,
      List<Record> records, boolean orderedWrite) {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder.builderFor(keyedTable)
        .withChangeAction(action)
        .withTransactionId(txId);
    if (orderedWrite) {
      builder.withOrdered();
    }
    try (TaskWriter<Record> writer = builder.buildWriter(ChangeLocationKind.INSTANT)) {
      return DataTestHelpers.writeRecords(writer, records);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<DataFile> writeBaseStore(
      ArcticTable table, long txId, List<Record> records,
      boolean orderedWrite, boolean writeHiveLocation) {
    return writeBaseStore(table, txId, records, orderedWrite, writeHiveLocation, null);
  }

  public static List<DataFile> writeBaseStore(
      ArcticTable table, long txId, List<Record> records,
      boolean orderedWrite, boolean writeHiveLocation, String hiveLocation) {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder.builderFor(table);
    if (table.isKeyedTable()) {
      builder.withTransactionId(txId);
    }
    if (orderedWrite) {
      builder.withOrdered();
    }
    if (hiveLocation != null) {
      builder.withCustomHiveSubdirectory(hiveLocation);
    }
    LocationKind writeLocationKind = writeHiveLocation ? HiveLocationKind.INSTANT : BaseLocationKind.INSTANT;
    try (TaskWriter<Record> writer = builder.buildWriter(writeLocationKind)) {
      return DataTestHelpers.writeRecords(writer, records);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<Record> readKeyedTable(
      KeyedTable keyedTable, Expression expression,
      Schema projectSchema, boolean useDiskMap, boolean readDeletedData) {
    AdaptHiveGenericArcticDataReader reader;
    if (projectSchema == null) {
      projectSchema = keyedTable.schema();
    }
    if (useDiskMap) {
      reader = new AdaptHiveGenericArcticDataReader(
          keyedTable.io(),
          keyedTable.schema(),
          projectSchema,
          keyedTable.primaryKeySpec(),
          null,
          true,
          IdentityPartitionConverters::convertConstant,
          null, false, new StructLikeCollections(true, 0L)
      );
    } else {
      reader = new AdaptHiveGenericArcticDataReader(
          keyedTable.io(),
          keyedTable.schema(),
          projectSchema,
          keyedTable.primaryKeySpec(),
          null,
          true,
          IdentityPartitionConverters::convertConstant
      );
    }

    return DataTestHelpers.readKeyedTable(keyedTable, reader, expression, projectSchema, readDeletedData);
  }

  public static List<Record> readChangeStore(
      KeyedTable keyedTable, Expression expression, Schema projectSchema,
      boolean useDiskMap) {
    if (projectSchema == null) {
      projectSchema = keyedTable.schema();
    }
    Schema expectTableSchema = MetadataColumns.appendChangeStoreMetadataColumns(keyedTable.schema());
    Schema expectProjectSchema = MetadataColumns.appendChangeStoreMetadataColumns(projectSchema);

    GenericAdaptHiveIcebergDataReader reader;
    if (useDiskMap) {
      reader = new GenericAdaptHiveIcebergDataReader(
          keyedTable.asKeyedTable().io(),
          expectTableSchema,
          expectProjectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false,
          new StructLikeCollections(true, 0L));
    } else {
      reader = new GenericAdaptHiveIcebergDataReader(
          keyedTable.asKeyedTable().io(),
          expectTableSchema,
          expectProjectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false
      );
    }

    return DataTestHelpers.readChangeStore(keyedTable, reader, expression);
  }

  public static List<Record> readBaseStore(
      ArcticTable table, Expression expression, Schema projectSchema,
      boolean useDiskMap) {
    if (projectSchema == null) {
      projectSchema = table.schema();
    }

    GenericAdaptHiveIcebergDataReader reader;
    if (useDiskMap) {
      reader = new GenericAdaptHiveIcebergDataReader(
          table.io(),
          table.schema(),
          projectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false,
          new StructLikeCollections(true, 0L));
    } else {
      reader = new GenericAdaptHiveIcebergDataReader(
          table.io(),
          table.schema(),
          projectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false
      );
    }

    return DataTestHelpers.readBaseStore(table, reader, expression);
  }

  public static void testWrite(ArcticTable table, LocationKind locationKind, List<Record> records, String pathFeature)
      throws IOException {
    testWrite(table, locationKind, records, pathFeature, null, null);
  }

  public static void testWrite(
      ArcticTable table, LocationKind locationKind, List<Record> records, String pathFeature,
      Expression expression, List<Record> readRecords) throws IOException {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder
        .builderFor(table)
        .withTransactionId(table.isKeyedTable() ? 1L : null);

    TaskWriter<Record> changeWrite = builder.buildWriter(locationKind);
    for (Record record : records) {
      changeWrite.write(record);
    }
    WriteResult complete = changeWrite.complete();
    Arrays.stream(complete.dataFiles()).forEach(s -> Assert.assertTrue(s.path().toString().contains(pathFeature)));
    CloseableIterator<Record> iterator = readParquet(
        table.schema(),
        complete.dataFiles(),
        expression,
        table.io(),
        table.isKeyedTable() ? table.asKeyedTable().primaryKeySpec() : null,
        table.spec()
    );
    Set<Record> result = new HashSet<>();
    Iterators.addAll(result, iterator);
    if (readRecords == null) {
      Assert.assertEquals(result, new HashSet<>(records));
    } else {
      Assert.assertEquals(result, new HashSet<>(readRecords));
    }
  }

  public static void testWriteChange(
      KeyedTable table, long txId, List<Record> records, ChangeAction action) throws IOException {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder
        .builderFor(table)
        .withChangeAction(action)
        .withTransactionId(txId);

    TaskWriter<Record> changeWrite = builder.buildWriter(ChangeLocationKind.INSTANT);
    for (Record record : records) {
      changeWrite.write(record);
    }
    DataFile[] dataFiles = changeWrite.complete().dataFiles();
    AppendFiles appendFiles = table.changeTable().newAppend();
    Arrays.asList(dataFiles).forEach(appendFiles::appendFile);
    appendFiles.commit();
  }

  public static List<Record> readHiveKeyedTable(KeyedTable keyedTable, Expression expression) {
    AdaptHiveGenericArcticDataReader reader = new AdaptHiveGenericArcticDataReader(
        keyedTable.io(),
        keyedTable.schema(),
        keyedTable.schema(),
        keyedTable.primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant
    );
    List<Record> result = Lists.newArrayList();
    try (CloseableIterable<CombinedScanTask> combinedScanTasks = keyedTable.newScan().filter(expression).planTasks()) {
      combinedScanTasks.forEach(combinedTask -> combinedTask.tasks().forEach(scTask -> {
        try (CloseableIterator<Record> records = reader.readData(scTask)) {
          while (records.hasNext()) {
            result.add(records.next());
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result;
  }

  private static CloseableIterable<Record> readParquet(Schema schema, String path, Expression expression) {
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(
        Files.localInput(path))
        .project(schema)
        .filter(expression == null ? Expressions.alwaysTrue() : expression)
        .createReaderFunc(fileSchema -> AdaptHiveGenericParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);

    CloseableIterable<Record> iterable = builder.build();
    return iterable;
  }

  private static CloseableIterator<Record> readParquet(
      Schema schema, DataFile[] dataFiles, Expression expression,
      ArcticFileIO fileIO, PrimaryKeySpec primaryKeySpec, PartitionSpec partitionSpec) {
    List<ArcticFileScanTask> arcticFileScanTasks = Arrays.stream(dataFiles).map(s -> new BasicArcticFileScanTask(
        DefaultKeyedFile.parseBase(s),
        null,
        partitionSpec,
        expression
    )).collect(Collectors.toList());
    if (primaryKeySpec != null) {
      KeyedTableScanTask keyedTableScanTask = new NodeFileScanTask(arcticFileScanTasks);
      AdaptHiveGenericArcticDataReader genericArcticDataReader = new AdaptHiveGenericArcticDataReader(
          fileIO,
          schema,
          schema,
          primaryKeySpec,
          null,
          true,
          IdentityPartitionConverters::convertConstant
      );
      return genericArcticDataReader.readData(keyedTableScanTask);
    } else {
      GenericAdaptHiveIcebergDataReader genericArcticDataReader = new GenericAdaptHiveIcebergDataReader(
          fileIO,
          schema,
          schema,
          null,
          true,
          IdentityPartitionConverters::convertConstant,
          false
      );
      return CloseableIterable.concat(arcticFileScanTasks.stream()
          .map(s -> genericArcticDataReader.readData(s)).collect(Collectors.toList())).iterator();
    }
  }
}
