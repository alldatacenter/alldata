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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddComponentToHostRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.AddHostRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteHostRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.HostEventCreator;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class HostEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    HostEventCreator creator = new HostEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, "ambari1.example.com");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.Host, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Host addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Hostname(ambari1.example.com)";

    Assert.assertTrue("Class mismatch", event instanceof AddHostRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void queryPostTest() {
    HostEventCreator creator = new HostEventCreator();

    Map<String,Object> properties = new HashMap<>();

    Set<Map<String,String>> set = new HashSet<>();
    Map<String,String> map = new HashMap<>();
    map.put(HostComponentResourceProvider.COMPONENT_NAME, "MYCOMPONENT");
    set.add(map);

    properties.put("host_components", set);

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.QUERY_POST, Resource.Type.Host, properties, null, HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID + "=ambari1.example.com");
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Component addition to host), RequestType(QUERY_POST), url(http://example.com:8080/api/v1/testHosts/host_name=ambari1.example.com), ResultStatus(200 OK), Hostname(ambari1.example.com), Component(MYCOMPONENT)";

    Assert.assertTrue("Class mismatch", event instanceof AddComponentToHostRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

  @Test
  public void deleteTest() {
    HostEventCreator creator = new HostEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Host, "ambari1.example.com");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.DELETE, Resource.Type.Host, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Host deletion), RequestType(DELETE), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Hostname(ambari1.example.com)";

    Assert.assertTrue("Class mismatch", event instanceof DeleteHostRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
