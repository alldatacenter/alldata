package com.netease.arctic.spark.hive;

import com.netease.arctic.spark.SparkTestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestUnkeyedTableDml extends SparkTestBase {
  private final String database = "db_hive";
  private final String tableA = "testA";
  private final String tableB = "testB";
  private final String unPartitionTable = "unPartitionTable";

  protected String createTableTemplateA = "create table {0}.{1}( \n" +
      " id int, \n" +
      " name string, \n" +
      " data string) \n" +
      " using arctic partitioned by (data) ;";

  protected String createTableTemplateB = "create table {0}.{1}( \n" +
      " id int, \n" +
      " point double, \n" +
      " data string) \n" +
      " using arctic partitioned by (data) ;";

  @Before
  public void prepare() {
    sql("use " + catalogNameHive);
    sql("create database if not exists " + database);
    sql(createTableTemplateA, database, tableA);
  }

  @After
  public void cleanUpTable() {
    sql("drop table " + database + "." + tableA);
    sql("drop database " + database);
  }

  @Test
  public void testUpdate() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', '1' ) , " +
        "(2, 'bbb', '1'), " +
        "(3, 'bbb', '1'), " +
        "(4, 'bbb', '1'), " +
        "(5, 'bbb', '2'), " +
        "(6, 'bbb', '2'), " +
        "(7, 'bbb', '2'), " +
        "(8, 'ccc', '2') ");

    sql("update {0}.{1} set name = \"ddd\" where id >= 1", database, tableA);
    rows = sql("select id, name from {0}.{1} where id = 3", database, tableA);

    Assert.assertEquals(1, rows.size());
    Assert.assertEquals("ddd", rows.get(0)[1]);
    sql("select * from {0}.{1} ", database, tableA);
    Assert.assertEquals(8, rows.size());

  }

  @Test
  public void testUpdateDiffColType() {
    sql(createTableTemplateB, database, tableB);
    sql("insert into " + database + "." + tableB +
        " values (1, 1.1, 'aaa' ) , " +
        "(2,  1.2, 'bbb'), " +
        "(3, 1.1, 'ccc') ");

    sql("update {0}.{1} set point = 1.3 where id = 3", database, tableB);
    rows = sql("select id, point from {0}.{1} where id = 3", database, tableB);

    Assert.assertEquals(1, rows.size());
    Assert.assertEquals(1.3, rows.get(0)[1]);
    sql("select * from {0}.{1} ", database, tableB);
    Assert.assertEquals(3, rows.size());
    sql("drop table " + database + "." + tableB);

  }

  @Test
  public void testUpdateAnyPartitions() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', '1' ) , " +
        "(2, 'bbb', '2'), " +
        "(3, 'ccc', '1') ");

    sql("update {0}.{1} as t set name = ''ddd'' where data =  ''1'' or data =''2''", database, tableA);
    rows = sql("select * from {0}.{1} ", database, tableA);

    Assert.assertEquals(3, rows.size());
    Assert.assertEquals("ddd", rows.get(0)[1]);
  }

  @Test
  public void testUpdatePartitionField() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', '1' ) , " +
        "(2, 'bbb', '2'), " +
        "(3, 'ccc', '1') ");

    sql("update {0}.{1} set data = ''3'' where id = 1", database, tableA);
    rows = sql("select id, data from {0}.{1} where id =1", database, tableA);

    Assert.assertEquals(1, rows.size());
    Assert.assertEquals("3", rows.get(0)[1]);
  }

  @Test
  public void testUpdateUnPartitions() {
    sql( "create table {0}.{1}( \n" +
        " id int, \n" +
        " name string, \n" +
        " data string) \n" +
        " using arctic ", database, unPartitionTable);
    sql("insert into " + database + "." + unPartitionTable +
        " values (1, 'aaa', '1' ) , " +
        "(2, 'bbb', '2'), " +
        "(3, 'ccc', '1') ");

    sql("update {0}.{1} as t set name = ''ddd'' where data =  ''1'' or data =''2''", database, unPartitionTable);
    rows = sql("select * from {0}.{1} ", database, unPartitionTable);

    Assert.assertEquals(3, rows.size());
    Assert.assertEquals("ddd", rows.get(0)[1]);
    sql("drop table " + database + "." + unPartitionTable);
  }

  @Test
  public void testDelete() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', '1' ) , " +
        "(2, 'bbb', '2' ), " +
        "(3, 'ccc', '1' ) ");

    sql("delete from {0}.{1} where id = 3", database, tableA);
    rows = sql("select id, name from {0}.{1} order by id", database, tableA);

    Assert.assertEquals(2, rows.size());
    Assert.assertEquals(1, rows.get(0)[0]);
    Assert.assertEquals(2, rows.get(1)[0]);
  }

  @Test
  public void testDeleteAnyPartition() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', '1' ) , " +
        "(2, 'bbb', '2' ), " +
        "(3, 'ccc', '3' ), " +
        "(4, 'ddd', '1' ) ");

    sql("delete from {0}.{1} where data = ''1'' or data = ''2''", database, tableA);
    rows = sql("select id, name from {0}.{1} order by id", database, tableA);

    Assert.assertEquals(1, rows.size());
    Assert.assertEquals(3, rows.get(0)[0]);
  }

  @Test
  public void testInsert() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', 'abcd' ) , " +
        "(2, 'bbb', 'bbcd'), " +
        "(3, 'ccc', 'cbcd') ");

    rows = sql("select * from {0}.{1} ", database, tableA);

    Assert.assertEquals(3, rows.size());
  }

  @Test
  public void testUpdateHasNoFilter() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', 'abcd' ) , " +
        "(2, 'bbb', 'bbcd'), " +
        "(3, 'ccc', 'cbcd') ");
    sql("update {0}.{1} set name = \"ddd\"", database, tableA);
    rows = sql("select name from {0}.{1} group by name", database, tableA);

    Assert.assertEquals(1, rows.size());
    Assert.assertEquals("ddd", rows.get(0)[0]);
  }

  @Test
  public void testDeleteHasNoFilter() {
    sql("insert into " + database + "." + tableA +
        " values (1, 'aaa', 'abcd' ) , " +
        "(2, 'bbb', 'bbcd'), " +
        "(3, 'ccc', 'cbcd') ");
    sql("delete from {0}.{1}", database, tableA);
    rows = sql("select * from {0}.{1}", database, tableA);

    Assert.assertEquals(0, rows.size());
  }
}
