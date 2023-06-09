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
package com.mapr.drill.maprdb.tests.json;

import java.util.List;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.mapr.drill.maprdb.tests.MaprDBTestsSuite;

public class BaseJsonTest extends BaseTestQuery {
  protected static final String SCHEMA = "hbase.root";
  protected String format(final String sql) {
    return String.format(sql, SCHEMA, getTablePath());
  }

  protected String getTablePath() {
    throw new RuntimeException("unimplemented");
  }

  public static String format(final String sql, final String tablePath) {
    return String.format(sql, SCHEMA, tablePath);
  }

  @BeforeClass
  public static void setupDefaultTestCluster() throws Exception {
    // GuavaPatcher.patch is invoked in ExecTest ExecTest-->BaseTestQuery
    // GuavaPatcher.patch();

    // Since we override the class initializer of parent class,
    // invoke it explicitly. This will setup a Drill cluster.
    BaseTestQuery.setupDefaultTestCluster();

    MaprDBTestsSuite.setupTests();
    MaprDBTestsSuite.createPluginAndGetConf(getDrillbitContext());
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    MaprDBTestsSuite.cleanupTests();
  }


  protected List<QueryDataBatch> runHBaseSQLlWithResults(String sql) throws Exception {
    return testSqlWithResults(sql);
  }

  protected void runSQLAndVerifyCount(String sql, int expectedRowCount) throws Exception{
    List<QueryDataBatch> results = runHBaseSQLlWithResults(sql);
    logResultAndVerifyRowCount(results, expectedRowCount);
  }

  private void logResultAndVerifyRowCount(List<QueryDataBatch> results, int expectedRowCount) throws SchemaChangeException {
    int rowCount = logResult(results);
    if (expectedRowCount != -1) {
      Assert.assertEquals(expectedRowCount, rowCount);
    }
  }
}
