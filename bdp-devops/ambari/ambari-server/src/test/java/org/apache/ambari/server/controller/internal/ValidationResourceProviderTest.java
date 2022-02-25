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

package org.apache.ambari.server.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse.Version;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Clusters;
import org.junit.Test;

public class ValidationResourceProviderTest {

  @Test
  public void testCreateResources_checkRequestId() throws Exception {
    Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
    Set<String> propertyIds = Collections.singleton(ValidationResourceProvider.VALIDATION_ID_PROPERTY_ID);
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    ValidationResourceProvider provider = spy(new ValidationResourceProvider(ambariManagementController));
    StackAdvisorRequest stackAdvisorRequest = mock(StackAdvisorRequest.class);
    Request request = mock(Request.class);
    doReturn(stackAdvisorRequest).when(provider).prepareStackAdvisorRequest(request);

    StackAdvisorHelper saHelper = mock(StackAdvisorHelper.class);
    Configuration configuration = mock(Configuration.class);

    ValidationResponse response = mock(ValidationResponse.class);
    Version version = mock(Version.class);
    doReturn(3).when(response).getId();
    doReturn(version).when(response).getVersion();
    doReturn(response).when(saHelper).validate(any(StackAdvisorRequest.class));
    ValidationResourceProvider.init(saHelper, configuration, mock(Clusters.class), mock(AmbariMetaInfo.class));

    RequestStatus status = provider.createResources(request);

    Set<Resource> associatedResources = status.getAssociatedResources();
    assertNotNull(associatedResources);
    assertEquals(1, associatedResources.size());
    Resource resource = associatedResources.iterator().next();
    Object requestId = resource.getPropertyValue(ValidationResourceProvider.VALIDATION_ID_PROPERTY_ID);
    assertNotNull(requestId);
    assertEquals(3, requestId);
  }
}
