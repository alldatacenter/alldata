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
package org.apache.drill.exec.server.rest;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.server.rest.RestQueryRunner.QueryResult;
import org.apache.drill.test.ClusterFixture;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class TestQueryWrapperImpersonation extends RestServerTest {

  @BeforeClass
  public static void setupServer() throws Exception {
    startCluster(ClusterFixture.bareBuilder(dirTestWatcher)
      .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
      .configProperty(ExecConstants.IMPERSONATION_ENABLED, true));
  }

  @Test
  public void testImpersonation() throws Exception {
    QueryResult result = runQueryWithUsername(
        "SELECT CATALOG_NAME, SCHEMA_NAME FROM information_schema.SCHEMATA", "alfred");
    UserBitShared.QueryProfile queryProfile = getQueryProfile(result);
    assertNotNull(queryProfile);
    assertEquals("alfred", queryProfile.getUser());
  }

  @Test
  public void testImpersonationEnabledButUserNameNotProvided() throws Exception {
    QueryResult result = runQueryWithUsername(
        "SELECT CATALOG_NAME, SCHEMA_NAME FROM information_schema.SCHEMATA", null);
    UserBitShared.QueryProfile queryProfile = getQueryProfile(result);
    assertNotNull(queryProfile);
    assertEquals("anonymous", queryProfile.getUser());
  }
}
