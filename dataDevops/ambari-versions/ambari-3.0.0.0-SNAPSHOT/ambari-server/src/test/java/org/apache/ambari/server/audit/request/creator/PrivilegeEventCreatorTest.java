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

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.ClusterPrivilegeChangeRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.PrivilegeChangeRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.PrivilegeEventCreator;
import org.apache.ambari.server.controller.internal.PrivilegeResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class PrivilegeEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    PrivilegeEventCreator creator = new PrivilegeEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");
    properties.put(PrivilegeResourceProvider.PERMISSION_NAME, "Permission2");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_NAME, userName + "2");

    NamedPropertySet nps = new NamedPropertySet("1",properties);

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.ClusterPrivilege, null, null);
    request.getBody().addPropertySet(nps);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(User role change), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Role(Permission2), User(" + userName + "2)";

    Assert.assertTrue("Class mismatch", event instanceof PrivilegeChangeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putTest() {
    PrivilegeEventCreator creator = new PrivilegeEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");
    properties.put(PrivilegeResourceProvider.PERMISSION_NAME, "Permission1");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_NAME, userName);

    Map<String,Object> properties2 = new HashMap<>();
    properties2.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");
    properties2.put(PrivilegeResourceProvider.PERMISSION_NAME, "Permission2");
    properties2.put(PrivilegeResourceProvider.PRINCIPAL_NAME, userName + "2");

    Map<String,Object> properties3 = new HashMap<>();
    properties3.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "GROUP");
    properties3.put(PrivilegeResourceProvider.PERMISSION_NAME, "Permission1");
    properties3.put(PrivilegeResourceProvider.PRINCIPAL_NAME, "testgroup");

    NamedPropertySet nps = new NamedPropertySet("1",properties);
    NamedPropertySet nps2 = new NamedPropertySet("2",properties2);
    NamedPropertySet nps3 = new NamedPropertySet("3",properties3);

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.ClusterPrivilege, null, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    request.getBody().addPropertySet(nps);
    request.getBody().addPropertySet(nps2);
    request.getBody().addPropertySet(nps3);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Role change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Roles(\n" +
      "Permission1: \n" +
      "  Users: " + userName + "\n" +
      "  Groups: testgroup\n" +
      "Permission2: \n" +
      "  Users: " + userName + "2)";

    Assert.assertTrue("Class mismatch", event instanceof ClusterPrivilegeChangeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }


}
