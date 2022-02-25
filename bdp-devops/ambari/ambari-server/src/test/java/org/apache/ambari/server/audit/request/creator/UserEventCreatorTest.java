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
import org.apache.ambari.server.audit.event.request.ActivateUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.AdminUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.CreateUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.UserPasswordChangeRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.UserEventCreator;
import org.apache.ambari.server.controller.internal.UserResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class UserEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    UserEventCreator creator = new UserEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, "false");
    properties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, "true");
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "myUser");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.User, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(User creation), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Created Username(myUser), Active(yes), Administrator(no)";

    Assert.assertTrue("Class mismatch", event instanceof CreateUserRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void deleteTest() {
    UserEventCreator creator = new UserEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.User, "userToDelete");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.User, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(User delete), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Deleted Username(userToDelete)";

    Assert.assertTrue("Class mismatch", event instanceof DeleteUserRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void activeTest() {
    UserEventCreator creator = new UserEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, "true");
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "myUser");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.User, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Set user active/inactive), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Affected username(myUser), Active(yes)";

    Assert.assertTrue("Class mismatch", event instanceof ActivateUserRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void adminTest() {
    UserEventCreator creator = new UserEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, "false");
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "myUser");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.User, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Set user admin), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Affeted username(myUser), Administrator(no)";

    Assert.assertTrue("Class mismatch", event instanceof AdminUserRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void passwordChangeTest() {
    UserEventCreator creator = new UserEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "oldPassword");
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "myUser");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.User, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Password change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Affected username(myUser)";

    Assert.assertTrue("Class mismatch", event instanceof UserPasswordChangeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
