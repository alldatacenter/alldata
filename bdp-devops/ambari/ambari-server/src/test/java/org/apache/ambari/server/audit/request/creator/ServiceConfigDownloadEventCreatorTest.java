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
import org.apache.ambari.server.audit.event.request.ClientConfigDownloadRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.ServiceConfigDownloadEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class ServiceConfigDownloadEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void getTest() {
    ServiceConfigDownloadEventCreator creator = new ServiceConfigDownloadEventCreator();

    Map<Resource.Type,String> resource = new HashMap<>();
    resource.put(Resource.Type.Service, "MYSERVICE");
    resource.put(Resource.Type.Component, "MYCOMPONENT");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.GET, Resource.Type.ClientConfig, null, resource);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Client config download), RequestType(GET), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Service(MYSERVICE), Component(MYCOMPONENT)";

    Assert.assertTrue("Class mismatch", event instanceof ClientConfigDownloadRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
