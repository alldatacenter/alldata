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

package com.netease.arctic.trino.arctic;

import com.google.common.collect.ImmutableMap;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import io.trino.testing.QueryRunner;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_CATALOG_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class TestBaseArcticPrimaryTable extends TableTestBaseWithInitDataForTrino {

  public static final String PK_TABLE_FULL_NAME = "arctic.test_db.test_pk_table";

  @Override
  protected QueryRunner createQueryRunner() throws Exception {
    AMS = MockArcticMetastoreServer.getInstance();
    // setupAMS();
    tmp.create();
    setupTables();
    initData();
    return ArcticQueryRunner.builder()
        .setIcebergProperties(ImmutableMap.of(
            "arctic.url",
            String.format("thrift://localhost:%s/%s", AMS.port(), TEST_CATALOG_NAME),
            "arctic.enable-split-task-by-delete-ratio",
            "true"
        ))
        .build();
  }

  @Test
  public void testStats() {
    assertThat(query("SHOW STATS FOR " + PK_TABLE_FULL_NAME))
        .skippingTypesCheck()
        .matches("VALUES " +
            "('id', NULL, NULL, 0e0, NULL, '1', '6'), " +
            "('name$name', 4805e-1, NULL, 0e0, NULL, NULL, NULL), " +
            "('op_time', NULL, NULL, 0e0, NULL, '2022-01-01 12:00:00.000000', '2022-01-04 12:00:00.000000'), " +
            "(NULL, NULL, NULL, NULL, 7e0, NULL, NULL)");
  }

  @Test
  public void tableMOR() throws InterruptedException {
    assertQuery("select id from " + PK_TABLE_FULL_NAME, "VALUES 1, 2, 3, 6");
  }

  @Test
  public void tableMORWithProject() throws InterruptedException {
    assertQuery("select op_time, \"name$name\" from " + PK_TABLE_FULL_NAME,
        "VALUES (TIMESTAMP '2022-01-01 12:00:00', 'john'), " +
            "(TIMESTAMP'2022-01-02 12:00:00', 'lily'), " +
            "(TIMESTAMP'2022-01-03 12:00:00', 'jake'), " +
            "(TIMESTAMP'2022-01-01 12:00:00', 'mack')");
  }

  @Test
  public void baseQuery() {
    assertQuery("select id from " + "arctic.test_db.\"test_pk_table#base\"", "VALUES 1, 2, 3");
  }

  @Test
  public void baseQueryWhenTableNameContainCatalogAndDataBase() {
    assertQuery("select id from " + "arctic.test_db.\"arctic.test_db.test_pk_table#base\"", "VALUES 1, 2, 3");
  }

  @Test
  public void baseQueryWhenTableNameContainDataBase() {
    assertQuery("select id from " + "arctic.test_db.\"test_db.test_pk_table#base\"", "VALUES 1, 2, 3");
  }

  @Test
  public void changeQuery() {
    assertQuery("select * from " + "arctic.test_db.\"test_pk_table#change\"",
        "VALUES (6,'mack',TIMESTAMP '2022-01-01 12:00:00.000000' ,3,1,'INSERT')," +
            "(5,'mary',TIMESTAMP '2022-01-01 12:00:00.000000',2,1,'INSERT')," +
            "(5,'mary',TIMESTAMP '2022-01-01 12:00:00.000000',4,1,'DELETE')");
  }

  @Test
  public void changeQueryWhenTableNameContainCatalogAndDataBase() {
    assertQuery("select * from " + "arctic.test_db.\"arctic.test_db.test_pk_table#change\"",
        "VALUES (6,'mack',TIMESTAMP '2022-01-01 12:00:00.000000' ,3,1,'INSERT')," +
            "(5,'mary',TIMESTAMP '2022-01-01 12:00:00.000000',2,1,'INSERT')," +
            "(5,'mary',TIMESTAMP '2022-01-01 12:00:00.000000',4,1,'DELETE')");
  }

  @Test
  public void changeQueryWhenTableNameContainDataBase() {
    assertQuery("select * from " + "arctic.test_db.\"test_db.test_pk_table#change\"",
        "VALUES (6,'mack',TIMESTAMP '2022-01-01 12:00:00.000000' ,3,1,'INSERT')," +
            "(5,'mary',TIMESTAMP '2022-01-01 12:00:00.000000',2,1,'INSERT')," +
            "(5,'mary',TIMESTAMP '2022-01-01 12:00:00.000000',4,1,'DELETE')");
  }

  @AfterClass(alwaysRun = true)
  public void clear() {
    clearTable();
  }
}
