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
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TestKeyedTableDML extends SparkTestBase {
  private final String database = "db_test";
  private final String table = "testA";
  private KeyedTable keyedTable;
  private final List<Object[]> baseFiles = Lists.newArrayList(
      new Object[]{1, "aaa", ofDateWithZone(2022, 1, 1, 0)},
      new Object[]{2, "bbb", ofDateWithZone(2022, 1, 2, 0)},
      new Object[]{3, "ccc", ofDateWithZone(2022, 1, 2, 0)});
  private List<DataFile> dataFiles;

  @Before
  public void before() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists {0}", database);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n" +
        " options ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table);
    sql("refresh table {0}.{1}", database, table);
    keyedTable = loadTable(TableIdentifier.of(catalogNameArctic, database, table)).asKeyedTable();
  }

  @After
  public void cleanUp() {
    sql("drop table {0}.{1}", database, table);
  }

  @Test
  public void testMergeOnRead() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);
    writeBase(identifier,  baseFiles);
    writeChange(identifier, ChangeAction.INSERT, Lists.newArrayList(
        newRecord(keyedTable, 4, "ddd", quickDateWithZone(4) ),
        newRecord(keyedTable, 5, "eee", quickDateWithZone(4) )
    ));
    writeChange(identifier, ChangeAction.DELETE, Lists.newArrayList(
        newRecord(keyedTable, 1, "aaa", quickDateWithZone(1))
    ));

    rows = sql("select * from {0}.{1}", database, table);
    Assert.assertEquals(4, rows.size());
    Set<Object> idSet = rows.stream().map(r -> r[0]).collect(Collectors.toSet());

    Assert.assertEquals(4, idSet.size());
    Assert.assertTrue(idSet.contains(4));
    Assert.assertTrue(idSet.contains(5));
    Assert.assertFalse(idSet.contains(1));
  }


  @Test
  public void testSelectChangeFiles() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);
    writeBase(identifier,  baseFiles);
    writeChange(identifier,  ChangeAction.INSERT, Lists.newArrayList(
        newRecord(keyedTable, 4, "ddd", quickDateWithZone(4)),
        newRecord(keyedTable, 5, "eee", quickDateWithZone(4))
    ));
    writeChange(identifier,  ChangeAction.DELETE, Lists.newArrayList(
        newRecord(keyedTable, 1, "aaa", ofDateWithZone(2022, 1, 1, 0)),
        newRecord(keyedTable, 2, "bbb", ofDateWithZone(2022, 1, 2, 0))
    ));
    rows = sql("select * from {0}.{1}.change", database, table);
    Assert.assertEquals(4, rows.size());
    // check column number
    Assert.assertEquals(6, rows.get(0).length);
  }


  @Test
  public void testSelectDeleteAll() throws IOException {
    List<DataFile> dataFiles = writeBase(TableIdentifier.of(catalogNameArctic, database, table), baseFiles);
    insertBasePosDeleteFiles(keyedTable.beginTransaction(""), dataFiles);
    rows = sql("select * from {0}.{1}", database, table);
    Assert.assertEquals(0, rows.size());
  }

  @Test
  public void testSelectDeletePart() throws IOException {
    List<DataFile> dataFiles = writeBase(TableIdentifier.of(catalogNameArctic, database, table), baseFiles);
    List<DataFile> deleteFiles = dataFiles.stream().filter(dataFile -> Objects.equals(18993,
        dataFile.partition().get(0, Object.class))).collect(Collectors.toList());
    insertBasePosDeleteFiles(keyedTable.beginTransaction(""), deleteFiles);
    rows = sql("select id from {0}.{1}", database, table);
    assertContainIdSet(rows, 0, 2, 3);
  }

  protected void insertBasePosDeleteFiles(long transactionId, List<DataFile> dataFiles) throws IOException {
    Map<StructLike, List<DataFile>> dataFilesPartitionMap =
        new HashMap<>(dataFiles.stream().collect(Collectors.groupingBy(ContentFile::partition)));
    List<DeleteFile> deleteFiles = new ArrayList<>();
    for (Map.Entry<StructLike, List<DataFile>> dataFilePartitionMap : dataFilesPartitionMap.entrySet()) {
      StructLike partition = dataFilePartitionMap.getKey();
      List<DataFile> partitionFiles = dataFilePartitionMap.getValue();
      Map<DataTreeNode, List<DataFile>> nodeFilesPartitionMap = new HashMap<>(partitionFiles.stream()
          .collect(Collectors.groupingBy(dataFile ->
              FileNameGenerator.parseFileNodeFromFileName(dataFile.path().toString()))));
      for (Map.Entry<DataTreeNode, List<DataFile>> nodeFilePartitionMap : nodeFilesPartitionMap.entrySet()) {
        DataTreeNode key = nodeFilePartitionMap.getKey();
        List<DataFile> nodeFiles = nodeFilePartitionMap.getValue();

        // write pos delete
        SortedPosDeleteWriter<Record> writer = GenericTaskWriters.builderFor(keyedTable)
            .withTransactionId(transactionId).buildBasePosDeleteWriter(key.getMask(), key.getIndex(), partition);
        for (DataFile nodeFile : nodeFiles) {
          writer.delete(nodeFile.path(), 0);
        }
        deleteFiles.addAll(writer.complete());
      }
    }
    RowDelta rowDelta = keyedTable.baseTable().newRowDelta();
    deleteFiles.forEach(rowDelta::addDeletes);
    rowDelta.commit();
  }

}
