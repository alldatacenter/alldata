/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.iceberg;

import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.iceberg.format.IcebergFormatPluginConfig;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.util.Text;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.HistoryEntry;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.hadoop.HadoopTables;
import org.apache.iceberg.io.FileAppender;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.types.Types;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_PLUGIN_NAME;
import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.apache.drill.test.TestBuilder.mapOfObject;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IcebergQueriesTest extends ClusterTest {
  private static Table table;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));

    StoragePluginRegistry pluginRegistry = cluster.drillbit().getContext().getStorage();
    FileSystemConfig pluginConfig = (FileSystemConfig) pluginRegistry.getPlugin(DFS_PLUGIN_NAME).getConfig();
    Map<String, FormatPluginConfig> formats = new HashMap<>(pluginConfig.getFormats());
    formats.put("iceberg", IcebergFormatPluginConfig.builder().build());
    FileSystemConfig newPluginConfig = new FileSystemConfig(
      pluginConfig.getConnection(),
      pluginConfig.getConfig(),
      pluginConfig.getWorkspaces(),
      formats,
      PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    newPluginConfig.setEnabled(pluginConfig.isEnabled());
    pluginRegistry.put(DFS_PLUGIN_NAME, newPluginConfig);

    // defining another plugin with iceberg format to ensure that DRILL-8049 is fixed
    FileSystemConfig anotherFileSystemConfig = pluginConfig.copyWithFormats(formats);
    pluginRegistry.put("dfs2", anotherFileSystemConfig);

    Configuration config = new Configuration();
    config.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);

    HadoopTables tables = new HadoopTables(config);
    Schema structSchema = new Schema(
      Types.NestedField.optional(13, "struct_int_field", Types.IntegerType.get()),
      Types.NestedField.optional(14, "struct_string_field", Types.StringType.get())
    );
    Types.ListType repeatedStructType =  Types.ListType.ofOptional(
      16, Types.StructType.of(
      Types.NestedField.optional(17, "struct_int_field", Types.IntegerType.get()),
      Types.NestedField.optional(18, "struct_string_field", Types.StringType.get())
    ));
    Schema schema = new Schema(
      Types.NestedField.optional(1, "int_field", Types.IntegerType.get()),
      Types.NestedField.optional(2, "long_field", Types.LongType.get()),
      Types.NestedField.optional(3, "float_field", Types.FloatType.get()),
      Types.NestedField.optional(4, "double_field", Types.DoubleType.get()),
      Types.NestedField.optional(5, "string_field", Types.StringType.get()),
      Types.NestedField.optional(6, "boolean_field", Types.BooleanType.get()),
      Types.NestedField.optional(26, "time_field", Types.TimeType.get()),
      Types.NestedField.optional(27, "timestamp_field", Types.TimestampType.withoutZone()),
      Types.NestedField.optional(28, "date_field", Types.DateType.get()),
      Types.NestedField.optional(29, "decimal_field", Types.DecimalType.of(4, 2)),
      Types.NestedField.optional(30, "uuid_field", Types.UUIDType.get()),
      Types.NestedField.optional(31, "fixed_field", Types.FixedType.ofLength(10)),
      Types.NestedField.optional(32, "binary_field", Types.BinaryType.get()),
      Types.NestedField.optional(7, "list_field", Types.ListType.ofOptional(
        10, Types.StringType.get())),
      Types.NestedField.optional(8, "map_field", Types.MapType.ofOptional(
        11, 12, Types.StringType.get(), Types.FloatType.get())),
      Types.NestedField.required(9, "struct_field", structSchema.asStruct()),
      Types.NestedField.required(15, "repeated_struct_field", repeatedStructType),
      Types.NestedField.required(19, "repeated_list_field", Types.ListType.ofOptional(
        20, Types.ListType.ofOptional(21, Types.StringType.get()))),
      Types.NestedField.optional(22, "repeated_map_field", Types.ListType.ofOptional(
        23, Types.MapType.ofOptional(24, 25, Types.StringType.get(), Types.FloatType.get())))
    );

    List<String> listValue = Arrays.asList("a", "b", "c");

    Map<String, Float> mapValue = new HashMap<>();
    mapValue.put("a", 0.1F);
    mapValue.put("b", 0.2F);

    Map<String, Float> secondMapValue = new HashMap<>();
    secondMapValue.put("true", 1F);
    secondMapValue.put("false", 0F);

    Record structValue = GenericRecord.create(structSchema);
    structValue.setField("struct_int_field", 123);
    structValue.setField("struct_string_field", "abc");

    Record secondStructValue = GenericRecord.create(structSchema);
    secondStructValue.setField("struct_int_field", 321);
    secondStructValue.setField("struct_string_field", "def");

    Record record = GenericRecord.create(schema);
    record.setField("int_field", 1);
    record.setField("long_field", 100L);
    record.setField("float_field", 0.5F);
    record.setField("double_field", 1.5D);
    record.setField("string_field", "abc");
    record.setField("boolean_field", true);
    record.setField("time_field", LocalTime.of(2, 42, 42));
    record.setField("timestamp_field", LocalDateTime.of(1994, 4, 18, 11, 0, 0));
    record.setField("date_field", LocalDate.of(1994, 4, 18));
    record.setField("decimal_field", new BigDecimal("12.34"));
    record.setField("uuid_field", new byte[16]);
    record.setField("fixed_field", new byte[10]);
    record.setField("binary_field", ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
    record.setField("list_field", listValue);
    record.setField("map_field", mapValue);
    record.setField("struct_field", structValue);
    record.setField("repeated_struct_field", Arrays.asList(structValue, structValue));
    record.setField("repeated_list_field", Arrays.asList(listValue, listValue));
    record.setField("repeated_map_field", Arrays.asList(mapValue, mapValue));

    Record nullsRecord = GenericRecord.create(schema);
    nullsRecord.setField("int_field", null);
    nullsRecord.setField("long_field", null);
    nullsRecord.setField("float_field", null);
    nullsRecord.setField("double_field", null);
    nullsRecord.setField("string_field", null);
    nullsRecord.setField("boolean_field", null);
    nullsRecord.setField("time_field", null);
    nullsRecord.setField("timestamp_field", null);
    nullsRecord.setField("date_field", null);
    nullsRecord.setField("decimal_field", null);
    nullsRecord.setField("uuid_field", null);
    nullsRecord.setField("fixed_field", null);
    nullsRecord.setField("binary_field", null);
    nullsRecord.setField("list_field", null);
    nullsRecord.setField("map_field", null);
    nullsRecord.setField("struct_field", GenericRecord.create(structSchema));
    nullsRecord.setField("repeated_struct_field", Collections.emptyList());
    nullsRecord.setField("repeated_list_field", Collections.emptyList());
    nullsRecord.setField("repeated_map_field", Collections.emptyList());

    Record secondRecord = GenericRecord.create(schema);
    secondRecord.setField("int_field", 988);
    secondRecord.setField("long_field", 543L);
    secondRecord.setField("float_field", Float.NaN);
    secondRecord.setField("double_field", Double.MAX_VALUE);
    secondRecord.setField("string_field", "def");
    secondRecord.setField("boolean_field", false);
    secondRecord.setField("time_field", LocalTime.of(3, 41, 53));
    secondRecord.setField("timestamp_field", LocalDateTime.of(1995, 9, 10, 9, 0, 0));
    secondRecord.setField("date_field", LocalDate.of(1995, 9, 10));
    secondRecord.setField("decimal_field", new BigDecimal("99.99"));
    secondRecord.setField("uuid_field", new byte[16]);
    secondRecord.setField("fixed_field", new byte[10]);
    secondRecord.setField("binary_field", ByteBuffer.wrap("world".getBytes(StandardCharsets.UTF_8)));
    secondRecord.setField("list_field", Arrays.asList("y", "n"));
    secondRecord.setField("map_field", secondMapValue);
    secondRecord.setField("struct_field", secondStructValue);
    secondRecord.setField("repeated_struct_field", Arrays.asList(structValue, secondStructValue));
    secondRecord.setField("repeated_list_field", Arrays.asList(listValue, Arrays.asList("y", "n")));
    secondRecord.setField("repeated_map_field", Arrays.asList(mapValue, secondMapValue));

    String location = Paths.get(dirTestWatcher.getDfsTestTmpDir().toURI().getPath(), "testAllTypes").toUri().getPath();
    table = tables.create(schema, location);

    writeParquetAndCommitDataFile(table, "allTypes", Arrays.asList(record, nullsRecord));
    writeParquetAndCommitDataFile(table, "allTypes_1", Collections.singleton(secondRecord));

    String avroLocation = Paths.get(dirTestWatcher.getDfsTestTmpDir().toURI().getPath(), "testAllTypesAvro").toUri().getPath();
    writeAndCommitDataFile(tables.create(structSchema, avroLocation), "allTypes", FileFormat.AVRO,
      Arrays.asList(structValue, GenericRecord.create(structSchema), secondStructValue));

    String orcLocation = Paths.get(dirTestWatcher.getDfsTestTmpDir().toURI().getPath(), "testAllTypesOrc").toUri().getPath();
    writeAndCommitDataFile(tables.create(structSchema, orcLocation), "allTypes", FileFormat.ORC,
      Arrays.asList(structValue, GenericRecord.create(structSchema), secondStructValue));

    String emptyTableLocation = Paths.get(dirTestWatcher.getDfsTestTmpDir().toURI().getPath(), "testAllTypesEmpty").toUri().getPath();
    tables.create(structSchema, emptyTableLocation);
  }

  private static void writeParquetAndCommitDataFile(Table table, String name, Iterable<Record> records) throws IOException {
    writeAndCommitDataFile(table, name, FileFormat.PARQUET, records);
  }

  private static void writeAndCommitDataFile(Table table, String name, FileFormat fileFormat, Iterable<Record> records) throws IOException {
    OutputFile outputFile = table.io().newOutputFile(
      new Path(table.location(), fileFormat.addExtension(name)).toUri().getPath());

    FileAppender<Record> fileAppender = new GenericAppenderFactory(table.schema())
      .newAppender(outputFile, fileFormat);
    fileAppender.addAll(records);
    fileAppender.close();

    DataFile dataFile = DataFiles.builder(table.spec())
      .withInputFile(outputFile.toInputFile())
      .withMetrics(fileAppender.metrics())
      .build();

    Transaction transaction = table.newTransaction();
    transaction.newAppend()
      .appendFile(dataFile)
      .commit();
    transaction.commitTransaction();
  }

  @Test
  public void testSerDe() throws Exception {
    String plan = queryBuilder().sql("select * from dfs.tmp.testAllTypes").explainJson();
    long count = queryBuilder().physical(plan).run().recordCount();
    assertEquals(3, count);
  }

  @Test
  public void testSelectWithSnapshotId() throws Exception {
    String snapshotQuery = "select snapshot_id from dfs.tmp.`testAllTypes#snapshots` order by committed_at limit 1";

    String query = "select * from table(dfs.tmp.testAllTypes(type => 'iceberg', snapshotId => %s))";

    long snapshotId = queryBuilder().sql(snapshotQuery).singletonLong();
    String plan = queryBuilder().sql(query, snapshotId).explainJson();
    long count = queryBuilder().physical(plan).run().recordCount();
    assertEquals(2, count);
  }

  @Test
  public void testSelectWithSnapshotAsOfTime() throws Exception {
    String snapshotQuery = "select committed_at from dfs.tmp.`testAllTypes#snapshots` order by committed_at limit 1";

    String query = "select * from table(dfs.tmp.testAllTypes(type => 'iceberg', snapshotAsOfTime => %s))";

    long snapshotId = queryBuilder().sql(snapshotQuery).singletonLong();
    String plan = queryBuilder().sql(query, snapshotId).explainJson();
    long count = queryBuilder().physical(plan).run().recordCount();
    assertEquals(2, count);
  }

  @Test
  public void testSelectFromSnapshotId() throws Exception {
    String snapshotQuery = "select snapshot_id from dfs.tmp.`testAllTypes#snapshots` order by committed_at limit 1";

    String query = "select * from table(dfs.tmp.testAllTypes(type => 'iceberg', fromSnapshotId => %s))";

    long snapshotId = queryBuilder().sql(snapshotQuery).singletonLong();
    String plan = queryBuilder().sql(query, snapshotId).explainJson();
    long count = queryBuilder().physical(plan).run().recordCount();
    assertEquals(1, count);
  }

  @Test
  public void testSelectFromSnapshotIdAndToSnapshotId() throws Exception {
    String snapshotQuery = "select snapshot_id from dfs.tmp.`testAllTypes#snapshots` order by committed_at";

    String query = "select * from table(dfs.tmp.testAllTypes(type => 'iceberg', fromSnapshotId => %s, toSnapshotId => %s))";

    DirectRowSet rowSet = queryBuilder().sql(snapshotQuery).rowSet();
    try {
      RowSetReader reader = rowSet.reader();
      assertTrue(reader.next());
      Long fromSnapshotId = (Long) reader.column(0).reader().getObject();
      assertTrue(reader.next());
      Long toSnapshotId = (Long) reader.column(0).reader().getObject();

      String plan = queryBuilder().sql(query, fromSnapshotId, toSnapshotId).explainJson();
      long count = queryBuilder().physical(plan).run().recordCount();
      assertEquals(1, count);
    } finally {
      rowSet.clear();
    }
  }

  @Test
  public void testSelectWithSnapshotIdAndSnapshotAsOfTime() throws Exception {
    String query = "select * from table(dfs.tmp.testAllTypes(type => 'iceberg', snapshotId => %s, snapshotAsOfTime => %s))";
    try {
      queryBuilder().sql(query, 123, 456).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("Both 'snapshotId' and 'snapshotAsOfTime' cannot be specified"));
    }
  }

  @Test
  public void testAllTypes() throws Exception {
    testBuilder()
      .sqlQuery("select * from dfs.tmp.testAllTypes")
      .unOrdered()
      .baselineColumns("int_field", "long_field", "float_field", "double_field", "string_field",
        "boolean_field", "time_field", "timestamp_field", "date_field", "decimal_field", "uuid_field",
        "fixed_field", "binary_field", "list_field", "map_field", "struct_field", "repeated_struct_field",
        "repeated_list_field", "repeated_map_field")
      .baselineValues(1, 100L, 0.5F, 1.5D, "abc", true, LocalTime.of(2, 42, 42),
        LocalDateTime.of(1994, 4, 18, 11, 0, 0), LocalDate.of(1994, 4, 18),
        new BigDecimal("12.34"), new byte[16], new byte[10], "hello".getBytes(StandardCharsets.UTF_8),
        listOf("a", "b", "c"),
        mapOfObject(
          new Text("a"), 0.1F,
          new Text("b"), 0.2F),
        mapOf(
          "struct_int_field", 123,
          "struct_string_field", "abc"),
        listOf(
          mapOf(
            "struct_int_field", 123,
            "struct_string_field", "abc"),
          mapOf(
            "struct_int_field", 123,
            "struct_string_field", "abc")),
        listOf(listOf("a", "b", "c"), listOf("a", "b", "c")),
        listOf(
          mapOfObject(
            new Text("a"), 0.1F,
            new Text("b"), 0.2F),
          mapOfObject(
            new Text("a"), 0.1F,
            new Text("b"), 0.2F))
        )
      .baselineValues(null, null, null, null, null, null, null, null, null, null, null, null, null,
        listOf(), mapOfObject(), mapOf(), listOf(), listOf(), listOf())
      .baselineValues(988, 543L, Float.NaN, Double.MAX_VALUE, "def", false, LocalTime.of(3, 41, 53),
        LocalDateTime.of(1995, 9, 10, 9, 0, 0), LocalDate.of(1995, 9, 10),
        new BigDecimal("99.99"), new byte[16], new byte[10], "world".getBytes(StandardCharsets.UTF_8),
        listOf("y", "n"),
        mapOfObject(
          new Text("true"), 1F,
          new Text("false"), 0F),
        mapOf(
          "struct_int_field", 321,
          "struct_string_field", "def"),
        listOf(
          mapOf(
            "struct_int_field", 123,
            "struct_string_field", "abc"),
          mapOf(
            "struct_int_field", 321,
            "struct_string_field", "def")),
        listOf(listOf("a", "b", "c"), listOf("y", "n")),
        listOf(
          mapOfObject(
            new Text("a"), 0.1F,
            new Text("b"), 0.2F),
          mapOfObject(
            new Text("true"), 1F,
            new Text("false"), 0F))
      )
      .go();
  }

  @Test
  public void testProjectingColumns() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("projection\\=struct<1: int_field: optional int, 5: string_field: optional string>")
      .match();

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("int_field", "string_field")
      .baselineValues(1, "abc")
      .baselineValues(null, null)
      .baselineValues(988, "def")
      .go();
  }

  @Test
  public void testProjectNestedColumn() throws Exception {
    String query = "select t.struct_field.struct_int_field as i, list_field[1] as l," +
      "t.repeated_struct_field[0].struct_string_field s from dfs.tmp.testAllTypes t";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("projection\\=struct<" +
        "14: list_field: optional list<string>, " +
        "16: struct_field: required struct<23: struct_int_field: optional int>, " +
        "17: repeated_struct_field: required list<struct<27: struct_string_field: optional string>>")
      .match();

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("i", "l", "s")
      .baselineValues(123, "b", "abc")
      .baselineValues(null, null, null)
      .baselineValues(321, "n", "abc")
      .go();
  }

  @Test
  public void testFilterPushdown() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes where long_field = 100";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("filter\\=ref\\(name\\=\"long_field\"\\) \\=\\= 100")
      .include("projection\\=struct<1: int_field: optional int, 2: long_field: optional long, 5: string_field: optional string>")
      .match();

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("int_field", "string_field")
      .baselineValues(1, "abc")
      .go();
  }

  @Test
  public void testEmptyResults() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes where long_field = 101";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("filter\\=ref\\(name\\=\"long_field\"\\) \\=\\= 101")
      .include("projection\\=struct<1: int_field: optional int, 2: long_field: optional long, 5: string_field: optional string>")
      .match();

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .expectsEmptyResultSet()
      .go();
  }

  @Test
  public void testFilterWithDifferentTypes() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes where long_field = '100'";

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("int_field", "string_field")
      .baselineValues(1, "abc")
      .go();
  }

  @Test
  public void testSelectEntriesMetadata() throws Exception {
    String query = "select * from dfs.tmp.`testAllTypes#entries`";

    long count = queryBuilder().sql(query).run().recordCount();
    assertEquals(2, count);
  }

  @Test
  public void testSelectFilesMetadata() throws Exception {
    String query = "select * from dfs.tmp.`testAllTypes#files`";

    long count = queryBuilder().sql(query).run().recordCount();
    assertEquals(2, count);
  }

  @Test
  public void testSelectHistoryMetadata() throws Exception {
    String query = "select * from dfs.tmp.`testAllTypes#history`";

    List<HistoryEntry> entries = table.history();

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("made_current_at", "snapshot_id", "parent_id", "is_current_ancestor")
      .baselineValues(LocalDateTime.ofInstant(Instant.ofEpochMilli(entries.get(0).timestampMillis()), ZoneId.of("UTC")),
        entries.get(0).snapshotId(), null, true)
      .baselineValues(LocalDateTime.ofInstant(Instant.ofEpochMilli(entries.get(1).timestampMillis()), ZoneId.of("UTC")),
        entries.get(1).snapshotId(), entries.get(0).snapshotId(), true)
      .go();
  }

  @Test
  public void testSelectSnapshotsMetadata() throws Exception {
    String query = "select * from dfs.tmp.`testAllTypes#snapshots`";

    List<Snapshot> snapshots = new ArrayList<>();
    table.snapshots().forEach(snapshots::add);

    JsonStringHashMap<Object, Object> summaryMap = new JsonStringHashMap<>();
    snapshots.get(0).summary().forEach((k, v) ->
      summaryMap.put(new Text(k.getBytes(StandardCharsets.UTF_8)), new Text(v.getBytes(StandardCharsets.UTF_8))));

    JsonStringHashMap<Object, Object> secondSummaryMap = new JsonStringHashMap<>();
    snapshots.get(1).summary().forEach((k, v) ->
      secondSummaryMap.put(new Text(k.getBytes(StandardCharsets.UTF_8)), new Text(v.getBytes(StandardCharsets.UTF_8))));

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("committed_at", "snapshot_id", "parent_id", "operation", "manifest_list", "summary")
      .baselineValues(LocalDateTime.ofInstant(Instant.ofEpochMilli(snapshots.get(0).timestampMillis()), ZoneId.of("UTC")),
        snapshots.get(0).snapshotId(), snapshots.get(0).parentId(), snapshots.get(0).operation(), snapshots.get(0).manifestListLocation(), summaryMap)
      .baselineValues(LocalDateTime.ofInstant(Instant.ofEpochMilli(snapshots.get(1).timestampMillis()), ZoneId.of("UTC")),
        snapshots.get(1).snapshotId(), snapshots.get(1).parentId(), snapshots.get(1).operation(), snapshots.get(1).manifestListLocation(), secondSummaryMap)
      .go();
  }

  @Test
  public void testSelectManifestsMetadata() throws Exception {
    String query = "select * from dfs.tmp.`testAllTypes#manifests`";

    long count = queryBuilder().sql(query).run().recordCount();
    assertEquals(2, count);
  }

  @Test
  public void testSelectPartitionsMetadata() throws Exception {
    String query = "select * from dfs.tmp.`testAllTypes#partitions`";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("record_count", "file_count")
      .baselineValues(3L, 2)
      .go();
  }

  @Test
  public void testSchemaProvisioning() throws Exception {
    String query = "select int_field, string_field from table(dfs.tmp.testAllTypes(schema => 'inline=(int_field varchar not null default `error`)'))";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("projection\\=struct<1: int_field: optional int, 5: string_field: optional string>")
      .match();

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("int_field", "string_field")
      .baselineValues("1", "abc")
      .baselineValues("error", null)
      .baselineValues("988", "def")
      .go();
  }

  @Test
  public void testSelectAvroFormat() throws Exception {
    testBuilder()
      .sqlQuery("select * from dfs.tmp.testAllTypesAvro")
      .unOrdered()
      .baselineColumns("struct_int_field", "struct_string_field")
      .baselineValues(123, "abc")
      .baselineValues(null, null)
      .baselineValues(321, "def")
      .go();
  }

  @Test
  public void testSelectOrcFormat() throws Exception {
    testBuilder()
      .sqlQuery("select * from dfs.tmp.testAllTypesOrc")
      .unOrdered()
      .baselineColumns("struct_int_field", "struct_string_field")
      .baselineValues(123, "abc")
      .baselineValues(null, null)
      .baselineValues(321, "def")
      .go();
  }

  @Test
  public void testSelectEmptyTable() throws Exception {
    testBuilder()
      .sqlQuery("select * from dfs.tmp.testAllTypesEmpty")
      .unOrdered()
      .expectsEmptyResultSet()
      .go();
  }

  @Test
  public void testLimit() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes limit 1";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Limit\\(fetch\\=\\[1\\]\\)")
      .include("maxRecords\\=1")
      .match();

    long count = queryBuilder().sql(query).run().recordCount();
    assertEquals(1, count);
  }

  @Test
  public void testLimitWithFilter() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes where int_field = 1 limit 1";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Limit\\(fetch\\=\\[1\\]\\)")
      .include("maxRecords\\=1")
      .include("filter\\=ref\\(name=\"int_field\"\\) \\=\\= 1")
      .match();

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("int_field", "string_field")
      .baselineValues(1, "abc")
      .go();
  }

  @Test
  public void testNoLimitWithSort() throws Exception {
    String query = "select int_field, string_field from dfs.tmp.testAllTypes order by int_field limit 1";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Limit\\(fetch\\=\\[1\\]\\)")
      .include("maxRecords\\=-1")
      .match();

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("int_field", "string_field")
      .baselineValues(1, "abc")
      .go();
  }

  @Test
  public void testLateralSql() throws Exception {
    String sql =
      "SELECT t.c_name, t2.ord.o_shop AS o_shop\n" +
        "FROM cp.`lateraljoin/nested-customer.json` t,\n" +
        "unnest(t.orders) t2(ord)\n" +
        "LIMIT 1";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("c_name", "o_shop")
      .baselineValues("customer1", "Meno Park 1st")
      .go();
  }

  @Test
  public void testLateralSqlIceberg() throws Exception {
    String sql =
      "SELECT t.int_field, t2.ord.struct_string_field struct_string_field\n" +
        "FROM dfs.tmp.testAllTypes t,\n" +
        "unnest(t.repeated_struct_field) t2(ord)\n" +
        "ORDER BY t.int_field\n" +
        "LIMIT 1";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("int_field", "struct_string_field")
      .baselineValues(1, "abc")
      .go();
  }

  @Test
  public void testFilterPushCorrelate() throws Exception {
    String sql =
      "SELECT t.c_name, t2.ord.o_shop AS o_shop\n" +
        "FROM cp.`lateraljoin/nested-customer.json` t,\n" +
        "unnest(t.orders) t2(ord)\n" +
        "WHERE t.c_name='customer1' AND t2.ord.o_shop='Meno Park 1st'";

    testBuilder()
      .unOrdered()
      .sqlQuery(sql)
      .baselineColumns("c_name", "o_shop")
      .baselineValues("customer1", "Meno Park 1st")
      .go();
  }
}
