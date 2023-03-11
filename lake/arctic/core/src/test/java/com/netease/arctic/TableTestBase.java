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

package com.netease.arctic;

import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.iceberg.optimize.InternalRecordWrapper;
import com.netease.arctic.io.reader.GenericArcticDataReader;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScan;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.deletes.EqualityDeleteWriter;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.deletes.PositionDeleteWriter;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Multimap;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.ArrayUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_CATALOG_NAME;
import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_DB_NAME;

/**
 * @deprecated since 0.4.1, will be removed in 0.5.0; use {@link com.netease.arctic.catalog.TableTestBase} instead.
 */
@Deprecated
public class TableTestBase {

  public static final Schema TABLE_SCHEMA = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get()),
      Types.NestedField.required(3, "op_time", Types.TimestampType.withoutZone())
  );
  public static final Schema UNION_TABLE_SCHEMA = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get()),
      Types.NestedField.required(3, "time", Types.StringType.get()),
      Types.NestedField.required(4, "num", Types.IntegerType.get())
  );
  public static final PartitionSpec SPEC = PartitionSpec.builderFor(TABLE_SCHEMA)
      .day("op_time").build();
  public static final PartitionSpec UNION_SPEC = PartitionSpec.builderFor(UNION_TABLE_SCHEMA)
      .identity("time").identity("num").build();
  protected static final MockArcticMetastoreServer AMS = MockArcticMetastoreServer.getInstance();
  protected static final TableIdentifier TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, "test_table");
  protected static final TableIdentifier PK_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, "test_pk_table");
  protected static final TableIdentifier NO_PARTITION_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, "test_no_partition_table");
  protected static final TableIdentifier PK_UPSERT_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, "test_pk_upsert_table");
  protected static final TableIdentifier PK_NO_PARTITION_UPSERT_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, "test_no_partition_pk_upsert_table");
  protected static final TableIdentifier PK_UNION_PARTITION_UPSERT_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, "test_union_partition_pk_upsert_table");
  protected static final Record RECORD = GenericRecord.create(TABLE_SCHEMA);
  protected static final Schema POS_DELETE_SCHEMA = new Schema(
      MetadataColumns.DELETE_FILE_PATH,
      MetadataColumns.DELETE_FILE_POS
  );
  protected static final PrimaryKeySpec PRIMARY_KEY_SPEC = PrimaryKeySpec.builderFor(TABLE_SCHEMA)
      .addColumn("id").build();
  protected static final DataFile FILE_A = DataFiles.builder(SPEC)
      .withPath("/path/to/data-a.parquet")
      .withFileSizeInBytes(10)
      .withPartitionPath("op_time_day=2022-01-01") // easy way to set partition data for now
      .withRecordCount(2) // needs at least one record or else metrics will filter it out
      .build();
  protected static final DataFile FILE_B = DataFiles.builder(SPEC)
      .withPath("/path/to/data-b.parquet")
      .withFileSizeInBytes(10)
      .withPartitionPath("op_time_day=2022-01-02") // easy way to set partition data for now
      .withRecordCount(2) // needs at least one record or else metrics will filter it out
      .build();
  protected static final DataFile FILE_C = DataFiles.builder(SPEC)
      .withPath("/path/to/data-c.parquet")
      .withFileSizeInBytes(10)
      .withPartitionPath("op_time_day=2022-01-03") // easy way to set partition data for now
      .withRecordCount(2) // needs at least one record or else metrics will filter it out
      .build();
  protected static final DataFile FILE_D = DataFiles.builder(SPEC)
      .withPath("/path/to/data-d.parquet")
      .withFileSizeInBytes(10)
      .withPartitionPath("op_time_day=2022-01-03") // easy way to set partition data for now
      .withRecordCount(2) // needs at least one record or else metrics will filter it out
      .build();
  private static final Logger LOG = LoggerFactory.getLogger(TableTestBase.class);
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  protected ArcticCatalog testCatalog;
  protected UnkeyedTable testTable;
  protected KeyedTable testKeyedTable;
  protected KeyedTable testNoPartitionTable;
  protected KeyedTable testKeyedUpsertTable;
  protected KeyedTable testKeyedNoPartitionUpsertTable;
  protected KeyedTable testKeyedUnionPartitionUpsertTable;
  protected File tableDir = null;

  public static List<Record> readKeyedTable(KeyedTable keyedTable) {
    GenericArcticDataReader reader = new GenericArcticDataReader(
        keyedTable.io(),
        keyedTable.schema(),
        keyedTable.schema(),
        keyedTable.primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant
    );
    List<Record> result = Lists.newArrayList();
    try (CloseableIterable<CombinedScanTask> combinedScanTasks = keyedTable.newScan().planTasks()) {
      combinedScanTasks.forEach(combinedTask -> combinedTask.tasks().forEach(scTask -> {
        try (CloseableIterator<Record> records = reader.readData(scTask)) {
          while (records.hasNext()) {
            result.add(records.next());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  public static Pair<List<Record>, List<String>> readKeyedTableWithFilters(KeyedTable keyedTable, Expression expr) {
    GenericArcticDataReader reader = new GenericArcticDataReader(
        keyedTable.io(),
        keyedTable.schema(),
        keyedTable.schema(),
        keyedTable.primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant
    );
    List<Record> result = Lists.newArrayList();
    List<String> path = Lists.newArrayList();
    KeyedTableScan keyedTableScan = keyedTable.newScan();
    keyedTableScan = keyedTableScan.filter(expr);
    try (CloseableIterable<CombinedScanTask> combinedScanTasks = keyedTableScan.planTasks()) {
      combinedScanTasks.forEach(combinedTask -> combinedTask.tasks().forEach(scTask -> {
        scTask.insertTasks().stream().forEach(fst -> path.add(fst.file().path().toString()));
        scTask.arcticEquityDeletes().stream().forEach(fst -> path.add(fst.file().path().toString()));
        try (CloseableIterator<Record> records = reader.readData(scTask)) {
          while (records.hasNext()) {
            result.add(records.next());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new ImmutablePair<>(result, path);
  }

  public static Record newGenericRecord(Schema schema, Object... fields) {
    GenericRecord record = GenericRecord.create(schema);
    for (int i = 0; i < schema.columns().size(); i++) {
      record.set(i, fields[i]);
    }
    return record;
  }

  public static Record newGenericRecord(Types.StructType type, Object... fields) {
    GenericRecord record = GenericRecord.create(type);
    for (int i = 0; i < type.fields().size(); i++) {
      record.set(i, fields[i]);
    }
    return record;
  }

  public static LocalDateTime quickDate(int day) {
    return LocalDateTime.of(2020, 1, day, 0, 0);
  }

  public static StructLike partitionData(Schema tableSchema, PartitionSpec spec, Object... partitionValues) {
    GenericRecord record = GenericRecord.create(tableSchema);
    int index = 0;
    Set<Integer> partitionField = Sets.newHashSet();
    spec.fields().forEach(f -> partitionField.add(f.sourceId()));
    List<Types.NestedField> tableFields = tableSchema.columns();
    for (int i = 0; i < tableFields.size(); i++) {
      // String sourceColumnName = tableSchema.findColumnName(i);
      Types.NestedField sourceColumn = tableFields.get(i);
      if (partitionField.contains(sourceColumn.fieldId())) {
        Object partitionVal = partitionValues[index];
        index++;
        record.set(i, partitionVal);
      } else {
        record.set(i, 0);
      }
    }

    PartitionKey pd = new PartitionKey(spec, tableSchema);
    InternalRecordWrapper wrapper = new InternalRecordWrapper(tableSchema.asStruct());
    wrapper = wrapper.wrap(record);
    pd.partition(wrapper);
    return pd;
  }

  public static List<DataFile> writeBaseNoCommit(KeyedTable table, long txId, List<Record> records) {
    try (GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(table)
        .withTransactionId(txId).buildBaseWriter()) {
      records.forEach(d -> {
        try {
          writer.write(d);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
      WriteResult result = writer.complete();
      return Arrays.asList(result.dataFiles());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static DataFile writeNewDataFile(Table table, List<Record> records, StructLike partitionData)
      throws IOException {
    GenericAppenderFactory appenderFactory = new GenericAppenderFactory(table.schema(), table.spec());
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(table, table.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile();

    DataWriter<Record> writer = appenderFactory
        .newDataWriter(outputFile, FileFormat.PARQUET, partitionData);

    for (Record record : records) {
      writer.write(record);
    }
    writer.close();
    return writer.toDataFile();
  }

  public static DeleteFile writeEqDeleteFile(Table table, List<Record> records, StructLike partitionData)
      throws IOException {
    List<Integer> equalityFieldIds = Lists.newArrayList(table.schema().findField("id").fieldId());
    Schema eqDeleteRowSchema = table.schema().select("id");
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(table.schema(), table.spec(),
            ArrayUtil.toIntArray(equalityFieldIds), eqDeleteRowSchema, null);
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(table, table.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile(table.spec(), partitionData);

    EqualityDeleteWriter<Record> writer = appenderFactory
        .newEqDeleteWriter(outputFile, FileFormat.PARQUET, partitionData);

    for (Record record : records) {
      writer.write(record);
    }
    writer.close();
    return writer.toDeleteFile();
  }

  public static DeleteFile writePosDeleteFile(
      Table table, Multimap<String, Long> file2Positions,
      StructLike partitionData) throws IOException {
    GenericAppenderFactory appenderFactory =
        new GenericAppenderFactory(table.schema(), table.spec());
    OutputFileFactory outputFileFactory =
        OutputFileFactory.builderFor(table, table.spec().specId(), 1)
            .build();
    EncryptedOutputFile outputFile = outputFileFactory.newOutputFile(table.spec(), partitionData);

    PositionDeleteWriter<Record> writer = appenderFactory
        .newPosDeleteWriter(outputFile, FileFormat.PARQUET, partitionData);
    for (Map.Entry<String, Collection<Long>> entry : file2Positions.asMap().entrySet()) {
      String filePath = entry.getKey();
      Collection<Long> positions = entry.getValue();
      for (Long position : positions) {
        PositionDelete<Record> positionDelete = PositionDelete.create();
        positionDelete.set(filePath, position, null);
        writer.write(positionDelete);
      }
    }
    writer.close();
    return writer.toDeleteFile();
  }

  @Before
  public void setupTables() throws Exception {
    LOG.info("setupTables start");
    testCatalog = CatalogLoader.load(AMS.getUrl());
    tableDir = temp.newFolder();

    String db = TABLE_ID.getDatabase();
    if (!testCatalog.listDatabases().contains(db)) {
      testCatalog.createDatabase(db);
    }

    testTable = testCatalog
        .newTableBuilder(TABLE_ID, TABLE_SCHEMA)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/table")
        .withPartitionSpec(SPEC)
        .create().asUnkeyedTable();

    testKeyedTable = testCatalog
        .newTableBuilder(PK_TABLE_ID, TABLE_SCHEMA)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/pk_table")
        .withPartitionSpec(SPEC)
        .withPrimaryKeySpec(PRIMARY_KEY_SPEC)
        .create().asKeyedTable();

    testNoPartitionTable = testCatalog
        .newTableBuilder(NO_PARTITION_TABLE_ID, TABLE_SCHEMA)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/no_partition_table")
        .withPrimaryKeySpec(PRIMARY_KEY_SPEC)
        .create().asKeyedTable();

    testKeyedUpsertTable = testCatalog
        .newTableBuilder(PK_UPSERT_TABLE_ID, TABLE_SCHEMA)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/pk_upsert_table")
        .withProperty(TableProperties.UPSERT_ENABLED, "true")
        .withPartitionSpec(SPEC)
        .withPrimaryKeySpec(PRIMARY_KEY_SPEC)
        .create().asKeyedTable();

    testKeyedNoPartitionUpsertTable = testCatalog
        .newTableBuilder(PK_NO_PARTITION_UPSERT_TABLE_ID, TABLE_SCHEMA)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/pk_no_partition_upsert_table")
        .withProperty(TableProperties.UPSERT_ENABLED, "true")
        .withPrimaryKeySpec(PRIMARY_KEY_SPEC)
        .create().asKeyedTable();

    testKeyedUnionPartitionUpsertTable = testCatalog
        .newTableBuilder(PK_UNION_PARTITION_UPSERT_TABLE_ID, UNION_TABLE_SCHEMA)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/pk_union_partition_upsert_table")
        .withProperty(TableProperties.UPSERT_ENABLED, "true")
        .withPartitionSpec(UNION_SPEC)
        .withPrimaryKeySpec(PRIMARY_KEY_SPEC)
        .create().asKeyedTable();

    this.before();
    LOG.info("setupTables end");
  }

  public void before() throws Exception {
    // implement for sub case
  }

  @After
  public void clearTable() {
    LOG.info("clearTable start");
    testCatalog.dropTable(TABLE_ID, true);
    AMS.handler().getTableCommitMetas().remove(TABLE_ID.buildTableIdentifier());

    testCatalog.dropTable(PK_TABLE_ID, true);
    AMS.handler().getTableCommitMetas().remove(PK_TABLE_ID.buildTableIdentifier());

    testCatalog.dropTable(NO_PARTITION_TABLE_ID, true);
    AMS.handler().getTableCommitMetas().remove(NO_PARTITION_TABLE_ID.buildTableIdentifier());

    testCatalog.dropTable(PK_UPSERT_TABLE_ID, true);
    AMS.handler().getTableCommitMetas().remove(PK_UPSERT_TABLE_ID.buildTableIdentifier());

    testCatalog.dropTable(PK_NO_PARTITION_UPSERT_TABLE_ID, true);
    AMS.handler().getTableCommitMetas().remove(PK_NO_PARTITION_UPSERT_TABLE_ID.buildTableIdentifier());
    testCatalog.dropTable(PK_UNION_PARTITION_UPSERT_TABLE_ID, true);
    AMS.handler().getTableCommitMetas().remove(PK_UNION_PARTITION_UPSERT_TABLE_ID.buildTableIdentifier());
    LOG.info("clearTable end");
  }

  public List<DataFile> writeBase(TableIdentifier identifier, List<Record> records) {
    KeyedTable table = testCatalog.loadTable(identifier).asKeyedTable();
    long txId = table.beginTransaction("");
    try (GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(table)
        .withTransactionId(txId).buildBaseWriter()) {
      records.forEach(d -> {
        try {
          writer.write(d);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
      WriteResult result = writer.complete();
      AppendFiles appendFiles = table.baseTable().newAppend();
      Arrays.stream(result.dataFiles()).forEach(appendFiles::appendFile);
      appendFiles.commit();
      return Arrays.asList(result.dataFiles());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<DataFile> writeChange(TableIdentifier identifier, ChangeAction action, List<Record> records) {
    KeyedTable table = testCatalog.loadTable(identifier).asKeyedTable();
    try (GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(table)
        .withChangeAction(action)
        .buildChangeWriter()) {
      records.forEach(d -> {
        try {
          writer.write(d);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      WriteResult result = writer.complete();
      AppendFiles appendFiles = table.changeTable().newAppend();
      Arrays.stream(result.dataFiles()).forEach(appendFiles::appendFile);
      appendFiles.commit();
      return Arrays.asList(result.dataFiles());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
