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
import org.apache.ambari.server.audit.event.request.AddUserToGroupRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.MembershipChangeRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.RemoveUserFromGroupRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.MemberEventCreator;
import org.apache.ambari.server.controller.internal.MemberResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class MemberEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    MemberEventCreator creator = new MemberEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Group, "GroupName");
    resource.put(Resource.Type.Member, "MemberName");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.Member, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(User addition to group), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Group(GroupName), Affected username(MemberName)";

    Assert.assertTrue("Class mismatch", event instanceof AddUserToGroupRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putTest() {
    MemberEventCreator creator = new MemberEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID, "GroupName");
    properties.put(MemberResourceProvider.MEMBER_USER_NAME_PROPERTY_ID, "MemberName");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Member, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Membership change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Group(GroupName), Members(MemberName)";

    Assert.assertTrue("Class mismatch", event instanceof MembershipChangeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void deleteTest() {
    MemberEventCreator creator = new MemberEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Group, "GroupName");
    resource.put(Resource.Type.Member, "MemberName");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.Member, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(User removal from group), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Group(GroupName), Affected username(MemberName)";

    Assert.assertTrue("Class mismatch", event instanceof RemoveUserFromGroupRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
