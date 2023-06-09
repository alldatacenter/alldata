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

package com.netease.arctic.spark;

import com.google.common.collect.Lists;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyData;
import com.netease.arctic.io.writer.TaskWriterKey;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructLikeMap;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.StructType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestOptimizeWrite extends SparkTestBase {

  private final String database = "db";
  private final String sinkTable = "sink_table";
  private final String sourceTable = "source_table";
  private final TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, sinkTable);

  private List<GenericRecord> sources;
  private Schema schema;

  @Before
  public void before() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists {0}", database);

    schema = new Schema(
        Types.NestedField.of(1, false, "id", Types.IntegerType.get()),
        Types.NestedField.of(2, false, "column1", Types.StringType.get()),
        Types.NestedField.of(3, false, "column2", Types.StringType.get())
    );

    sources = Lists.newArrayList(
        newRecord(schema,1, "aaa", "aaa"),
        newRecord(schema,2, "bbb", "aaa"),
        newRecord(schema,3, "aaa", "bbb"),
        newRecord(schema,4, "bbb", "bbb"),
        newRecord(schema,5, "aaa", "ccc"),
        newRecord(schema,6, "bbb", "ccc")
    );


    StructType structType = SparkSchemaUtil.convert(schema);
    Dataset<Row> ds = spark.createDataFrame(
        sources.stream()
            .map(TestOptimizeWrite::genericRecordToSparkRow)
            .collect(Collectors.toList()), structType);
    ds = ds.repartition(new Column("column2"));
    ds.registerTempTable(sourceTable);
  }

  @After
  public void cleanUpTable() {
    sql("drop table " + database + "." + sinkTable);
    sql("drop table " + sourceTable);
    sql("drop database " + database);
  }


  /**
   * 6 rows write to 2 partition, bucket=1, expect result 2 files.
   * each partition contain 1 file, each file contain 3 rows.
   */
  @Test
  public void testPrimaryKeyPartitionedTable() {
    sql("create table {0}.{1} ( \n" +
            " id int , \n" +
            " column1 string , \n " +
            " column2 string, \n" +
            " primary key (id) \n" +
            ") using arctic \n" +
            " partitioned by ( column1 ) \n" +
            " TBLPROPERTIES(''write.distribution-mode'' = ''hash'', " +
            "''write.distribution.hash-mode'' = ''auto''," +
            "''base.file-index.hash-bucket'' = ''1'')"
        , database, sinkTable);
    sql("insert overwrite {0}.{1} SELECT id, column1, column2 from {2}",
        database, sinkTable, sourceTable);
    rows = sql("select * from {0}.{1} order by id", database, sinkTable);
    Assert.assertEquals(6, rows.size());
    int size = expectFileSize(2);
    Assert.assertEquals(
        size,
        baseTableSize(identifier));
  }

  /**
   * 6 input rows, write to 2 partitions, bucket = 2, expect 4 files.
   * each partition contain 3 rows, split into 2 files.
   */
  @Test
  public void testKeyedPartitionedTableWithFileSplitNum() {
    sql("create table {0}.{1} ( \n" +
            " id int , \n" +
            " column1 string , \n " +
            " column2 string, \n" +
            " primary key (id) \n" +
            ") using arctic \n" +
            " partitioned by ( column1 ) \n" +
            " TBLPROPERTIES(" +
            "''base.file-index.hash-bucket'' = ''2'')"
        , database, sinkTable);
    sql("insert overwrite {0}.{1} SELECT id, column1, column2 from {2}",
        database, sinkTable, sourceTable);
    rows = sql("select * from {0}.{1} order by id", database, sinkTable);
    Assert.assertEquals(6, rows.size());
    ArcticTable table = loadTable(identifier);
    int size = expectFileSize(2);
    Assert.assertEquals(size,
        baseTableSize(identifier));
  }

  protected long baseTableSize(TableIdentifier identifier) {
    ArcticTable arcticTable = loadTable(identifier);
    UnkeyedTable base = null;
    if (arcticTable.isKeyedTable()) {
      base = arcticTable.asKeyedTable().baseTable();
    } else {
      base = arcticTable.asUnkeyedTable();
    }
    StructLikeMap<List<DataFile>> dfMap = partitionFiles(base);
    return dfMap.values().stream().map(List::size)
        .reduce(0, Integer::sum).longValue();
  }


  @Test
  public void testUnkeyedTablePartitioned() {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " column1 string , \n " +
        " column2 string \n" +
        ") using arctic \n" +
        " partitioned by ( column1 ) \n" , database, sinkTable);
    sql("insert overwrite {0}.{1} SELECT id, column1, column2 from {2}",
        database, sinkTable, sourceTable);

    rows = sql("select * from {0}.{1} order by id", database, sinkTable);
    Assert.assertEquals(6, rows.size());
    int fileCount = expectFileSize(4);
    Assert.assertEquals(fileCount,
        baseTableSize(identifier));

  }

  public int expectFileSize(int buckets) {
    ArcticTable table = loadTable(identifier);
    return expectFiles(
        sources,
        table.schema(),
        table.spec(),
        table.isKeyedTable() ? table.asKeyedTable().primaryKeySpec(): null,
        buckets
    );
  }

  public static int expectFiles(
      List<GenericRecord> sources,
      Schema schema,
      PartitionSpec partitionSpec,
      PrimaryKeySpec keySpec,
      int buckets) {
    PartitionKey partitionKey = new PartitionKey(partitionSpec, schema);
    PrimaryKeyData primaryKey = keySpec == null ? null : new PrimaryKeyData(keySpec, schema);
    int mask = buckets - 1 ;

    Set<TaskWriterKey> writerKeys = Sets.newHashSet();
    for (GenericRecord row: sources){
      partitionKey.partition(row);
      DataTreeNode node;
      if (keySpec != null) {
        primaryKey.primaryKey(row);
        node = primaryKey.treeNode(mask);
      } else {
        node = DataTreeNode.ROOT;
      }
      writerKeys.add(
          new TaskWriterKey(partitionKey.copy(), node, DataFileType.BASE_FILE)
      );
    }
    return writerKeys.size();
  }

  public static Row genericRecordToSparkRow(GenericRecord record) {
    Object[] values = new Object[record.size()];
    for (int i = 0 ; i < values.length; i++ ) {
      values[i] = record.get(i);
    }
    return RowFactory.create(values);
  }
}
