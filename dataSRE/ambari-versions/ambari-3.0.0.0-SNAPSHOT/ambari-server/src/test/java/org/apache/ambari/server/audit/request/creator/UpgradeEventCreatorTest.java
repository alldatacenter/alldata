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

package org.apache.ambari.server.audit.request.creator;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddUpgradeRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.UpgradeEventCreator;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class UpgradeEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    UpgradeEventCreator creator = new UpgradeEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(UpgradeResourceProvider.UPGRADE_REPO_VERSION_ID, "1234");
    properties.put(UpgradeResourceProvider.UPGRADE_TYPE, "ROLLING");
    properties.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "mycluster");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.Upgrade, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Upgrade addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Repository version ID(1234), Upgrade type(ROLLING), Cluster name(mycluster)";

    Assert.assertTrue("Class mismatch", event instanceof AddUpgradeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }
}
