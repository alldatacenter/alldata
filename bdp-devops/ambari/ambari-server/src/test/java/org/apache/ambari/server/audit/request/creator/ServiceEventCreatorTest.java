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
import org.apache.ambari.server.audit.event.request.DeleteServiceRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.StartOperationRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.ServiceEventCreator;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

public class ServiceEventCreatorTest extends AuditEventCreatorTestBase {

  @Test
  public void deleteTest() {
    ServiceEventCreator creator = new ServiceEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Service, "MyService");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.Service, null, resource);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Service deletion), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Service(MyService)";

    Assert.assertTrue("Class mismatch", event instanceof DeleteServiceRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void putForAllHostsTest() {
    clusterTest(Request.Type.PUT);
  }
  @Test
  public void postForAllHostsTest() {
    clusterTest(Request.Type.POST);
  }

  private void clusterTest(Request.Type type) {
    ServiceEventCreator creator = new ServiceEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "STARTED");

    Request request = AuditEventCreatorTestHelper.createRequest(type, Resource.Type.Service, properties, null);
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_LEVEL_ID, "CLUSTER");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_CLUSTER_ID, "mycluster");

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(STARTED: all services (mycluster)), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void serviceTest() {
    ServiceEventCreator creator = new ServiceEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "STARTED");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "MyService");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Service, properties, null);
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_LEVEL_ID, "SERVICE");
    request.getBody().addRequestInfoProperty(RequestOperationLevel.OPERATION_CLUSTER_ID, "mycluster");

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(STARTED: MyService (mycluster)), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void maintenanceModeTest() {
    ServiceEventCreator creator = new ServiceEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ServiceResourceProvider.SERVICE_MAINTENANCE_STATE_PROPERTY_ID, "ON");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "MyService");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Service, properties, null);

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Turn ON Maintenance Mode for MyService), RequestId(1), Status(Successfully queued)";

    Assert.assertTrue("Class mismatch", event instanceof StartOperationRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void failureTest() {
    ServiceEventCreator creator = new ServiceEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ServiceResourceProvider.SERVICE_MAINTENANCE_STATE_PROPERTY_ID, "ON");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "MyService");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Service, properties, null);

    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    addRequestId(resultTree, 1L);

    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, "Failed for testing"), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Turn ON Maintenance Mode for MyService), RequestId(1), Status(Failed to queue), Reason(Failed for testing)";

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
