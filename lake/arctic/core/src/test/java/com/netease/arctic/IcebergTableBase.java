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

package com.netease.arctic;

import com.google.common.collect.Maps;
import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.data.file.DeleteFileWithSequence;
import com.netease.arctic.scan.CombinedIcebergScanTask;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.deletes.EqualityDeleteWriter;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.deletes.PositionDeleteWriter;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.types.Types;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @deprecated since 0.4.1, will be removed in 0.5.0; use {@link com.netease.arctic.catalog.TableTestBase} instead.
 */
@Deprecated
public class IcebergTableBase {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  protected Table unPartitionTable;
  protected Table partitionTable;
  protected String unPartitionName = "un_partition_table";
  protected String partitionName = "un_partition_table";
  protected Schema unPartitionSchema = new Schema(
      Types.NestedField.required(1, "id", Types.LongType.get())
  );
  protected Schema partitionSchema = new Schema(
      Types.NestedField.required(1, "id", Types.LongType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get())
  );
  protected CombinedIcebergScanTask unPartitionAllFileTask;
  protected CombinedIcebergScanTask unPartitionOnlyDataTask;
  protected CombinedIcebergScanTask partitionAllFileTask;
  protected CombinedIcebergScanTask partitionOnlyDataTask;
  private HadoopCatalog catalog;

  @Before
  public void setupTables() throws IOException {
    Configuration conf = new Configuration();
    File tableDir = temp.newFolder();
    catalog = new HadoopCatalog(conf, tableDir.getAbsolutePath());
    initUnPartitionTable();
    initPartitionTable();
  }

  private void initUnPartitionTable() throws IOException {
    //create table
    TableIdentifier tableIdentifier = TableIdentifier.of(unPartitionName);
    if (catalog.tableExists(tableIdentifier)) {
      unPartitionTable = catalog.loadTable(tableIdentifier);
    } else {
      Map<String, String> map = Maps.newHashMap();
      map.put(TableProperties.FORMAT_VERSION, "2");
      unPartitionTable = catalog.createTable(tableIdentifier, unPartitionSchema, PartitionSpec.unpartitioned(), map);
    }

    Record record = GenericRecord.create(unPartitionSchema);

    DataFileWithSequence avroData = new DataFileWithSequence(insert(Arrays.asList(
        record.copy("id", 1L)
    ), FileFormat.AVRO, unPartitionSchema), 1L);

    DataFileWithSequence parquetData = new DataFileWithSequence(insert(Arrays.asList(
        record.copy("id", 2L)
    ), FileFormat.PARQUET, unPartitionSchema), 2L);

    DataFileWithSequence orcData = new DataFileWithSequence(insert(Arrays.asList(
        record.copy("id", 3L)
    ), FileFormat.ORC, unPartitionSchema), 3L);

    DeleteFileWithSequence eqDeleteFile = new DeleteFileWithSequence(eqDelete(Arrays.asList(
        record.copy("id", 1L)
    ), FileFormat.PARQUET, unPartitionSchema), 4L);

    DeleteFileWithSequence posDeleteFile = new DeleteFileWithSequence(posDelete(Arrays.asList(
        PositionDelete.<Record>create().set(parquetData.path(), 0, record.copy("id", 2L))
    ), FileFormat.AVRO, unPartitionSchema), 5L);

    unPartitionAllFileTask = new CombinedIcebergScanTask(
        new DataFileWithSequence[]{avroData, parquetData, orcData},
        new DeleteFileWithSequence[]{eqDeleteFile, posDeleteFile},
        PartitionSpec.unpartitioned(),
        null
    );

    unPartitionOnlyDataTask = new CombinedIcebergScanTask(
        new DataFileWithSequence[]{avroData, parquetData, orcData},
        null,
        PartitionSpec.unpartitioned(),
        null
    );
  }

  private void initPartitionTable() throws IOException {
    //create table
    TableIdentifier tableIdentifier = TableIdentifier.of(partitionName);
    PartitionSpec partitionSpec = PartitionSpec.builderFor(partitionSchema).identity("name").build();
    if (catalog.tableExists(tableIdentifier)) {
      partitionTable = catalog.loadTable(tableIdentifier);
    } else {
      Map<String, String> map = Maps.newHashMap();
      map.put(TableProperties.FORMAT_VERSION, "2");
      partitionTable = catalog.createTable(tableIdentifier, partitionSchema, partitionSpec, map);
    }

    Record record = GenericRecord.create(partitionSchema);

    DataFileWithSequence avroData = new DataFileWithSequence(insert(Arrays.asList(
        record.copy("id", 1L, "name", "1")
    ), FileFormat.AVRO, partitionSchema), 1L);

    DataFileWithSequence parquetData = new DataFileWithSequence(insert(Arrays.asList(
        record.copy("id", 2L, "name", "2")
    ), FileFormat.PARQUET, partitionSchema), 2L);

    DataFileWithSequence orcData = new DataFileWithSequence(insert(Arrays.asList(
        record.copy("id", 3L, "name", "3")
    ), FileFormat.ORC, partitionSchema), 3L);

    DeleteFileWithSequence eqDeleteFile = new DeleteFileWithSequence(eqDelete(Arrays.asList(
        record.copy("id", 1L, "name", "1")
    ), FileFormat.PARQUET, partitionSchema), 4L);

    DeleteFileWithSequence posDeleteFile = new DeleteFileWithSequence(posDelete(Arrays.asList(
        PositionDelete.<Record>create().set(
            parquetData.path(),
            0,
            record.copy("id", 2L, "name", "2"))
    ), FileFormat.AVRO, partitionSchema), 5L);

    partitionAllFileTask = new CombinedIcebergScanTask(
        new DataFileWithSequence[]{avroData, parquetData, orcData},
        new DeleteFileWithSequence[]{eqDeleteFile, posDeleteFile},
        PartitionSpec.unpartitioned(),
        null
    );

    partitionOnlyDataTask = new CombinedIcebergScanTask(
        new DataFileWithSequence[]{avroData, parquetData, orcData},
        null,
        PartitionSpec.unpartitioned(),
        null
    );
  }

  private DataFile insert(List<Record> records, FileFormat fileFormat, Schema schema) throws IOException {
    GenericAppenderFactory fileAppenderFactory = new GenericAppenderFactory(schema);
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(unPartitionTable, 0, 1)
        .format(fileFormat).build();
    DataWriter<Record> recordDataWriter =
        fileAppenderFactory.newDataWriter(outputFileFactory.newOutputFile(), fileFormat, null);
    for (Record record : records) {
      recordDataWriter.write(record);
    }
    recordDataWriter.close();
    return recordDataWriter.toDataFile();
  }

  private DeleteFile eqDelete(List<Record> records, FileFormat fileFormat, Schema schema) throws IOException {
    GenericAppenderFactory fileAppenderFactory = new GenericAppenderFactory(schema, PartitionSpec.unpartitioned(),
        new int[] {1}, schema, schema);
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(unPartitionTable, 0, 1)
        .format(fileFormat).build();
    EqualityDeleteWriter<Record> recordDataWriter =
        fileAppenderFactory.newEqDeleteWriter(outputFileFactory.newOutputFile(), fileFormat, null);
    recordDataWriter.write(records);
    recordDataWriter.close();
    return recordDataWriter.toDeleteFile();
  }

  private DeleteFile posDelete(List<PositionDelete<Record>> positionDeletes, FileFormat fileFormat, Schema schema)
      throws IOException {
    GenericAppenderFactory fileAppenderFactory = new GenericAppenderFactory(schema);
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(unPartitionTable, 0, 1)
        .format(fileFormat).build();
    PositionDeleteWriter<Record> recordDataWriter =
        fileAppenderFactory.newPosDeleteWriter(outputFileFactory.newOutputFile(), fileFormat, null);
    recordDataWriter.write(positionDeletes);
    recordDataWriter.close();
    return recordDataWriter.toDeleteFile();
  }

  @After
  public void clean() {
    temp.delete();
  }
}
