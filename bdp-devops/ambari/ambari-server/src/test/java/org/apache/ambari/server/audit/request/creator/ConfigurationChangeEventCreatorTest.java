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
import org.apache.ambari.server.audit.event.request.ClusterNameChangeRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ConfigurationChangeRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.ConfigurationChangeEventCreator;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.ServiceConfigVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class ConfigurationChangeEventCreatorTest extends AuditEventCreatorTestBase {

  @Test
  public void clusterNameChangeTest() {
    ConfigurationChangeEventCreator creator = new ConfigurationChangeEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "newname");

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Cluster, "oldname");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Cluster, properties, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Cluster name change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Old name(oldname), New name(newname)";

    Assert.assertTrue("Class mismatch", event instanceof ClusterNameChangeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void configurationChangeTest() {
    ConfigurationChangeEventCreator creator = new ConfigurationChangeEventCreator();


    TreeNode<Resource> resultTree = new TreeNodeImpl<>(null, null, null);
    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    TreeNode<Resource> resourceNode = new TreeNodeImpl<>(resultTree, resource, "resources");
    Resource version = new ResourceImpl(Resource.Type.Cluster);
    version.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_PROPERTY_ID, "1");
    version.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID, "note");
    TreeNode<Resource> versionNode = new TreeNodeImpl<>(resourceNode, version, "");
    resourceNode.addChild(versionNode);
    resultTree.addChild(resourceNode);

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.PUT, Resource.Type.Cluster, new HashMap<>(), null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK), resultTree);

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Configuration change), RequestType(PUT), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), VersionNumber(V1), VersionNote(note)";

    Assert.assertTrue("Class mismatch", event instanceof ConfigurationChangeRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }
}
