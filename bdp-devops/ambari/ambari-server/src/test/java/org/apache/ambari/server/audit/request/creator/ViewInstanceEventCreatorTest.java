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
import org.apache.ambari.server.audit.event.request.AddViewInstanceRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ChangeViewInstanceRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteViewInstanceRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.ViewInstanceEventCreator;
import org.apache.ambari.server.controller.internal.ViewInstanceResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class ViewInstanceEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    ViewInstanceEventCreator creator = new ViewInstanceEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ViewInstanceResourceProvider.VIEW_NAME, "MyView");
    properties.put(ViewInstanceResourceProvider.VERSION, "1.9");
    properties.put(ViewInstanceResourceProvider.INSTANCE_NAME, "MyViewInstance");
    properties.put(ViewInstanceResourceProvider.LABEL, "MyViewLabel");
    properties.put(ViewInstanceResourceProvider.DESCRIPTION, "Test view");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.ViewInstance, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(View addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Type(MyView), Version(1.9), Name(MyViewInstance), Display name(MyViewLabel), Description(Test view)";

    Assert.assertTrue("Class mismatch", event instanceof AddViewInstanceRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putTest() {
    ViewInstanceEventCreator creator = new ViewInstanceEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ViewInstanceResourceProvider.VIEW_NAME, "MyView");
    properties.put(ViewInstanceResourceProvider.VERSION, "1.9");
    properties.put(ViewInstanceResourceProvider.INSTANCE_NAME, "MyViewInstance");
    properties.put(ViewInstanceResourceProvider.LABEL, "MyViewLabel");
    properties.put(ViewInstanceResourceProvider.DESCRIPTION, "Test view");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.ViewInstance, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(View change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Type(MyView), Version(1.9), Name(MyViewInstance), Display name(MyViewLabel), Description(Test view)";

    Assert.assertTrue("Class mismatch", event instanceof ChangeViewInstanceRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void deleteTest() {
    ViewInstanceEventCreator creator = new ViewInstanceEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.View, "MyView");
    resource.put(Resource.Type.ViewVersion, "1.2");
    resource.put(Resource.Type.ViewInstance, "MyViewInstance");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.ViewInstance, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(View deletion), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Type(MyView), Version(1.2), Name(MyViewInstance)";

    Assert.assertTrue("Class mismatch", event instanceof DeleteViewInstanceRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
