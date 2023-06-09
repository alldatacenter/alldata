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
package org.apache.drill.exec.sql;

import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.record.metadata.PrimitiveColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ischema.InfoSchemaConstants;
import org.apache.drill.metastore.Metastore;
import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({SqlTest.class, MetastoreTest.class, UnlikelyTest.class})
public class TestInfoSchemaWithMetastore extends ClusterTest {

  private static final List<String> TABLES_COLUMNS = Arrays.asList(
    InfoSchemaConstants.SHRD_COL_TABLE_CATALOG,
    InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA,
    InfoSchemaConstants.SHRD_COL_TABLE_NAME,
    InfoSchemaConstants.TBLS_COL_TABLE_TYPE,
    InfoSchemaConstants.TBLS_COL_TABLE_SOURCE,
    InfoSchemaConstants.TBLS_COL_LOCATION,
    InfoSchemaConstants.TBLS_COL_NUM_ROWS,
    InfoSchemaConstants.TBLS_COL_LAST_MODIFIED_TIME);

  private static Metastore metastore;

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    builder.configProperty(ExecConstants.ZK_ROOT, dirTestWatcher.getRootDir().getAbsolutePath());
    builder.sessionOption(ExecConstants.METASTORE_ENABLED, true);
    startCluster(builder);
    MetastoreRegistry metastoreRegistry = client.cluster().drillbit().getContext().getMetastoreRegistry();
    metastore = metastoreRegistry.get();
  }

  @Test
  public void testTableNoStats() throws Exception {
    String tableName = "table_no_stats";
    BaseTableMetadata table = BaseTableMetadata.builder()
      .tableInfo(TableInfo.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .name(tableName)
        .type("PARQUET")
        .build())
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.TABLE)
        .key(MetadataInfo.GENERAL_INFO_KEY)
        .build())
      .location(new Path("/tmp", tableName))
      .metadataStatistics(Collections.emptyList())
      .columnsStatistics(Collections.emptyMap())
      .partitionKeys(Collections.emptyMap())
      .build();

    metastore.tables().modify()
      .overwrite(table.toMetadataUnit())
      .execute();

    client.testBuilder()
      .sqlQuery("select %s from information_schema.`tables` where table_name = '%s'",
        String.join(", ", TABLES_COLUMNS), tableName)
      .unOrdered()
      .baselineColumns(TABLES_COLUMNS.toArray(new String[0]))
      .baselineValues("DRILL", "dfs.tmp", tableName, "TABLE", table.getTableInfo().type(),
        table.getLocation().toUri().toString(), null, null)
      .go();
  }

  @Test
  public void testTableWithStats() throws Exception {
    ZonedDateTime currentTime = currentUtcTime();

    String tableName = "table_with_stats";
    BaseTableMetadata table = BaseTableMetadata.builder()
      .tableInfo(TableInfo.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .name(tableName)
        .type("PARQUET")
        .build())
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.TABLE)
        .key(MetadataInfo.GENERAL_INFO_KEY)
        .build())
      .location(new Path("/tmp", tableName))
      .metadataStatistics(Collections.singletonList(
        new StatisticsHolder<>(100L, TableStatisticsKind.ROW_COUNT)))
      .columnsStatistics(Collections.emptyMap())
      .partitionKeys(Collections.emptyMap())
      .lastModifiedTime(currentTime.toInstant().toEpochMilli())
      .build();

    metastore.tables().modify()
      .overwrite(table.toMetadataUnit())
      .execute();

    client.testBuilder()
      .sqlQuery("select %s from information_schema.`tables` where table_name = '%s'",
        String.join(", ", TABLES_COLUMNS), tableName)
      .unOrdered()
      .baselineColumns(TABLES_COLUMNS.toArray(new String[0]))
      .baselineValues("DRILL", "dfs.tmp", tableName, "TABLE", table.getTableInfo().type(),
        table.getLocation().toUri().toString(), 100L, currentTime.toLocalDateTime())
      .go();
  }

  @Test
  public void testColumns() throws Exception {
    BaseTableMetadata tableNoSchema = BaseTableMetadata.builder()
      .tableInfo(TableInfo.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .name("table_no_schema")
        .type("PARQUET")
        .build())
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.TABLE)
        .key(MetadataInfo.GENERAL_INFO_KEY)
        .build())
      .location(new Path("/tmp", "table_no_schema"))
      .metadataStatistics(Collections.emptyList())
      .columnsStatistics(Collections.emptyMap())
      .partitionKeys(Collections.emptyMap())
      .build();

    TupleMetadata schema = new SchemaBuilder()
      .addNullable("bigint_col", TypeProtos.MinorType.BIGINT)
      .addDecimal("decimal_col", TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 10, 2)
      .add("interval_col", TypeProtos.MinorType.INTERVALYEAR)
      .addArray("array_col", TypeProtos.MinorType.BIT)
      .addMap("struct_col")
        .addNullable("struct_bigint", TypeProtos.MinorType.BIGINT)
        .add("struct_varchar", TypeProtos.MinorType.VARCHAR)
        .addMap("nested_struct")
          .addNullable("nested_struct_boolean", TypeProtos.MinorType.BIT)
          .add("nested_struct_varchar", TypeProtos.MinorType.VARCHAR)
          .resumeMap()
        .resumeSchema()
      .buildSchema();

    PrimitiveColumnMetadata varcharCol = new PrimitiveColumnMetadata("varchar_col",
      TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.VARCHAR)
        .setMode(TypeProtos.DataMode.REQUIRED)
        .build());
    varcharCol.setDefaultValue("ABC");

    PrimitiveColumnMetadata timestampColumn = new PrimitiveColumnMetadata("timestamp_col",
      TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.TIMESTAMP)
        .setMode(TypeProtos.DataMode.REQUIRED)
        .build());
    timestampColumn.setFormat("yyyy-MM-dd HH:mm:ss");

    schema.addColumn(varcharCol);
    schema.addColumn(timestampColumn);

    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = new HashMap<>();
    columnsStatistics.put(SchemaPath.parseFromString("varchar_col"),
      new ColumnStatistics<>(Arrays.asList(
        new StatisticsHolder<>("aaa", ColumnStatisticsKind.MIN_VALUE),
        new StatisticsHolder<>("zzz", ColumnStatisticsKind.MAX_VALUE))));
    columnsStatistics.put(SchemaPath.parseFromString("struct_col.nested_struct.nested_struct_varchar"),
      new ColumnStatistics<>(Arrays.asList(
        new StatisticsHolder<>("bbb", ColumnStatisticsKind.MIN_VALUE),
        new StatisticsHolder<>("ccc", ColumnStatisticsKind.MAX_VALUE))));
    columnsStatistics.put(SchemaPath.parseFromString("bigint_col"),
      new ColumnStatistics<>(Arrays.asList(
        new StatisticsHolder<>(100L, ColumnStatisticsKind.NULLS_COUNT),
        new StatisticsHolder<>(10.5D, ColumnStatisticsKind.NDV))));
    columnsStatistics.put(SchemaPath.parseFromString("struct_col.struct_bigint"),
      new ColumnStatistics<>(Collections.singletonList(
        new StatisticsHolder<>(10.5D, ColumnStatisticsKind.NON_NULL_COUNT))));

    ZonedDateTime currentTime = currentUtcTime();

    String tableName = "table_with_schema";
    BaseTableMetadata tableWithSchema = BaseTableMetadata.builder()
      .tableInfo(TableInfo.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .name(tableName)
        .type("PARQUET")
        .build())
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.TABLE)
        .key(MetadataInfo.GENERAL_INFO_KEY)
        .build())
      .location(new Path("/tmp", tableName))
      .schema(schema)
      .metadataStatistics(Collections.emptyList())
      .columnsStatistics(columnsStatistics)
      .partitionKeys(Collections.emptyMap())
      .lastModifiedTime(currentTime.toInstant().toEpochMilli())
      .build();

    metastore.tables().modify()
      .overwrite(tableNoSchema.toMetadataUnit(), tableWithSchema.toMetadataUnit())
      .execute();

    List<String> columns = Arrays.asList(
      InfoSchemaConstants.SHRD_COL_TABLE_CATALOG,
      InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA,
      InfoSchemaConstants.SHRD_COL_TABLE_NAME,
      InfoSchemaConstants.COLS_COL_COLUMN_NAME,
      InfoSchemaConstants.COLS_COL_ORDINAL_POSITION,
      InfoSchemaConstants.COLS_COL_COLUMN_DEFAULT,
      InfoSchemaConstants.COLS_COL_IS_NULLABLE,
      InfoSchemaConstants.COLS_COL_DATA_TYPE,
      InfoSchemaConstants.COLS_COL_CHARACTER_MAXIMUM_LENGTH,
      InfoSchemaConstants.COLS_COL_CHARACTER_OCTET_LENGTH,
      InfoSchemaConstants.COLS_COL_NUMERIC_PRECISION,
      InfoSchemaConstants.COLS_COL_NUMERIC_PRECISION_RADIX,
      InfoSchemaConstants.COLS_COL_NUMERIC_SCALE,
      InfoSchemaConstants.COLS_COL_DATETIME_PRECISION,
      InfoSchemaConstants.COLS_COL_INTERVAL_TYPE,
      InfoSchemaConstants.COLS_COL_INTERVAL_PRECISION,
      InfoSchemaConstants.COLS_COL_COLUMN_SIZE,
      InfoSchemaConstants.COLS_COL_COLUMN_FORMAT,
      InfoSchemaConstants.COLS_COL_NUM_NULLS,
      InfoSchemaConstants.COLS_COL_MIN_VAL,
      InfoSchemaConstants.COLS_COL_MAX_VAL,
      InfoSchemaConstants.COLS_COL_NDV,
      InfoSchemaConstants.COLS_COL_EST_NUM_NON_NULLS,
      InfoSchemaConstants.COLS_COL_IS_NESTED);

    client.testBuilder()
      .sqlQuery("select %s from information_schema.`columns` where table_name " +
        "in ('%s', '%s')", String.join(", ", columns), tableNoSchema.getTableInfo().name(), tableName)
      .unOrdered()
      .baselineColumns(columns.toArray(new String[0]))
      .baselineValues("DRILL", "dfs.tmp", tableName, "bigint_col", 1, null, "YES", "BIGINT", null, null,
        0, 2, 0, null, null, null, 20, null, 100L, null, null, 10.5D, null, false)
      .baselineValues("DRILL", "dfs.tmp", tableName, "decimal_col", 2, null, "YES", "DECIMAL", null, null,
        10, 10, 2, null, null, null, 12, null, null, null, null, null, null, false)
      .baselineValues("DRILL", "dfs.tmp", tableName, "interval_col", 3, null, "NO", "INTERVAL", null, null,
        null, null, null, null, "INTERVAL YEAR TO MONTH", 0, 9, null, null, null, null, null, null, false)
      .baselineValues("DRILL", "dfs.tmp", tableName, "array_col", 4, null, "NO", "ARRAY", null, null,
        null, null, null, null, null, null, 0, null, null, null, null, null, null, false)
      .baselineValues("DRILL", "dfs.tmp", tableName, "struct_col", 5, null, "NO", "STRUCT", null, null,
        null, null, null, null, null, null, 0, null, null, null, null, null, null, false)
      .baselineValues("DRILL", "dfs.tmp", tableName, "struct_col.struct_bigint", 5, null, "YES", "BIGINT", null, null,
        0, 2, 0, null, null, null, 20, null, null, null, null, null, 10.5D, true)
      .baselineValues("DRILL", "dfs.tmp", tableName, "struct_col.struct_varchar", 5, null, "NO", "CHARACTER VARYING", 65535, 65535,
        null, null, null, null, null, null, 65535, null, null, null, null, null, null, true)
      .baselineValues("DRILL", "dfs.tmp", tableName, "struct_col.nested_struct", 5, null, "NO", "STRUCT", null, null,
        null, null, null, null, null, null, 0, null, null, null, null, null, null, true)
      .baselineValues("DRILL", "dfs.tmp", tableName, "struct_col.nested_struct.nested_struct_boolean", 5, null, "YES", "BOOLEAN", null, null,
        null, null, null, null, null, null, 1, null, null, null, null, null, null, true)
      .baselineValues("DRILL", "dfs.tmp", tableName, "struct_col.nested_struct.nested_struct_varchar", 5, null, "NO", "CHARACTER VARYING", 65535, 65535,
        null, null, null, null, null, null, 65535, null, null, "bbb", "ccc", null, null, true)
      .baselineValues("DRILL", "dfs.tmp", tableName, "varchar_col", 6, "ABC", "NO", "CHARACTER VARYING", 65535, 65535,
        null, null, null, null, null, null, 65535, null, null, "aaa", "zzz", null, null, false)
      .baselineValues("DRILL", "dfs.tmp", tableName, "timestamp_col", 7, null, "NO", "TIMESTAMP", null, null,
        null, null, null, 19, null, null, 19, "yyyy-MM-dd HH:mm:ss", null, null, null, null, null, false)
      .go();
  }

  @Test
  public void testPartitions() throws Exception {
    String tableName = "table_with_partitions";
    ZonedDateTime currentTime = currentUtcTime();

    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name(tableName)
      .type("PARQUET")
      .build();

    SegmentMetadata defaultSegment = SegmentMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.SEGMENT)
        .key(MetadataInfo.DEFAULT_SEGMENT_KEY)
        .build())
      .path(new Path("/tmp", tableName))
      .locations(Collections.emptySet())
      .metadataStatistics(Collections.emptyList())
      .columnsStatistics(Collections.emptyMap())
      .lastModifiedTime(currentTime.toInstant().toEpochMilli())
      .build();

    SegmentMetadata segment = SegmentMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.SEGMENT)
        .key("part_int=3")
        .identifier("part_int=3")
        .build())
      .column(SchemaPath.parseFromString("dir0"))
      .partitionValues(Collections.singletonList("part_int=3"))
      .path(new Path(String.format("/tmp/%s/part_int=3", tableName)))
      .locations(Collections.emptySet())
      .metadataStatistics(Collections.emptyList())
      .columnsStatistics(Collections.emptyMap())
      .lastModifiedTime(currentTime.toInstant().toEpochMilli())
      .build();

    PartitionMetadata partition = PartitionMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(MetadataInfo.builder()
        .type(MetadataType.PARTITION)
        .key("part_int=3")
        .identifier("part_int=3/part_varchar=g")
        .build())
      .column(SchemaPath.parseFromString("part_varchar"))
      .partitionValues(Collections.singletonList("g"))
      .locations(Collections.emptySet())
      .metadataStatistics(Collections.emptyList())
      .columnsStatistics(Collections.emptyMap())
      .lastModifiedTime(currentTime.toInstant().toEpochMilli())
      .build();

    metastore.tables().modify()
      .overwrite(defaultSegment.toMetadataUnit(), segment.toMetadataUnit(), partition.toMetadataUnit())
      .execute();

    List<String> columns = Arrays.asList(
      InfoSchemaConstants.SHRD_COL_TABLE_CATALOG,
      InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA,
      InfoSchemaConstants.SHRD_COL_TABLE_NAME,
      InfoSchemaConstants.PARTITIONS_COL_METADATA_KEY,
      InfoSchemaConstants.PARTITIONS_COL_METADATA_TYPE,
      InfoSchemaConstants.PARTITIONS_COL_METADATA_IDENTIFIER,
      InfoSchemaConstants.PARTITIONS_COL_PARTITION_COLUMN,
      InfoSchemaConstants.PARTITIONS_COL_PARTITION_VALUE,
      InfoSchemaConstants.PARTITIONS_COL_LOCATION,
      InfoSchemaConstants.PARTITIONS_COL_LAST_MODIFIED_TIME);

    client.testBuilder()
      .sqlQuery("select %s from information_schema.`partitions` where table_name = '%s'",
        String.join(", ", columns), tableName)
      .unOrdered()
      .baselineColumns(columns.toArray(new String[0]))
      .baselineValues("DRILL", "dfs.tmp", tableName, "part_int=3", MetadataType.SEGMENT.name(),
        "part_int=3", "`dir0`", "part_int=3", "/tmp/table_with_partitions/part_int=3", currentTime.toLocalDateTime())
      .baselineValues("DRILL", "dfs.tmp", tableName, "part_int=3", MetadataType.PARTITION.name(),
        "part_int=3/part_varchar=g", "`part_varchar`", "g", null, currentTime.toLocalDateTime())
      .go();
  }

  private ZonedDateTime currentUtcTime() {
    // Java 9 and later returns LocalDateTime with nanoseconds precision,
    // but Java 8 returns LocalDateTime with milliseconds precision
    // and metastore stores last modified time in milliseconds
    ZonedDateTime currentTime = ZonedDateTime.of(LocalDateTime.now().withNano(0), ZoneId.systemDefault());
    return currentTime.withZoneSameInstant(ZoneId.of("UTC"));
  }
}
