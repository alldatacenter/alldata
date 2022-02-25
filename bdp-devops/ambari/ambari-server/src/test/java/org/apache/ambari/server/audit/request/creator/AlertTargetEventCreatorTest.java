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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddAlertTargetRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ChangeAlertTargetRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteAlertTargetRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.AlertTargetEventCreator;
import org.apache.ambari.server.controller.internal.AlertTargetResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.notifications.dispatchers.EmailDispatcher;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService;
import org.junit.Test;

import junit.framework.Assert;

public class AlertTargetEventCreatorTest extends AuditEventCreatorTestBase {

  @Test
  public void postTest() {
    AlertTargetEventCreator creator = new AlertTargetEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION, "Target description");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_NAME, "Target name");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE, "NotifType");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES + "/" + EmailDispatcher.JAVAMAIL_FROM_PROPERTY, "email");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_STATES, Arrays.asList("S","T","A","T","E","S"));
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_GROUPS, Arrays.asList("G","R","P","S"));
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES + "/" + AlertNoticeDispatchService.AMBARI_DISPATCH_RECIPIENTS, Arrays.asList("a@a.com","b@b.com","c@c.com"));

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.AlertTarget, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Notification addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Notification name(Target name), Description(Target description), Notification type(NotifType), Group IDs(G, R, P, S), Email from(email), Email to(a@a.com, b@b.com, c@c.com), Alert states(S, T, A, T, E, S)";

    Assert.assertTrue("Class mismatch", event instanceof AddAlertTargetRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putTest() {
    AlertTargetEventCreator creator = new AlertTargetEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION, "Target description");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_NAME, "Target name");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE, "NotifType");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES + "/" + EmailDispatcher.JAVAMAIL_FROM_PROPERTY, "email");
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_STATES, Arrays.asList("S","T","A","T","E","S"));
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_GROUPS, Arrays.asList("G","R","P","S"));
    properties.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES + "/" + AlertNoticeDispatchService.AMBARI_DISPATCH_RECIPIENTS, Arrays.asList("a@a.com","b@b.com","c@c.com"));

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.AlertTarget, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Notification change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Notification name(Target name), Description(Target description), Notification type(NotifType), Group IDs(G, R, P, S), Email from(email), Email to(a@a.com, b@b.com, c@c.com), Alert states(S, T, A, T, E, S)";

    Assert.assertTrue("Class mismatch", event instanceof ChangeAlertTargetRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void deleteTest() {
    AlertTargetEventCreator creator = new AlertTargetEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.AlertTarget, "888");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.AlertTarget, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Notification removal), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Notification ID(888)";

    Assert.assertTrue("Class mismatch", event instanceof DeleteAlertTargetRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
