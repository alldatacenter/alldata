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

package com.netease.arctic.server.dashboard.model;

/**
 * sql shortcut
 */
public enum SqlExample {
  CREATE_TABLE("CreateTable",
      "create table db_name.table_name (\n" +
          "    id int,\n" +
          "    name string, \n" +
          "    ts timestamp,\n" +
          "    primary key (id)\n" +
          ") using arctic \n" +
          "partitioned by (days(ts)) \n" +
          "tblproperties ('table.props' = 'val');"),
  DELETE_TABLE("DeleteTable",
      "drop table db_name.table_name;"),
  EDIT_TABLE("EditTable",
      "alter table db_name.table_name add column data int ;\n" +
          "alter table db_name.table_name alter column data bigint ;\n" +
          "alter table db_name.table_name drop column data;"),
  ALTER_PROPERTIES("AlterProperties",
      "alter table db_name.table_name set tblproperties (\n" +
          "    'comment' = 'A table comment.');"),
  SHOW_DATABASES("ShowDatabases",
      "show databases;"),
  SHOW_TABLES("ShowTables",
          "show tables;"),
  DESCRIBE("Describe",
      "desc db_name.table_name;");

  private String name;
  private String sql;

  SqlExample(String name, String sql) {
    this.name = name;
    this.sql = sql;
  }

  public String getName() {
    return name;
  }

  public String getSql() {
    return sql;
  }
}
