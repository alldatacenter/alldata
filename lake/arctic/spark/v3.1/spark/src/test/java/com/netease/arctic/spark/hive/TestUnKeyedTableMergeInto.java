package com.netease.arctic.spark.hive;

import com.netease.arctic.spark.SparkTestBase;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.spark.SparkException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestUnKeyedTableMergeInto extends SparkTestBase {
  private final String database = "db_test";

  private final String tgTableA = "tgTableA";
  private final String srcTableA = "srcTableA";
  private final String hiveTable = "hiveTable";

  @Before
  public void before() {
    sql("use " + catalogNameHive);
    sql("create database if not exists {0}", database);
    sql("CREATE TABLE {0}.{1} (" +
        "id int, data string) " +
        "USING arctic", database, tgTableA);
    sql("CREATE TABLE {0}.{1} (" +
        "id int, data string) " +
        "USING arctic", database, srcTableA);
    sql("CREATE TABLE {0}.{1} (" +
        "id INT, data STRING) " +
        "STORED AS parquet", database, hiveTable) ;
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''a''), (4, ''d'')", database, tgTableA);
    sql("INSERT INTO TABLE {0}.{1} VALUES (5, ''e''), (6, ''c'')", database, tgTableA);
  }

  @After
  public void cleanUp() {
    sql("drop table {0}.{1}", database, tgTableA);
    sql("drop table {0}.{1}", database, srcTableA);
    sql("drop table {0}.{1}", database, hiveTable);
  }

  @Test
  public void testMergeWithAllCauses() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 1 THEN " +
        "  DELETE " +
        "WHEN MATCHED AND t.id = 6 THEN " +
        "  UPDATE SET * " +
        "WHEN MATCHED AND t.id = 4 THEN " +
        "  UPDATE SET * " +
        "WHEN NOT MATCHED AND s.id != 1 THEN " +
        "  INSERT *", database, tgTableA, srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(2, "e"), // new
        row(4, "g"), // update
        row(5, "e"), // kept
        row(6, "f")  // update
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }

  @Test
  public void testMergeIntoEmptyTargetInsertAllNonMatchingRows() {
    sql("CREATE TABLE {0}.{1} (" +
        "id int, data string) " +
        "USING arctic", database, "emptyTable") ;
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN NOT MATCHED THEN " +
        "  INSERT *", database, "emptyTable", srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(1, "d"), // new
        row(2, "e"), // new
        row(4, "g"), // new
        row(6, "f")  // new
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, "emptyTable"));
    sql("drop table {0}.{1}", database, "emptyTable");
  }

  @Test
  public void testMergeIntoEmptyTargetInsertOnlyMatchingRows() {
    sql("CREATE TABLE {0}.{1} (" +
        "id int, data string) " +
        "USING arctic", database, "emptyTable") ;
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN NOT MATCHED AND (s.id >=2) THEN " +
        "  INSERT *", database, "emptyTable", srcTableA);

    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(2, "e"), // new
        row(4, "g"), // new
        row(6, "f")  // new
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, "emptyTable"));
    sql("drop table {0}.{1}", database, "emptyTable");
  }

  @Test
  public void testMergeWithOnlyUpdateClause() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 1 THEN " +
        "  UPDATE SET *", database, tgTableA, srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(1, "d"), // update
        row(4, "d"), // kept
        row(5, "e"), // kept
        row(6, "c")  // kept
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }

  @Test
  public void testMergeWithOnlyDeleteClause() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 6 THEN" +
        "  DELETE", database, tgTableA, srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(1, "a"), // kept
        row(4, "d"), // kept
        row(5, "e")  // kept
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }

  @Test
  public void testMergeWithAllCausesWithExplicitColumnSpecification() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 1 THEN " +
        "  DELETE " +
        "WHEN MATCHED AND t.id = 6 THEN " +
        "  UPDATE SET t.id = s.id, t.data = s.data  " +
        "WHEN MATCHED AND t.id = 4 THEN " +
        "  UPDATE SET t.id = s.id, t.data = s.data  " +
        "WHEN NOT MATCHED AND s.id != 1 THEN " +
        "  INSERT (t.id, t.data) VALUES (s.id, s.data)", database, tgTableA, srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(2, "e"), // new
        row(4, "g"), // update
        row(5, "e"), // kept
        row(6, "f")  // update
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }

  @Test
  public void testMergeWithUnconditionalDelete() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED THEN " +
        "  DELETE " +
        "WHEN NOT MATCHED AND s.id = 2 THEN " +
        "  INSERT * ", database, tgTableA, srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(2, "e"), // new
        row(5, "e") // kept
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }

  @Test
  public void testMergeWithExtraColumnsInSource() {
    sql("CREATE TABLE {0}.{1} (" +
        "id INT, v STRING) " +
        "USING arctic", database, "target") ;
    sql("CREATE TABLE {0}.{1} (" +
        "id INT, extra_col INT, v STRING ) " +
        "USING arctic", database, "source") ;
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES " +
        "(1, ''v1''), " +
        "(2, ''v2'')", database, "target");
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES " +
        "(1, -1, ''v1_1''), " +
        "(3, -1, ''v3''), " +
        "(4, -1, ''v4'')", database, "source");
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED THEN " +
        " UPDATE SET t.v = s.v " +
        "WHEN NOT MATCHED THEN " +
        "  INSERT (t.v, t.id) VALUES (s.v, s.id)", database, "target", "source");
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(1, "v1_1"), // new
        row(2, "v2"),   // kept
        row(3, "v3"),   // new
        row(4, "v4")    // new
    );
    assertEquals("Output should match", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, "target"));
    sql("drop table {0}.{1}", database, "target");
    sql("drop table {0}.{1}", database, "source");
  }

  @Test
  public void testMergeMatchedMulitRowsForOneKey() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, srcTableA);
    sql("INSERT INTO TABLE {0}.{1} VALUES (6, ''d'')", database, srcTableA);
    Assert.assertThrows(SparkException.class,
        () -> sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
        "ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 1 THEN " +
        "  DELETE " +
        "WHEN MATCHED AND t.id = 6 THEN " +
        "  UPDATE SET t.id = s.id, t.data = s.data  " +
        "WHEN MATCHED AND t.id = 4 THEN " +
        "  UPDATE SET t.id = s.id, t.data = s.data  " +
        "WHEN NOT MATCHED AND s.id != 1 THEN " +
        "  INSERT (t.id, t.data) VALUES (s.id, s.data)", database, tgTableA, srcTableA));
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(1, "a"), // kept
        row(4, "d"), // kept
        row(5, "e"), // kept
        row(6, "c")  // kept
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }

  @Test
  public void testMergeNotMatchedMulitRowsForOneKey() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (2, ''e'')", database, srcTableA);
    sql("INSERT INTO TABLE {0}.{1} VALUES (2, ''c'')", database, srcTableA);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
            "ON t.id == s.id " +
            "WHEN MATCHED THEN " +
            "  UPDATE SET * " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (t.id, t.data) VALUES (s.id, s.data)", database, tgTableA, srcTableA);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(1, "d"), // kept
        row(2, "c"), // new
        row(2, "e"), // new
        row(4, "d"), // kept
        row(5, "e"), // kept
        row(6, "c")  // kept
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id, data", database, tgTableA));
  }

  @Test
  public void testMergeSourceTableIsNonArcticTable() {
    sql("INSERT OVERWRITE TABLE {0}.{1} VALUES (1, ''d''), (4, ''g''), (2, ''e''), (6, ''f'')", database, hiveTable);
    sql("MERGE INTO {0}.{1} AS t USING {0}.{2} AS s " +
            "ON t.id == s.id " +
            "WHEN MATCHED THEN " +
            "  DELETE " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT * ", database, tgTableA, hiveTable);
    ImmutableList<Object[]> expectedRows = ImmutableList.of(
        row(2, "e"), // new
        row(5, "e") // kept
    );
    assertEquals("Should have expected rows", expectedRows,
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, tgTableA));
  }
}
