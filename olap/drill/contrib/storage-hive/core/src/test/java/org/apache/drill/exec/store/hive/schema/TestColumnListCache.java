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
package org.apache.drill.exec.store.hive.schema;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.store.hive.ColumnListsCache;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Category({SlowTest.class})
public class TestColumnListCache extends BaseTest {

  @Test
  public void testTableColumnsIndex() {
    ColumnListsCache cache = new ColumnListsCache();
    List<FieldSchema> columns = Lists.newArrayList();
    columns.add(new FieldSchema("f1", "int", null));
    columns.add(new FieldSchema("f2", "int", null));
    assertEquals(0, cache.addOrGet(columns));
  }

  @Test
  public void testPartitionColumnsIndex() {
    ColumnListsCache cache = new ColumnListsCache();
    List<FieldSchema> columns = Lists.newArrayList();
    columns.add(new FieldSchema("f1", "int", null));
    columns.add(new FieldSchema("f2", "int", null));
    cache.addOrGet(columns);
    columns.add(new FieldSchema("f3", "int", null));
    assertEquals(1, cache.addOrGet(columns));
  }

  @Test
  public void testColumnListUnique() {
    ColumnListsCache cache = new ColumnListsCache();
    List<FieldSchema> columns = Lists.newArrayList();
    columns.add(new FieldSchema("f1", "int", null));
    columns.add(new FieldSchema("f2", "int", null));
    cache.addOrGet(columns);
    cache.addOrGet(Lists.newArrayList(columns));
    assertEquals(0, cache.addOrGet(Lists.newArrayList(columns)));
  }

  @Test
  public void testPartitionColumnListAccess() {
    ColumnListsCache cache = new ColumnListsCache();
    List<FieldSchema> columns = Lists.newArrayList();
    columns.add(new FieldSchema("f1", "int", null));
    columns.add(new FieldSchema("f2", "int", null));
    cache.addOrGet(columns);
    cache.addOrGet(columns);
    columns.add(new FieldSchema("f3", "int", null));
    cache.addOrGet(columns);
    cache.addOrGet(columns);
    columns.add(new FieldSchema("f4", "int", null));
    cache.addOrGet(columns);
    cache.addOrGet(columns);
    assertEquals(columns, cache.getColumns(2));
  }

  @Test
  public void testPartitionColumnCaching() {
    ColumnListsCache cache = new ColumnListsCache();
    List<FieldSchema> columns = Lists.newArrayList();
    columns.add(new FieldSchema("f1", "int", null));
    columns.add(new FieldSchema("f2", "int", null));
    // sum of all indexes from cache
    int indexSum = cache.addOrGet(columns);
    indexSum += cache.addOrGet(columns);
    List<FieldSchema> sameColumns = Lists.newArrayList(columns);
    indexSum += cache.addOrGet(sameColumns);
    List<FieldSchema> otherColumns = Lists.newArrayList();
    otherColumns.add(new FieldSchema("f3", "int", null));
    otherColumns.add(new FieldSchema("f4", "int", null));
    // sum of all indexes from cache
    int secondIndexSum = cache.addOrGet(otherColumns);
    secondIndexSum += cache.addOrGet(otherColumns);
    List<FieldSchema> sameOtherColumns = Lists.newArrayList();
    sameOtherColumns.add(new FieldSchema("f3", "int", null));
    sameOtherColumns.add(new FieldSchema("f4", "int", null));
    secondIndexSum += cache.addOrGet(sameOtherColumns);
    secondIndexSum += cache.addOrGet(Lists.newArrayList(sameOtherColumns));
    secondIndexSum += cache.addOrGet(otherColumns);
    secondIndexSum += cache.addOrGet(otherColumns);
    indexSum += cache.addOrGet(sameColumns);
    indexSum += cache.addOrGet(columns);
    // added only two kinds of column lists
    assertNull(cache.getColumns(3));
    // sum of the indices of the first column list
    assertEquals(0, indexSum);
    assertEquals(6, secondIndexSum);
  }
}
