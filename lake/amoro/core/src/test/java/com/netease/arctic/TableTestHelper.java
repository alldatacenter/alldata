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

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;

import java.util.List;
import java.util.Map;

public interface TableTestHelper {

  String TEST_CATALOG_NAME = "test_catalog";
  String TEST_DB_NAME = "test_db";
  String TEST_TABLE_NAME = "test_table";

  TableIdentifier TEST_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, TEST_TABLE_NAME);

  Schema tableSchema();

  PartitionSpec partitionSpec();

  PrimaryKeySpec primaryKeySpec();

  Map<String, String> tableProperties();

  Record generateTestRecord(int id, String name, long ts, String opTime);

  List<DataFile> writeChangeStore(KeyedTable keyedTable, Long txId, ChangeAction action,
      List<Record> records, boolean orderedWrite);

  List<DataFile> writeBaseStore(ArcticTable keyedTable, long txId, List<Record> records, boolean orderedWrite);

  List<Record> readKeyedTable(KeyedTable keyedTable, Expression expression, Schema projectSchema, boolean useDiskMap,
      boolean readDeletedData);

  List<Record> readChangeStore(KeyedTable keyedTable, Expression expression, Schema projectSchema, boolean useDiskMap);

  List<Record> readBaseStore(ArcticTable table, Expression expression, Schema projectSchema, boolean useDiskMap);
}
