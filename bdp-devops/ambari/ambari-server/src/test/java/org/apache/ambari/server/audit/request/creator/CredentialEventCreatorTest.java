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
import org.apache.ambari.server.audit.event.request.AddCredentialRequestAuditEvent;
import org.apache.ambari.server.audit.request.eventcreator.CredentialEventCreator;
import org.apache.ambari.server.controller.internal.CredentialResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

public class CredentialEventCreatorTest extends AuditEventCreatorTestBase{

  @Test
  public void postTest() {
    CredentialEventCreator creator = new CredentialEventCreator();

    Map<String,Object> properties = new HashMap<>();
    properties.put(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID, "mycluster");
    properties.put(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID, "USER");
    properties.put(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID, "Alias");
    properties.put(CredentialResourceProvider.CREDENTIAL_PRINCIPAL_PROPERTY_ID, "newuser");

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.Credential, properties, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEvent event = AuditEventCreatorTestHelper.getEvent(creator, request, result);

    String actual = event.getAuditMessage();
    String expected = "User(" + userName + "), RemoteIp(1.2.3.4), Operation(Credential addition), RequestType(POST), url(http://example.com:8080/api/v1/test), ResultStatus(200 OK), Type(USER), Principal(newuser), Alias(Alias), Cluster name(mycluster)";

    Assert.assertTrue("Class mismatch", event instanceof AddCredentialRequestAuditEvent);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.contains(userName));
  }

}
