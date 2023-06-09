package com.netease.arctic.spark.hive;

import com.netease.arctic.spark.SparkTestBase;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.netease.arctic.spark.SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES;

public class TestCreateTableDDL extends SparkTestBase {
  private final String database = "db_hive";
  private final String tableA = "testA";
  private final String tableB = "testB";


  @Before
  public void prepare() {
    sql("use " + catalogNameHive);
    sql("create database if not exists " + database);
  }

  @After
  public void clean() {
    sql("drop database if exists " + database);
  }


  @Test
  public void testCreateKeyedTableWithPartitioned() throws TException {
    // hive style
    TableIdentifier identifierA = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by (ts string, dt string) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);
    assertTableExist(identifierA);
    ArcticTable keyedTableA = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, keyedTableA.schema().asStruct());
    sql("desc table {0}.{1}", database, tableA);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTableA.asKeyedTable().primaryKeySpec().fieldNames()});
    Assert.assertTrue(keyedTableA.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTableA.properties().get("props.test1"));
    Assert.assertTrue(keyedTableA.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTableA.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);

    // column reference style
    TableIdentifier identifierB = TableIdentifier.of(catalogNameHive, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string ,\n " +
        " dt string ,\n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable keyedTableB = loadTable(identifierB);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, keyedTableB.schema().asStruct());

    sql("desc table {0}.{1}", database, tableB);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTableB.asKeyedTable().primaryKeySpec().fieldNames()});

    Assert.assertTrue(keyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTableB.properties().get("props.test1"));
    Assert.assertTrue(keyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTableB.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifierB);
  }

  @Test
  public void testCreateKeyedTableUnPartitioned() throws TException {
    TableIdentifier identifierA = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp , \n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);

    assertTableExist(identifierA);
    ArcticTable keyedTable = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withoutZone())
        );
    Assert.assertEquals("Schema should match expected",
        expectedSchema, keyedTable.schema().asStruct());

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTable.asKeyedTable().primaryKeySpec().fieldNames()});
    Assert.assertTrue(keyedTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTable.properties().get("props.test1"));
    Assert.assertTrue(keyedTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTable.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);
  }


  @Test
  public void testCreateUnKeyedTableWithPartitioned() throws TException {
    // hive style
    TableIdentifier identifierA = TableIdentifier.of(catalogNameHive, database, tableA);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string \n " +
        ") using arctic \n" +
        " partitioned by (ts string, dt string) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);

    assertTableExist(identifierA);
    ArcticTable unKeyedTable = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, unKeyedTable.schema().asStruct());

    Assert.assertTrue(unKeyedTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", unKeyedTable.properties().get("props.test1"));
    Assert.assertTrue(unKeyedTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", unKeyedTable.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);

    // column reference style
    TableIdentifier identifierB = TableIdentifier.of(catalogNameHive, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string, \n " +
        " dt string \n " +
        ") using arctic \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable unKeyedTableB = loadTable(identifierB);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, unKeyedTableB.schema().asStruct());

    sql("desc table {0}.{1}", database, tableB);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", unKeyedTableB.properties().get("props.test1"));
    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", unKeyedTableB.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifierB);
  }

  @Test
  public void testCreateUnKeyedTableUnPartitioned() throws TException {
    TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string \n " +
        ") using arctic \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable unKeyedTableB = loadTable(identifier);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, unKeyedTableB.schema().asStruct());

    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", unKeyedTableB.properties().get("props.test1"));
    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", unKeyedTableB.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateHiveTableUnPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string \n " +
        ") STORED AS parquet ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }


  @Test
  public void testCreateHiveTableWithPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string \n" +
        ") partitioned by (ts string, dt string) " +
        "STORED AS parquet ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    sql("desc {0}.{1}", database, tableB);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }

  @Test
  public void testCreateSourceTableWithPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string, \n " +
        " dt string \n " +
        ") using parquet \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    sql("desc {0}.{1}", database, tableB);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }


  @Test
  public void testCreateSourceTableUnPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string \n " +
        ") using parquet \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }

  @Test
  public void testCreateKeyedTableLike() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( ts ) \n" , database, tableB);

    sql("create table {0}.{1} like {2}.{3} " +
        " using arctic" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA, database, tableB);
    sql("desc table {0}.{1}", database, tableA);
    assertDescResult(rows, Lists.newArrayList("id"));

    sql("desc table extended {0}.{1}", database, tableA);
    assertDescResult(rows, Lists.newArrayList("id"));

    ArcticTable loadTable = loadTable(identifier);
    Assert.assertTrue(loadTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", loadTable.properties().get("props.test1"));
    Assert.assertTrue(loadTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", loadTable.properties().get("props.test2"));

    sql("drop table {0}.{1}", database, tableA);

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateTableLikeUsingHiveSource() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp \n" +
        ") STORED AS parquet \n" +
        " partitioned by ( ts )", database, tableB);

    sql("create table {0}.{1} like {2}.{3} using arctic " +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA, database, tableB);

    assertTableExist(identifier);
    ArcticTable loadTable = loadTable(identifier);
    Assert.assertTrue(loadTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", loadTable.properties().get("props.test1"));
    Assert.assertTrue(loadTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", loadTable.properties().get("props.test2"));

    sql("drop table {0}.{1}", database, tableA);

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateTableLikeUsingIcebergSource() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp \n" +
        ") using iceberg \n" +
        " partitioned by ( ts )", database, tableB);

    sql("create table {0}.{1} like {2}.{3} using arctic " +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA, database, tableB);

    assertTableExist(identifier);
    ArcticTable loadTable = loadTable(identifier);
    Assert.assertTrue(loadTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", loadTable.properties().get("props.test1"));
    Assert.assertTrue(loadTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", loadTable.properties().get("props.test2"));

    sql("drop table {0}.{1}", database, tableA);

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateUnKeyedTableLike() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp " +
        ") using arctic \n" +
        " partitioned by ( ts ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    sql("create table {0}.{1} like {2}.{3} using arctic", database, tableA, database, tableB);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withoutZone()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(identifier).schema().asStruct());

    sql("drop table {0}.{1}", database, tableA);

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateNewTableShouldHaveTimestampWithoutZone() {
    withSQLConf(ImmutableMap.of(
            USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES, "true"), () -> {
      TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, tableA);

      sql("create table {0}.{1} ( \n" +
              " id int , \n" +
              " name string , \n " +
              " ts timestamp , \n" +
              " primary key (id) \n" +
              ") using arctic \n" +
              " partitioned by ( ts ) \n" +
              " tblproperties ( \n" +
              " ''props.test1'' = ''val1'', \n" +
              " ''props.test2'' = ''val2'' ) ", database, tableA);
      Types.StructType expectedSchema = Types.StructType.of(
              Types.NestedField.required(1, "id", Types.IntegerType.get()),
              Types.NestedField.optional(2, "name", Types.StringType.get()),
              Types.NestedField.optional(3, "ts", Types.TimestampType.withoutZone()));
      Assert.assertEquals("Schema should match expected",
              expectedSchema, loadTable(identifier).schema().asStruct());
      List<Object[]> values = ImmutableList.of(
              row(1L, toTimestamp("2021-01-01T00:00:00.0"), toTimestamp("2021-02-01T00:00:00.0")),
              row(2L, toTimestamp("2021-01-01T00:00:00.0"), toTimestamp("2021-02-01T00:00:00.0")),
              row(3L, toTimestamp("2021-01-01T00:00:00.0"), toTimestamp("2021-02-01T00:00:00.0"))
      );
      sql("INSERT INTO {0}.{1} VALUES {2}", database, tableA, rowToSqlValues(values));

      rows = sql("SELECT * FROM {0}.{1} ORDER BY id", database, tableA);
      Assert.assertEquals(3, rows.size());
      sql("drop table {0}.{1}", database, tableA);
    });
  }

  @Test
  public void testCreateUppercaseTable() throws TException {
    // hive style
    TableIdentifier identifierA = TableIdentifier.of(catalogNameHive, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " ID int , \n" +
        " NAME string , \n " +
        " primary key (ID) \n" +
        ") using arctic \n" +
        " partitioned by (TS string, DT string) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'', \n" +
        " ''table.event-time-field'' = ''NAME'') ", database, tableA);
    assertTableExist(identifierA);
    ArcticTable keyedTableA = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, keyedTableA.schema().asStruct());
    sql("desc table {0}.{1}", database, tableA);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTableA.asKeyedTable().primaryKeySpec().fieldNames()});
    Assert.assertTrue(keyedTableA.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTableA.properties().get("props.test1"));
    Assert.assertTrue(keyedTableA.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTableA.properties().get("props.test2"));
    Assert.assertTrue(keyedTableA.properties().containsKey("table.event-time-field"));
    Assert.assertEquals("name", keyedTableA.properties().get("table.event-time-field"));

    sql("use spark_catalog");
    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);

    // column reference style
    TableIdentifier identifierB = TableIdentifier.of(catalogNameHive, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string ,\n " +
        " dt string ,\n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable keyedTableB = loadTable(identifierB);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, keyedTableB.schema().asStruct());

    sql("desc table {0}.{1}", database, tableB);
    assertPartitionResult(rows, Lists.newArrayList("ts", "dt"));

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTableB.asKeyedTable().primaryKeySpec().fieldNames()});

    Assert.assertTrue(keyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTableB.properties().get("props.test1"));
    Assert.assertTrue(keyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTableB.properties().get("props.test2"));

    sql("use spark_catalog");
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifierB);
  }

  private String rowToSqlValues(List<Object[]> rows) {
    List<String> rowValues = rows.stream().map(row -> {
      List<String> columns = Arrays.stream(row).map(value -> {
        if (value instanceof Long) {
          return value.toString();
        } else if (value instanceof Timestamp) {
          return String.format("timestamp '%s'", value);
        }
        throw new RuntimeException("Type is not supported");
      }).collect(Collectors.toList());
      return "(" + Joiner.on(",").join(columns) + ")";
    }).collect(Collectors.toList());
    return Joiner.on(",").join(rowValues);
  }

  protected Object[] row(Object... values) {
    return values;
  }

  private Timestamp toTimestamp(String value) {
    return new Timestamp(DateTime.parse(value).getMillis());
  }
}
