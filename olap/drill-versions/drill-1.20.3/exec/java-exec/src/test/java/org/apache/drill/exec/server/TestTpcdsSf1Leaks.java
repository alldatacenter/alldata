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
package org.apache.drill.exec.server;

import org.apache.drill.test.BaseTestQuery;

import static org.apache.drill.exec.ExecConstants.SLICE_TARGET;
import static org.apache.drill.exec.ExecConstants.SLICE_TARGET_DEFAULT;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

/**
 * To run this unit class you need to download the following data file:
 * http://apache-drill.s3.amazonaws.com/files/tpcds-sf1-parquet.tgz
 * and untar it in a some folder (e.g. /tpcds-sf1-parquet) then add the following workspace to
 * the dfs storage plugin
 *
 * ,"tpcds" : {
 *   location: "/tpcds-sf1-parquet",
 *   writable: false
 * }
 *
 */
@Ignore
public class TestTpcdsSf1Leaks extends BaseTestQuery {

  @Rule
  final public TestRule TIMEOUT = new Timeout(0); // wait forever

  @BeforeClass
  public static void initCluster() {
    updateTestCluster(3, null);
  }

  @Test
  public void test() throws Exception {
    setSessionOption(SLICE_TARGET, "10");
    try {
      final String query = getFile("tpcds-sf1/q73.sql");
      for (int i = 0; i < 20; i++) {
        try {
          runSQL(query);
        } catch (final Exception e) {
          fail("query failed: " + e.getMessage());
        }
      }
    }finally {
      setSessionOption(SLICE_TARGET, Long.toString(SLICE_TARGET_DEFAULT));
    }
  }

}
