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
import org.apache.ambari.server.audit.event.request.UpdateUpgradeItemRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.UpgradeItemEventCreator;
import org.apache.ambari.server.controller.internal.UpgradeItemResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

public class UpgradeItemEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    UpgradeItemEventCreator creator = new UpgradeItemEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(UpgradeItemResourceProvider.UPGRADE_ITEM_STAGE_ID, "1");
    properties.put(UpgradeItemResourceProvider.UPGRADE_REQUEST_ID, "9");
    properties.put(PropertyHelper.getPropertyId("UpgradeItem", "status"), "Status");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.UpgradeItem, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Action confirmation by the user), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Stage id(1), Status(Status), Request id(9)";

    Assert.assertTrue("Class mismatch", event instanceof UpdateUpgradeItemRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }
}
