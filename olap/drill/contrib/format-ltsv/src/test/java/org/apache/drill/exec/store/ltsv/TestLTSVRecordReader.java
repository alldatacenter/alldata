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
package org.apache.drill.exec.store.ltsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLTSVRecordReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
  }

  @Test
  public void testWildcard() throws Exception {
    testBuilder()
      .sqlQuery("SELECT * FROM cp.`simple.ltsv`")
      .unOrdered()
      .baselineColumns("host", "forwardedfor", "req", "status", "size", "referer", "ua", "reqtime", "apptime", "vhost")
      .baselineValues("xxx.xxx.xxx.xxx", "-", "GET /v1/xxx HTTP/1.1", "200", "4968", "-", "Java/1.8.0_131", "2.532", "2.532", "api.example.com")
      .baselineValues("xxx.xxx.xxx.xxx", "-", "GET /v1/yyy HTTP/1.1", "200", "412", "-", "Java/1.8.0_201", "3.580", "3.580", "api.example.com")
      .go();
  }

  @Test
  public void testSelectColumns() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ua, reqtime FROM cp.`simple.ltsv`")
      .unOrdered()
      .baselineColumns("ua", "reqtime")
      .baselineValues("Java/1.8.0_131", "2.532")
      .baselineValues("Java/1.8.0_201", "3.580")
      .go();
  }

  @Test
  public void testQueryWithConditions() throws Exception {
    testBuilder()
      .sqlQuery("SELECT * FROM cp.`simple.ltsv` WHERE reqtime > 3.0")
      .unOrdered()
      .baselineColumns("host", "forwardedfor", "req", "status", "size", "referer", "ua", "reqtime", "apptime", "vhost")
      .baselineValues("xxx.xxx.xxx.xxx", "-", "GET /v1/yyy HTTP/1.1", "200", "412", "-", "Java/1.8.0_201", "3.580", "3.580", "api.example.com")
      .go();
  }

  @Test
  public void testSkipEmptyLines() throws Exception {
    assertEquals(2, queryBuilder().sql("SELECT * FROM cp.`emptylines.ltsv`").run().recordCount());
  }

  @Test
  public void testReadException() throws Exception {
    try {
      run("SELECT * FROM cp.`invalid.ltsv`");
      fail();
    } catch (UserException e) {
      assertEquals(UserBitShared.DrillPBError.ErrorType.DATA_READ, e.getErrorType());
      assertTrue(e.getMessage().contains("Failure while reading messages from /invalid.ltsv. Record reader was at record: 1"));
    }
  }
}
