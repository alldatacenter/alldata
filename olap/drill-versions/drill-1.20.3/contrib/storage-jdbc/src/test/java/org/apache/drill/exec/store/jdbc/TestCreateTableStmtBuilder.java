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

package org.apache.drill.exec.store.jdbc;

import org.apache.drill.exec.store.jdbc.utils.CreateTableStmtBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCreateTableStmtBuilder {

  @Test
  public void testSimpleTable() {
    String table = "table";
    String schema = "schema";
    String catalog = "catalog";

    String completeTable = CreateTableStmtBuilder.buildCompleteTableName(table, catalog, schema);
    assertEquals("`catalog`.`schema`.`table`", completeTable);
    assertEquals("`catalog`.`table`", CreateTableStmtBuilder.buildCompleteTableName(table, catalog, ""));
    assertEquals("`catalog`.`table`", CreateTableStmtBuilder.buildCompleteTableName(table, catalog, null));
  }

  @Test
  public void testTablesWithSpaces() {
    String table = "table with spaces";
    String schema = "schema with spaces";
    String catalog = "catalog with spaces";

    String completeTable = CreateTableStmtBuilder.buildCompleteTableName(table, catalog, schema);
    assertEquals("`catalog with spaces`.`schema with spaces`.`table with spaces`", completeTable);
    assertEquals("`catalog with spaces`.`table with spaces`", CreateTableStmtBuilder.buildCompleteTableName(table, catalog, ""));
    assertEquals("`catalog with spaces`.`table with spaces`", CreateTableStmtBuilder.buildCompleteTableName(table, catalog, null));
  }
}
