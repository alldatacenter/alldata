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
package org.apache.drill.exec.work.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.calcite.avatica.util.Quoting;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaResp;
import org.apache.drill.exec.proto.UserProtos.RequestStatus;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestServerMetaProvider extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testServerMeta() throws Exception {
    GetServerMetaResp response = client.client().getServerMeta().get();
    assertNotNull(response);
    assertEquals(RequestStatus.OK, response.getStatus());
    assertNotNull(response.getServerMeta());

    assertEquals(Quoting.BACK_TICK.string, response.getServerMeta().getIdentifierQuoteString());
  }

  @Test
  public void testCurrentSchema() throws Exception {
    GetServerMetaResp response = client.client().getServerMeta().get();
    assertEquals(RequestStatus.OK, response.getStatus());
    assertEquals("", response.getServerMeta().getCurrentSchema());

    queryBuilder().sql("use dfs.tmp").run();

    response = client.client().getServerMeta().get();
    assertEquals(RequestStatus.OK, response.getStatus());
    assertEquals("dfs.tmp", response.getServerMeta().getCurrentSchema());
  }

}
