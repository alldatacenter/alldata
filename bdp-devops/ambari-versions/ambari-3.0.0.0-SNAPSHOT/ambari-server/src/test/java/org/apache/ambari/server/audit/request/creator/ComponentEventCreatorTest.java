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
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.StartOperationRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.ComponentEventCreator;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

public class ComponentEventCreatorTest extends AuditEventCreatorTestBase {

  @Test
  public void deleteTest() {
    ComponentEventCreator creator = new ComponentEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostComponentResourceProvider.HOST_NAME, "ambari1.example.com");

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.HostComponent, "MyComponent");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.HostComponent, properties, resource);

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Delete component MyComponent), Host name(ambari1.example.com), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putForAllHostsTest() {
    allHostsTest(Request.Type.PUT);
  }
  @Test
  public void postForAllHostsTest() {
    allHostsTest(Request.Type.POST);
  }

  private void allHostsTest(Request.Type type) {
    ComponentEventCreator creator = new ComponentEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostComponentResourceProvider.HOST_NAME, "ambari1.example.com");
    properties.put(HostComponentResourceProvider.CLUSTER_NAME, "mycluster");
    properties.put(HostComponentResourceProvider.STATE, "STARTED");

    Request request = AuditEventCreatorTestHelper.createRequest(type, Resource.Type.HostComponent, properties, null);
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_LEVEL_ID, "CLUSTER");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_CLUSTER_ID, "mycluster");
    request.getBody().setQueryString("hostname.in(a,b,c)");

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(STARTED: all services on all hosts that matches hostname.in(a,b,c) (mycluster)), Host name(ambari1.example.com), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void hostTest() {
    ComponentEventCreator creator = new ComponentEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostComponentResourceProvider.HOST_NAME, "ambari1.example.com");
    properties.put(HostComponentResourceProvider.CLUSTER_NAME, "mycluster");
    properties.put(HostComponentResourceProvider.STATE, "STARTED");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.HostComponent, properties, null);
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_LEVEL_ID, "HOST");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_HOST_NAME, "ambari1.example.com");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_CLUSTER_ID, "mycluster");
    request.getBody().addRequestInfoProperty("query", "host_component.in(MYCOMPONENT,MYCOMPONENT2)");

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(STARTED: MYCOMPONENT,MYCOMPONENT2 on ambari1.example.com (mycluster)), Host name(ambari1.example.com), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void hostComponentTest() {
    ComponentEventCreator creator = new ComponentEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostComponentResourceProvider.HOST_NAME, "ambari1.example.com");
    properties.put(HostComponentResourceProvider.CLUSTER_NAME, "mycluster");
    properties.put(HostComponentResourceProvider.STATE, "STARTED");
    properties.put(HostComponentResourceProvider.COMPONENT_NAME, "MYCOMPONENT");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.HostComponent, properties, null);
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_LEVEL_ID, "HOST_COMPONENT");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_SERVICE_ID, "MYSERVICE");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_HOST_NAME, "ambari1.example.com");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_CLUSTER_ID, "mycluster");

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(STARTED: MYCOMPONENT/MYSERVICE on ambari1.example.com (mycluster)), Host name(ambari1.example.com), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void maintenanceModeTest() {
    ComponentEventCreator creator = new ComponentEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostComponentResourceProvider.HOST_NAME, "ambari1.example.com");
    properties.put(HostComponentResourceProvider.MAINTENANCE_STATE, "ON");
    properties.put(HostComponentResourceProvider.COMPONENT_NAME, "MYCOMPONENT");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.HostComponent, properties, null);

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Turn ON Maintenance Mode for MYCOMPONENT), Host name(ambari1.example.com), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void failureTest() {
    ComponentEventCreator creator = new ComponentEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostComponentResourceProvider.HOST_NAME, "ambari1.example.com");
    properties.put(HostComponentResourceProvider.MAINTENANCE_STATE, "ON");
    properties.put(HostComponentResourceProvider.COMPONENT_NAME, "MYCOMPONENT");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.HostComponent, properties, null);

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, "Failed for testing"), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Turn ON Maintenance Mode for MYCOMPONENT), Host name(ambari1.example.com), RequestId(1), Status(Failed to queue), Reason(Failed for testing)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  private void addRequestId(TreeNode<Resource> resultTree, Long requestId) {
    Resource resource = new ResourceImpl(Resource.Type.Request);
    resource.addCategory(PropertyHelper.getPropertyCategory(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
    resource.setProperty(RequestResourceProvider.REQUEST_ID_PROPERTY_ID, requestId);
    TreeNode<Resource> requestNode = new TreeNodeImpl<>(resultTree, resource, "request");
    resultTree.addChild(requestNode);
  }

}
