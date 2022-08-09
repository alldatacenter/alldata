/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.junit.Assert;

/**
 * UpgradeItemService tests.
 */
public class UpgradeItemServiceTest extends BaseServiceTest {
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //updateServices
    UpgradeItemService service = new TestUpgradeItemService("clusterName", "upgradeId", "upgradeGroupId", 99L);
    Method m = service.getClass().getMethod("updateUpgradeItem", String.class, HttpHeaders.class, UriInfo.class, Long.class);
    Object[] args = new Object[] {"body", getHttpHeaders(), getUriInfo(), 99L};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    return listInvocations;
  }

  private class TestUpgradeItemService extends UpgradeItemService {
    private Long m_upgradeItemId;

    private TestUpgradeItemService(String clusterName, String upgradeId, String upgradeGroupId, Long upgradeItemId) {
      super(clusterName, upgradeId, upgradeGroupId);

      m_upgradeItemId = upgradeItemId;
    }

    @Override
    ResourceInstance createResourceInstance(Long upgradeItemId) {
      Assert.assertEquals(m_upgradeItemId, upgradeItemId);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }
}
