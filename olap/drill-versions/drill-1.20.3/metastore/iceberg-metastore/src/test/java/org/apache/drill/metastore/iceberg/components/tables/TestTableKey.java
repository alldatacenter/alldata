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
package org.apache.drill.metastore.iceberg.components.tables;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.iceberg.IcebergBaseTest;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestTableKey extends IcebergBaseTest {

  @Test
  public void testCreation() {
    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .build();

    TableKey expected = new TableKey("dfs", "tmp", "nation");

    assertEquals(expected, TableKey.of(unit));
  }

  @Test
  public void testToLocation() {
    TableKey tableKey = new TableKey("dfs", "tmp", "nation");

    String expected = new Path(
      Paths.get("/metastore", "dfs", "tmp", "nation").toFile().getPath())
      .toUri().getPath();

    assertEquals(expected, tableKey.toLocation("/metastore"));
  }

  @Test
  public void testToFilterConditions() {
    TableKey tableKey = new TableKey("dfs", "tmp", "nation");

    Map<MetastoreColumn, Object> expected = new HashMap<>();
    expected.put(MetastoreColumn.STORAGE_PLUGIN, "dfs");
    expected.put(MetastoreColumn.WORKSPACE, "tmp");
    expected.put(MetastoreColumn.TABLE_NAME, "nation");

    assertEquals(expected, tableKey.toFilterConditions());
  }
}
