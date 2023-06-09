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
package org.apache.drill.exec.metastore;

import org.apache.commons.io.FileUtils;
import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.drill.exec.sql.TestMetastoreCommands.DIR0_1994_Q1_SEGMENT_COLUMN_STATISTICS;
import static org.apache.drill.exec.sql.TestMetastoreCommands.DIR0_1994_SEGMENT_COLUMN_STATISTICS;
import static org.apache.drill.exec.sql.TestMetastoreCommands.TABLE_COLUMN_STATISTICS;
import static org.apache.drill.exec.sql.TestMetastoreCommands.TABLE_META_INFO;
import static org.apache.drill.exec.sql.TestMetastoreCommands.getBaseTableMetadata;
import static org.apache.drill.exec.sql.TestMetastoreCommands.getColumnStatistics;
import static org.apache.drill.exec.sql.TestMetastoreCommands.getMaxLastModified;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({SlowTest.class, MetastoreTest.class})
public class TestMetastoreWithEasyFormatPlugin extends ClusterTest {

  private static final String SCHEMA_STRING = "'inline=(" +
      "`o_orderkey` INT not null, " +
      "`o_custkey` INT not null, " +
      "`o_orderstatus` VARCHAR not null, " +
      "`o_totalprice` DOUBLE not null, " +
      "`o_orderdate` DATE not null format \"yyyy-MM-dd''T''HH:mm:ss.SSSXXX\", " +
      "`o_orderpriority` VARCHAR not null, " +
      "`o_clerk` VARCHAR not null, " +
      "`o_shippriority` INT not null, " +
      "`o_comment` VARCHAR not null)'";

  private static final TupleMetadata SCHEMA = new SchemaBuilder()
      .add("o_orderkey", TypeProtos.MinorType.INT)
      .add("o_custkey", TypeProtos.MinorType.INT)
      .add("o_orderstatus", TypeProtos.MinorType.VARCHAR)
      .add("o_totalprice", TypeProtos.MinorType.FLOAT8)
      .add("o_orderdate", TypeProtos.MinorType.DATE)
      .add("o_orderpriority", TypeProtos.MinorType.VARCHAR)
      .add("o_clerk", TypeProtos.MinorType.VARCHAR)
      .add("o_shippriority", TypeProtos.MinorType.INT)
      .add("o_comment", TypeProtos.MinorType.VARCHAR)
      .addNullable("dir0", TypeProtos.MinorType.VARCHAR)
      .addNullable("dir1", TypeProtos.MinorType.VARCHAR)
      .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setUp() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    builder.configProperty(ExecConstants.ZK_ROOT, dirTestWatcher.getRootDir().getAbsolutePath());
    startCluster(builder);
  }

  @Before
  public void prepare() {
    client.alterSession(ExecConstants.METASTORE_ENABLED, true);
    client.alterSession(ExecConstants.METASTORE_USE_SCHEMA_METADATA, true);
    client.alterSession(ExecConstants.METASTORE_USE_STATISTICS_METADATA, true);
    client.alterSession(ExecConstants.SLICE_TARGET, 1);
  }

  @Test
  public void testAnalyzeOnTextTable() throws Exception {
    String tableName = "multilevel/csv";
    TableInfo tableInfo = getTableInfo(tableName, "default", "csv");

    File table = dirTestWatcher.copyResourceToRoot(Paths.get(tableName));

    Path tablePath = new Path(table.toURI().getPath());

    BaseTableMetadata expectedTableMetadata = getBaseTableMetadata(tableInfo, table, SCHEMA);

    TableInfo baseTableInfo = TableInfo.builder()
        .name(tableName)
        .storagePlugin("dfs")
        .workspace("default")
        .build();

    Map<SchemaPath, ColumnStatistics<?>> dir0CSVStats = new HashMap<>(DIR0_1994_SEGMENT_COLUMN_STATISTICS);
    dir0CSVStats.put(SchemaPath.getSimplePath("o_comment"),
        getColumnStatistics(" accounts nag slyly. ironic",
            "yly final requests over the furiously regula", 40L, TypeProtos.MinorType.VARCHAR));

    SegmentMetadata dir0 = SegmentMetadata.builder()
        .tableInfo(baseTableInfo)
        .metadataInfo(MetadataInfo.builder()
            .type(MetadataType.SEGMENT)
            .identifier("1994")
            .key("1994")
            .build())
        .path(new Path(tablePath, "1994"))
        .schema(SCHEMA)
        .lastModifiedTime(getMaxLastModified(new File(table, "1994")))
        .column(SchemaPath.getSimplePath("dir0"))
        .columnsStatistics(dir0CSVStats)
        .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(40L, TableStatisticsKind.ROW_COUNT)))
        .locations(ImmutableSet.of(
            new Path(tablePath, "1994/Q1/orders_94_q1.csv"),
            new Path(tablePath, "1994/Q2/orders_94_q2.csv"),
            new Path(tablePath, "1994/Q3/orders_94_q3.csv"),
            new Path(tablePath, "1994/Q4/orders_94_q4.csv")))
        .partitionValues(Collections.singletonList("1994"))
        .build();

    Set<Path> expectedTopLevelSegmentLocations = ImmutableSet.of(
        new Path(tablePath, "1994"),
        new Path(tablePath, "1995"),
        new Path(tablePath, "1996"));

    Set<Set<Path>> expectedSegmentFilesLocations = new HashSet<>();

    Set<Path> segmentFiles = ImmutableSet.of(
        new Path(tablePath, "1994/Q2/orders_94_q2.csv"),
        new Path(tablePath, "1994/Q4/orders_94_q4.csv"),
        new Path(tablePath, "1994/Q1/orders_94_q1.csv"),
        new Path(tablePath, "1994/Q3/orders_94_q3.csv"));
    expectedSegmentFilesLocations.add(segmentFiles);

    segmentFiles = ImmutableSet.of(
        new Path(tablePath, "1995/Q2/orders_95_q2.csv"),
        new Path(tablePath, "1995/Q4/orders_95_q4.csv"),
        new Path(tablePath, "1995/Q1/orders_95_q1.csv"),
        new Path(tablePath, "1995/Q3/orders_95_q3.csv"));
    expectedSegmentFilesLocations.add(segmentFiles);

    segmentFiles = ImmutableSet.of(
        new Path(tablePath, "1996/Q3/orders_96_q3.csv"),
        new Path(tablePath, "1996/Q2/orders_96_q2.csv"),
        new Path(tablePath, "1996/Q4/orders_96_q4.csv"),
        new Path(tablePath, "1996/Q1/orders_96_q1.csv"));
    expectedSegmentFilesLocations.add(segmentFiles);

    long dir0q1lastModified = new File(new File(new File(table, "1994"), "Q1"), "orders_94_q1.csv").lastModified();
    FileMetadata dir01994q1File = FileMetadata.builder()
        .tableInfo(baseTableInfo)
        .metadataInfo(MetadataInfo.builder()
            .type(MetadataType.FILE)
            .identifier("1994/Q1/orders_94_q1.csv")
            .key("1994")
            .build())
        .schema(SCHEMA)
        .lastModifiedTime(dir0q1lastModified)
        .columnsStatistics(DIR0_1994_Q1_SEGMENT_COLUMN_STATISTICS)
        .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(10L, TableStatisticsKind.ROW_COUNT)))
        .path(new Path(tablePath, "1994/Q1/orders_94_q1.csv"))
        .build();

    try {
      testBuilder()
          .sqlQuery("analyze table table(dfs.`%s`(schema=>%s)) refresh metadata", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.default.%s]", tableName))
          .go();

      BaseTableMetadata actualTableMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .tableMetadata(tableInfo);

      assertEquals(expectedTableMetadata, actualTableMetadata);

      List<SegmentMetadata> topSegmentMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByColumn(tableInfo, null, "`dir0`");

      SegmentMetadata actualDir0Metadata =
          topSegmentMetadata.stream()
              .filter(unit -> unit.getMetadataInfo().identifier().equals("1994"))
              .findAny().orElseThrow(() -> new AssertionError("Segment is absent"));
      Set<Path> locations = actualDir0Metadata.getLocations();
      actualDir0Metadata.toBuilder().locations(locations);
      assertEquals(dir0, actualDir0Metadata);

      Set<Path> topLevelSegmentLocations = topSegmentMetadata.stream()
          .map(SegmentMetadata::getLocation)
          .collect(Collectors.toSet());

      // verify top segments locations
      assertEquals(
          expectedTopLevelSegmentLocations,
          topLevelSegmentLocations);

      Set<Set<Path>> segmentFilesLocations = topSegmentMetadata.stream()
          .map(SegmentMetadata::getLocations)
          .collect(Collectors.toSet());

      assertEquals(
          expectedSegmentFilesLocations,
          segmentFilesLocations);

      // verify nested segments
      List<SegmentMetadata> nestedSegmentMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByColumn(tableInfo, null, "`dir1`");

      assertEquals(12, nestedSegmentMetadata.size());

      SegmentMetadata dir01994q1Segment = SegmentMetadata.builder()
          .tableInfo(baseTableInfo)
          .metadataInfo(MetadataInfo.builder()
              .type(MetadataType.SEGMENT)
              .identifier("1994/Q1")
              .key("1994")
              .build())
          .path(new Path(new Path(tablePath, "1994"), "Q1"))
          .schema(SCHEMA)
          .lastModifiedTime(getMaxLastModified(new File(new File(table, "1994"), "Q1")))
          .column(SchemaPath.getSimplePath("dir1"))
          .columnsStatistics(DIR0_1994_Q1_SEGMENT_COLUMN_STATISTICS)
          .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(10L, TableStatisticsKind.ROW_COUNT)))
          .locations(ImmutableSet.of(new Path(tablePath, "1994/Q1/orders_94_q1.csv")))
          .partitionValues(Collections.singletonList("Q1"))
          .build();

      // verify segment for 1994
      assertEquals(dir01994q1Segment,
          nestedSegmentMetadata.stream()
              .filter(unit -> unit.getMetadataInfo().identifier().equals("1994/Q1"))
              .findAny()
              .orElse(null));

      // verify files metadata
      List<FileMetadata> filesMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .filesMetadata(tableInfo, null, null);

      assertEquals(12, filesMetadata.size());

      // verify first file metadata
      assertEquals(dir01994q1File,
          filesMetadata.stream()
              .filter(unit -> unit.getMetadataInfo().identifier().equals("1994/Q1/orders_94_q1.csv"))
              .findAny()
              .orElse(null));

    } finally {
      run("analyze table dfs.`%s` drop metadata if exists", tableName);
    }
  }

  @Test
  public void testAnalyzeOnTextTableWithHeader() throws Exception {
    String tableName = "store/text/data/cars.csvh";
    File tablePath = dirTestWatcher.copyResourceToRoot(Paths.get(tableName));
    TableInfo tableInfo = getTableInfo(tableName, "default", "csvh");

    TupleMetadata schema = new SchemaBuilder()
        .add("Year", TypeProtos.MinorType.VARCHAR)
        .add("Make", TypeProtos.MinorType.VARCHAR)
        .add("Model", TypeProtos.MinorType.VARCHAR)
        .add("Description", TypeProtos.MinorType.VARCHAR)
        .add("Price", TypeProtos.MinorType.VARCHAR)
        .build();

    ImmutableMap<SchemaPath, ColumnStatistics<?>> tableColumnStatistics = ImmutableMap.<SchemaPath, ColumnStatistics<?>>builder()
        .put(SchemaPath.getSimplePath("Description"),
            getColumnStatistics("", "ac, abs, moon", 4L, TypeProtos.MinorType.VARCHAR))
        .put(SchemaPath.getSimplePath("Make"),
            getColumnStatistics("Chevy", "Jeep", 4L, TypeProtos.MinorType.VARCHAR))
        .put(SchemaPath.getSimplePath("Model"),
            getColumnStatistics("E350", "Venture \"Extended Edition, Very Large\"", 4L, TypeProtos.MinorType.VARCHAR))
        .put(SchemaPath.getSimplePath("Price"),
            getColumnStatistics("3000.00", "5000.00", 4L, TypeProtos.MinorType.VARCHAR))
        .put(SchemaPath.getSimplePath("Year"),
            getColumnStatistics("1996", "1999", 4L, TypeProtos.MinorType.VARCHAR))
        .build();

    BaseTableMetadata expectedTableMetadata = BaseTableMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(TABLE_META_INFO)
        .schema(schema)
        .location(new Path(tablePath.toURI().getPath()))
        .columnsStatistics(tableColumnStatistics)
        .metadataStatistics(Arrays.asList(new StatisticsHolder<>(4L, TableStatisticsKind.ROW_COUNT),
            new StatisticsHolder<>(MetadataType.ALL, TableStatisticsKind.ANALYZE_METADATA_LEVEL)))
        .partitionKeys(Collections.emptyMap())
        .lastModifiedTime(getMaxLastModified(tablePath))
        .build();

    try {
      testBuilder()
          .sqlQuery("analyze table dfs.`%s` refresh metadata", tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.default.%s]", tableName))
          .go();

      BaseTableMetadata actualTableMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .tableMetadata(tableInfo);

      assertEquals(expectedTableMetadata, actualTableMetadata);
    } finally {
      run("analyze table dfs.`%s` drop metadata if exists", tableName);
    }
  }

  @Test
  public void testIncrementalAnalyzeNewFile() throws Exception {
    String tableName = "multilevel/csvNewFile";

    File table = dirTestWatcher.copyResourceToTestTmp(Paths.get("multilevel/csv"), Paths.get(tableName));

    Path tablePath = new Path(table.toURI().getPath());

    TableInfo tableInfo = getTableInfo(tableName, "tmp", "csv");

    // updates statistics values due to new segment
    Map<SchemaPath, ColumnStatistics<?>> updatedStatistics = new HashMap<>(TABLE_COLUMN_STATISTICS);
    updatedStatistics.replaceAll((logicalExpressions, columnStatistics) ->
        columnStatistics.cloneWith(new ColumnStatistics<>(
            Arrays.asList(
                new StatisticsHolder<>(130L, TableStatisticsKind.ROW_COUNT),
                new StatisticsHolder<>(130L, ColumnStatisticsKind.NON_NULL_VALUES_COUNT)))));

    BaseTableMetadata expectedTableMetadata = BaseTableMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(TABLE_META_INFO)
        .schema(SCHEMA)
        .location(tablePath)
        .columnsStatistics(updatedStatistics)
        .metadataStatistics(Arrays.asList(new StatisticsHolder<>(130L, TableStatisticsKind.ROW_COUNT),
            new StatisticsHolder<>(MetadataType.ALL, TableStatisticsKind.ANALYZE_METADATA_LEVEL)))
        .partitionKeys(Collections.emptyMap())
        .lastModifiedTime(getMaxLastModified(table))
        .build();

    try {
      testBuilder()
          .sqlQuery("ANALYZE TABLE table(dfs.tmp.`%s` (schema=>%s)) REFRESH METADATA", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      List<SegmentMetadata> segmentsMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByMetadataKey(tableInfo, null, null);

      assertEquals(15, segmentsMetadata.size());

      List<FileMetadata> filesMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .filesMetadata(tableInfo, null, null);

      assertEquals(12, filesMetadata.size());

      dirTestWatcher.copyResourceToTestTmp(
          Paths.get("multilevel", "csv", "1994", "Q4", "orders_94_q4.csv"),
          Paths.get(tableName, "1994", "Q4", "orders_94_q4_1.csv"));

      testBuilder()
          .sqlQuery("ANALYZE TABLE table(dfs.tmp.`%s` (schema=>%s)) REFRESH METADATA", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      BaseTableMetadata actualTableMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .tableMetadata(tableInfo);

      assertEquals(expectedTableMetadata, actualTableMetadata);

      segmentsMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByMetadataKey(tableInfo, null, null);

      // verifies that segments count left unchanged
      assertEquals(15, segmentsMetadata.size());

      filesMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .filesMetadata(tableInfo, null, null);

      assertEquals(13, filesMetadata.size());
    } finally {
      run("analyze table dfs.tmp.`%s` drop metadata if exists", tableName);

      FileUtils.deleteQuietly(table);
    }
  }


  @Test
  public void testIncrementalAnalyzeUnchangedTable() throws Exception {
    String tableName = "multilevel/csvUnchanged";

    File table = dirTestWatcher.copyResourceToTestTmp(Paths.get("multilevel/csv"), Paths.get(tableName));

    TableInfo tableInfo = getTableInfo(tableName, "tmp", "csv");

    long lastModifiedTime = getMaxLastModified(table);

    try {
      testBuilder()
          .sqlQuery("ANALYZE TABLE table(dfs.tmp.`%s` (schema=>%s)) REFRESH METADATA", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      List<SegmentMetadata> segmentMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByMetadataKey(tableInfo, null, null);

      assertEquals(15, segmentMetadata.size());

      testBuilder()
          .sqlQuery("ANALYZE TABLE table(dfs.tmp.`%s` (schema=>%s)) REFRESH METADATA", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(false, "Table metadata is up to date, analyze wasn't performed.")
          .go();

      segmentMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByMetadataKey(tableInfo, null, null);

      assertEquals(15, segmentMetadata.size());

      long postAnalyzeLastModifiedTime = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .metastoreTableInfo(tableInfo)
          .lastModifiedTime();

      assertEquals(lastModifiedTime, postAnalyzeLastModifiedTime);
    } finally {
      run("analyze table dfs.tmp.`%s` drop metadata if exists", tableName);

      FileUtils.deleteQuietly(table);
    }
  }

  @Test
  public void testIntWithNullsPartitionPruning() throws Exception {
    String tableName = "t5";

    try {
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csvh");
      run("create table dfs.tmp.`%s/a` as\n" +
          "select 100 as mykey, cast(null as varchar) as col_notexist from cp.`tpch/nation.parquet`\n" +
          "union all\n" +
          "select cast(null as int) as mykey, 'a' as col_notexist from cp.`tpch/region.parquet`", tableName);

      run("create table dfs.tmp.`%s/b` as\n" +
          "select 200 as mykey, cast(null as varchar) as col_notexist from cp.`tpch/nation.parquet`\n" +
          "union all\n" +
          "select  cast(null as int) as mykey, 'a' as col_notexist from cp.`tpch/region.parquet`", tableName);

      testBuilder()
          .sqlQuery("analyze table table(dfs.tmp.`%s` (schema=>'inline=(mykey int, col_notexist varchar)')) REFRESH METADATA", tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      String query = "select mykey from dfs.tmp.`%s` where mykey = 100";
      long actualRowCount = queryBuilder().sql(query, tableName).run().recordCount();
      assertEquals("Row count does not match the expected value", 25, actualRowCount);

      queryBuilder().sql(query, tableName)
          .planMatcher()
          .include("usedMetastore=true", "Filter", "numFiles=1")
          .match();
    } finally {
      run("analyze table dfs.tmp.`%s` drop metadata if exists", tableName);
      run("drop table if exists dfs.tmp.`%s`", tableName);
    }
  }

  @Test
  public void testPartitionPruningWithIsNull() throws Exception {
    String tableName = "t6";

    try {
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csvh");
      run("create table dfs.tmp.`%s/a` as\n" +
          "select cast(null as int) as mykey, 'a' as col_notexist from cp.`tpch/region.parquet`", tableName);

      run("create table dfs.tmp.`%s/b` as\n" +
          "select 200 as mykey, cast(null as varchar) as col_notexist from cp.`tpch/nation.parquet`", tableName);

      testBuilder()
          .sqlQuery("analyze table table(dfs.tmp.`%s` (schema=>'inline=(mykey int, col_notexist varchar)')) REFRESH METADATA", tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      String query = "select mykey from dfs.tmp.`%s` where mykey is null";
      long actualRowCount = queryBuilder().sql(query, tableName).run().recordCount();
      assertEquals("Row count does not match the expected value", 5, actualRowCount);

      queryBuilder().sql(query, tableName)
          .planMatcher()
          .include("usedMetastore=true")
          .exclude("Filter")
          .match();
    } finally {
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      run("analyze table dfs.tmp.`%s` drop metadata if exists", tableName);
      run("drop table if exists dfs.tmp.`%s`", tableName);
    }
  }

  @Test
  public void testAnalyzeOnJsonTable() throws Exception {
    String tableName = "multilevel/json";
    TableInfo tableInfo = getTableInfo(tableName, "default", "json");

    File table = dirTestWatcher.copyResourceToRoot(Paths.get(tableName));

    Path tablePath = new Path(table.toURI().getPath());

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("dir0", TypeProtos.MinorType.VARCHAR)
        .addNullable("dir1", TypeProtos.MinorType.VARCHAR)
        .addNullable("o_orderkey", TypeProtos.MinorType.BIGINT)
        .addNullable("o_custkey", TypeProtos.MinorType.BIGINT)
        .addNullable("o_orderstatus", TypeProtos.MinorType.VARCHAR)
        .addNullable("o_totalprice", TypeProtos.MinorType.FLOAT8)
        .addNullable("o_orderdate", TypeProtos.MinorType.VARCHAR)
        .addNullable("o_orderpriority", TypeProtos.MinorType.VARCHAR)
        .addNullable("o_clerk", TypeProtos.MinorType.VARCHAR)
        .addNullable("o_shippriority", TypeProtos.MinorType.BIGINT)
        .addNullable("o_comment", TypeProtos.MinorType.VARCHAR)
        .build();

    Map<SchemaPath, ColumnStatistics<?>> tableColumnStatistics = new HashMap<>(TABLE_COLUMN_STATISTICS);
    tableColumnStatistics.put(SchemaPath.getSimplePath("o_custkey"),
        getColumnStatistics(25L,
            1498L, 120L, TypeProtos.MinorType.BIGINT));
    tableColumnStatistics.put(SchemaPath.getSimplePath("o_orderdate"),
        getColumnStatistics("1994-01-01T00:00:00.000-08:00",
            "1996-12-19T00:00:00.000-08:00", 120L, TypeProtos.MinorType.VARCHAR));
    tableColumnStatistics.put(SchemaPath.getSimplePath("o_orderkey"),
        getColumnStatistics(1L,
            1319L, 120L, TypeProtos.MinorType.BIGINT));
    tableColumnStatistics.put(SchemaPath.getSimplePath("o_shippriority"),
        getColumnStatistics(0L,
            0L, 120L, TypeProtos.MinorType.BIGINT));

    BaseTableMetadata expectedTableMetadata = BaseTableMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(TABLE_META_INFO)
        .schema(schema)
        .location(new Path(table.toURI().getPath()))
        .columnsStatistics(tableColumnStatistics)
        .metadataStatistics(Arrays.asList(new StatisticsHolder<>(120L, TableStatisticsKind.ROW_COUNT),
            new StatisticsHolder<>(MetadataType.ALL, TableStatisticsKind.ANALYZE_METADATA_LEVEL)))
        .partitionKeys(Collections.emptyMap())
        .lastModifiedTime(getMaxLastModified(table))
        .build();

    TableInfo baseTableInfo = TableInfo.builder()
        .name(tableName)
        .storagePlugin("dfs")
        .workspace("default")
        .build();

    Map<SchemaPath, ColumnStatistics<?>> dir0CSVStats = new HashMap<>(DIR0_1994_SEGMENT_COLUMN_STATISTICS);
    dir0CSVStats.put(SchemaPath.getSimplePath("o_custkey"),
        getColumnStatistics(25L,
            1469L, 40L, TypeProtos.MinorType.BIGINT));
    dir0CSVStats.put(SchemaPath.getSimplePath("o_orderdate"),
        getColumnStatistics("1994-01-01T00:00:00.000-08:00",
            "1994-12-23T00:00:00.000-08:00", 40L, TypeProtos.MinorType.VARCHAR));
    dir0CSVStats.put(SchemaPath.getSimplePath("o_orderkey"),
        getColumnStatistics(5L,
            1031L, 40L, TypeProtos.MinorType.BIGINT));
    dir0CSVStats.put(SchemaPath.getSimplePath("o_shippriority"),
        getColumnStatistics(0L,
            0L, 40L, TypeProtos.MinorType.BIGINT));

    SegmentMetadata dir0 = SegmentMetadata.builder()
        .tableInfo(baseTableInfo)
        .metadataInfo(MetadataInfo.builder()
            .type(MetadataType.SEGMENT)
            .identifier("1994")
            .key("1994")
            .build())
        .path(new Path(tablePath, "1994"))
        .schema(schema)
        .lastModifiedTime(getMaxLastModified(new File(table, "1994")))
        .column(SchemaPath.getSimplePath("dir0"))
        .columnsStatistics(dir0CSVStats)
        .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(40L, TableStatisticsKind.ROW_COUNT)))
        .locations(ImmutableSet.of(
            new Path(tablePath, "1994/Q1/orders_94_q1.json"),
            new Path(tablePath, "1994/Q2/orders_94_q2.json"),
            new Path(tablePath, "1994/Q3/orders_94_q3.json"),
            new Path(tablePath, "1994/Q4/orders_94_q4.json")))
        .partitionValues(Collections.singletonList("1994"))
        .build();

    Set<Path> expectedTopLevelSegmentLocations = ImmutableSet.of(
        new Path(tablePath, "1994"),
        new Path(tablePath, "1995"),
        new Path(tablePath, "1996"));

    Set<Set<Path>> expectedSegmentFilesLocations = new HashSet<>();

    Set<Path> segmentFiles = ImmutableSet.of(
        new Path(tablePath, "1994/Q2/orders_94_q2.json"),
        new Path(tablePath, "1994/Q4/orders_94_q4.json"),
        new Path(tablePath, "1994/Q1/orders_94_q1.json"),
        new Path(tablePath, "1994/Q3/orders_94_q3.json"));
    expectedSegmentFilesLocations.add(segmentFiles);

    segmentFiles = ImmutableSet.of(
        new Path(tablePath, "1995/Q2/orders_95_q2.json"),
        new Path(tablePath, "1995/Q4/orders_95_q4.json"),
        new Path(tablePath, "1995/Q1/orders_95_q1.json"),
        new Path(tablePath, "1995/Q3/orders_95_q3.json"));
    expectedSegmentFilesLocations.add(segmentFiles);

    segmentFiles = ImmutableSet.of(
        new Path(tablePath, "1996/Q3/orders_96_q3.json"),
        new Path(tablePath, "1996/Q2/orders_96_q2.json"),
        new Path(tablePath, "1996/Q4/orders_96_q4.json"),
        new Path(tablePath, "1996/Q1/orders_96_q1.json"));
    expectedSegmentFilesLocations.add(segmentFiles);

    Map<SchemaPath, ColumnStatistics<?>> dir0q1Stats = new HashMap<>(DIR0_1994_Q1_SEGMENT_COLUMN_STATISTICS);
    dir0q1Stats.put(SchemaPath.getSimplePath("o_custkey"),
        getColumnStatistics(392L,
            1411L, 10L, TypeProtos.MinorType.BIGINT));
    dir0q1Stats.put(SchemaPath.getSimplePath("o_orderdate"),
        getColumnStatistics("1994-01-01T00:00:00.000-08:00",
            "1994-03-26T00:00:00.000-08:00", 10L, TypeProtos.MinorType.VARCHAR));
    dir0q1Stats.put(SchemaPath.getSimplePath("o_orderkey"),
        getColumnStatistics(66L,
            833L, 10L, TypeProtos.MinorType.BIGINT));
    dir0q1Stats.put(SchemaPath.getSimplePath("o_shippriority"),
        getColumnStatistics(0L,
            0L, 10L, TypeProtos.MinorType.BIGINT));

    long dir0q1lastModified = new File(new File(new File(table, "1994"), "Q1"), "orders_94_q1.json").lastModified();
    FileMetadata dir01994q1File = FileMetadata.builder()
        .tableInfo(baseTableInfo)
        .metadataInfo(MetadataInfo.builder()
            .type(MetadataType.FILE)
            .identifier("1994/Q1/orders_94_q1.json")
            .key("1994")
            .build())
        .schema(schema)
        .lastModifiedTime(dir0q1lastModified)
        .columnsStatistics(dir0q1Stats)
        .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(10L, TableStatisticsKind.ROW_COUNT)))
        .path(new Path(tablePath, "1994/Q1/orders_94_q1.json"))
        .build();

    try {
      testBuilder()
          .sqlQuery("analyze table table(dfs.`%s`(schema=>%s)) refresh metadata", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.default.%s]", tableName))
          .go();

      BaseTableMetadata actualTableMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .tableMetadata(tableInfo);

      assertEquals(expectedTableMetadata, actualTableMetadata);

      List<SegmentMetadata> topSegmentMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByColumn(tableInfo, null, "`dir0`");

      SegmentMetadata actualDir0Metadata =
          topSegmentMetadata.stream()
              .filter(unit -> unit.getMetadataInfo().identifier().equals("1994"))
              .findAny().orElseThrow(() -> new AssertionError("Segment is absent"));
      Set<Path> locations = actualDir0Metadata.getLocations();
      actualDir0Metadata.toBuilder().locations(locations);
      assertEquals(dir0, actualDir0Metadata);

      Set<Path> topLevelSegmentLocations = topSegmentMetadata.stream()
          .map(SegmentMetadata::getLocation)
          .collect(Collectors.toSet());

      // verify top segments locations
      assertEquals(
          expectedTopLevelSegmentLocations,
          topLevelSegmentLocations);

      Set<Set<Path>> segmentFilesLocations = topSegmentMetadata.stream()
          .map(SegmentMetadata::getLocations)
          .collect(Collectors.toSet());

      assertEquals(
          expectedSegmentFilesLocations,
          segmentFilesLocations);

      // verify nested segments
      List<SegmentMetadata> nestedSegmentMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .segmentsMetadataByColumn(tableInfo, null, "`dir1`");

      assertEquals(12, nestedSegmentMetadata.size());

      SegmentMetadata dir01994q1Segment = SegmentMetadata.builder()
          .tableInfo(baseTableInfo)
          .metadataInfo(MetadataInfo.builder()
              .type(MetadataType.SEGMENT)
              .identifier("1994/Q1")
              .key("1994")
              .build())
          .path(new Path(new Path(tablePath, "1994"), "Q1"))
          .schema(schema)
          .lastModifiedTime(getMaxLastModified(new File(new File(table, "1994"), "Q1")))
          .column(SchemaPath.getSimplePath("dir1"))
          .columnsStatistics(dir0q1Stats)
          .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(10L, TableStatisticsKind.ROW_COUNT)))
          .locations(ImmutableSet.of(new Path(tablePath, "1994/Q1/orders_94_q1.json")))
          .partitionValues(Collections.singletonList("Q1"))
          .build();

      // verify segment for 1994
      assertEquals(dir01994q1Segment,
          nestedSegmentMetadata.stream()
              .filter(unit -> unit.getMetadataInfo().identifier().equals("1994/Q1"))
              .findAny()
              .orElse(null));

      // verify files metadata
      List<FileMetadata> filesMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .filesMetadata(tableInfo, null, null);

      assertEquals(12, filesMetadata.size());

      // verify first file metadata
      assertEquals(dir01994q1File,
          filesMetadata.stream()
              .filter(unit -> unit.getMetadataInfo().identifier().equals("1994/Q1/orders_94_q1.json"))
              .findAny()
              .orElse(null));

    } finally {
      run("analyze table dfs.`%s` drop metadata if exists", tableName);
    }
  }

  @Test
  public void testAnalyzeWithSampleStatistics() throws Exception {
    String tableName = "multilevel/json/1994/Q1";

    try {
      dirTestWatcher.copyResourceToRoot(Paths.get(tableName));

      client.alterSession(PlannerSettings.STATISTICS_USE.getOptionName(), true);

      testBuilder()
          .sqlQuery("ANALYZE TABLE dfs.`%s` COLUMNS(o_orderkey) REFRESH METADATA COMPUTE STATISTICS SAMPLE 95 PERCENT", tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.default.%s]", tableName))
          .go();

      String query = "select EST_NUM_NON_NULLS is not null as has_value\n" +
          "from information_schema.`columns` where table_name='%s' and column_name='o_orderkey'";

      testBuilder()
          .sqlQuery(query, tableName)
          .unOrdered()
          .baselineColumns("has_value")
          .baselineValues(true)
          .go();
    } finally {
      run("analyze table dfs.`%s` drop metadata if exists", tableName);
      client.resetSession(PlannerSettings.STATISTICS_USE.getOptionName());
    }
  }

  @Test
  public void testEmptyCSV() throws Exception {
    String tableName = "store/text/directoryWithEmptyCSV/empty.csv";
    File tablePath = dirTestWatcher.copyResourceToRoot(Paths.get(tableName));

    TableInfo tableInfo = getTableInfo(tableName, "default", "csv");

    TupleMetadata schema = new SchemaBuilder()
        .add("Description", TypeProtos.MinorType.VARCHAR)
        .build();

    ImmutableMap<SchemaPath, ColumnStatistics<?>> tableColumnStatistics = ImmutableMap.<SchemaPath, ColumnStatistics<?>>builder()
        .put(SchemaPath.getSimplePath("Description"),
            getColumnStatistics(null, null, 0L, TypeProtos.MinorType.VARCHAR))
        .build();

    BaseTableMetadata expectedTableMetadata = BaseTableMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(TABLE_META_INFO)
        .schema(schema)
        .location(new Path(tablePath.toURI().getPath()))
        .columnsStatistics(tableColumnStatistics)
        .metadataStatistics(Arrays.asList(new StatisticsHolder<>(0L, TableStatisticsKind.ROW_COUNT),
            new StatisticsHolder<>(MetadataType.ALL, TableStatisticsKind.ANALYZE_METADATA_LEVEL)))
        .partitionKeys(Collections.emptyMap())
        .lastModifiedTime(getMaxLastModified(tablePath))
        .build();
    try {
      testBuilder()
          .sqlQuery("analyze table table(dfs.`%s` (schema=>'inline=(`Description` VARCHAR not null)')) refresh metadata", tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.default.%s]", tableName))
          .go();

      MetastoreTableInfo metastoreTableInfo = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .metastoreTableInfo(tableInfo);

      assertTrue("table metadata wasn't found", metastoreTableInfo.isExists());

      BaseTableMetadata tableMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .tableMetadata(tableInfo);

      assertEquals(expectedTableMetadata, tableMetadata);

      List<FileMetadata> filesMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .filesMetadata(tableInfo, null, null);

      assertEquals(1, filesMetadata.size());

      String query = "select * from dfs.`%s`";

      queryBuilder()
          .sql(query, tableName)
          .planMatcher()
          .include("usedMetastore=true")
          .match();

      testBuilder()
          .sqlQuery(query, tableName)
          .unOrdered()
          .baselineColumns("Description")
          .expectsEmptyResultSet()
          .go();
    } finally {
      run("analyze table dfs.`%s` drop metadata if exists", tableName);
    }
  }

  @Test
  public void testNonEmptyTableWithEmptyFile() throws Exception {
    String tableName = "csv_with_empty_file";

    dirTestWatcher.copyResourceToTestTmp(Paths.get("store", "text", "directoryWithEmptyCSV", "empty.csv"), Paths.get(tableName, "empty.csv"));
    File table = dirTestWatcher.copyResourceToTestTmp(Paths.get("store", "text", "data", "nations.csv"), Paths.get(tableName, "nations.csv")).getParentFile();

    TableInfo tableInfo = getTableInfo(tableName, "tmp", "csv");

    TupleMetadata schema = new SchemaBuilder()
        .add("n_nationkey", TypeProtos.MinorType.INT)
        .add("n_name", TypeProtos.MinorType.VARCHAR)
        .add("n_regionkey", TypeProtos.MinorType.INT)
        .add("n_comment", TypeProtos.MinorType.VARCHAR)
        .build();

    Map<SchemaPath, ColumnStatistics<?>> columnStatistics = ImmutableMap.<SchemaPath, ColumnStatistics<?>>builder()
        .put(SchemaPath.getSimplePath("n_nationkey"),
            getColumnStatistics(0, 24, 25L, TypeProtos.MinorType.INT))
        .put(SchemaPath.getSimplePath("n_name"),
            getColumnStatistics("ALGERIA", "VIETNAM", 25L, TypeProtos.MinorType.VARCHAR))
        .put(SchemaPath.getSimplePath("n_regionkey"),
            getColumnStatistics(0, 4, 25L, TypeProtos.MinorType.INT))
        .put(SchemaPath.getSimplePath("n_comment"),
            getColumnStatistics("alfoxespromiseslylyaccordingtotheregularaccounts.boldrequestsalon",
                "yfinalpackages.slowfoxescajolequickly.quicklysilentplateletsbreachironicaccounts.unusualpintobe",
                25L, TypeProtos.MinorType.VARCHAR))
        .build();

    BaseTableMetadata expectedTableMetadata = BaseTableMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(TABLE_META_INFO)
        .schema(schema)
        .location(new Path(table.toURI().getPath()))
        .columnsStatistics(columnStatistics)
        .metadataStatistics(Arrays.asList(new StatisticsHolder<>(25L, TableStatisticsKind.ROW_COUNT),
            new StatisticsHolder<>(MetadataType.ALL, TableStatisticsKind.ANALYZE_METADATA_LEVEL)))
        .partitionKeys(Collections.emptyMap())
        .lastModifiedTime(getMaxLastModified(table))
        .build();

    try {
      testBuilder()
          .sqlQuery("ANALYZE TABLE table(dfs.tmp.`%s` " +
              "(schema=>'inline=(" +
              "`n_nationkey` INT not null," +
              "`n_name` VARCHAR not null," +
              "`n_regionkey` INT not null," +
              "`n_comment` VARCHAR not null)')) REFRESH METADATA", tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      MetastoreTableInfo metastoreTableInfo = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .metastoreTableInfo(tableInfo);

      assertTrue("table metadata wasn't found", metastoreTableInfo.isExists());

      BaseTableMetadata tableMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .tableMetadata(tableInfo);

      assertEquals(expectedTableMetadata, tableMetadata);

      List<FileMetadata> filesMetadata = cluster.drillbit().getContext()
          .getMetastoreRegistry()
          .get()
          .tables()
          .basicRequests()
          .filesMetadata(tableInfo, null, null);

      assertEquals(2, filesMetadata.size());

      String query = "select * from dfs.tmp.`%s`";

      queryBuilder()
          .sql(query, tableName)
          .planMatcher()
          .include("usedMetastore=true")
          .match();

      long rowCount = queryBuilder().sql(query, tableName).run().recordCount();

      assertEquals(25, rowCount);
    } finally {
      run("analyze table dfs.tmp.`%s` drop metadata if exists", tableName);
    }
  }

  @Test
  public void testFilesPruningWithLimit() throws Exception {
    String tableName = "multilevel/csvLimit";

    dirTestWatcher.copyResourceToTestTmp(Paths.get("multilevel/csv"), Paths.get(tableName));

    try {
      testBuilder()
          .sqlQuery("ANALYZE TABLE table(dfs.tmp.`%s` (schema=>%s)) REFRESH METADATA", tableName, SCHEMA_STRING)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Collected / refreshed metadata for table [dfs.tmp.%s]", tableName))
          .go();

      queryBuilder()
          .sql("select * from dfs.tmp.`%s` limit 1", tableName)
          .planMatcher()
          .include("Limit", "numFiles=1", "limit=1")
          .match();

      // each file has 10 records, so 3 files should be picked
      queryBuilder()
          .sql("select * from dfs.tmp.`%s` limit 21", tableName)
          .planMatcher()
          .include("Limit", "numFiles=3", "limit=21")
          .match();
    } finally {
      run("analyze table dfs.tmp.`%s` drop metadata if exists", tableName);
    }
  }

  private TableInfo getTableInfo(String tableName, String workspace, String type) {
    return TableInfo.builder()
        .name(tableName)
        .owner(cluster.config().getString("user.name"))
        .storagePlugin("dfs")
        .workspace(workspace)
        .type(type)
        .build();
  }
}
