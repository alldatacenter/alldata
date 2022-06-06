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

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

/**
 * RequestStatusImpl Tests
 */
public class RequestStatusImplTest {

  @Test
  public void testGetAssociatedResources() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertEquals(Collections.emptySet(), status.getAssociatedResources());


    Resource associatedResource = new ResourceImpl(Resource.Type.Service);
    Set<Resource> associatedResources = Collections.singleton(associatedResource);
    status = new RequestStatusImpl(null, associatedResources);
    Assert.assertEquals(associatedResources, status.getAssociatedResources());
  }

  @Test
  public void testGetRequestResource() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertNull(status.getRequestResource());

    Resource requestResource = new ResourceImpl(Resource.Type.Request);
    status = new RequestStatusImpl(requestResource);

    Assert.assertEquals(requestResource, status.getRequestResource());
  }

  @Test
  public void testGetStatus() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertEquals(RequestStatus.Status.Complete, status.getStatus());

    Resource requestResource = new ResourceImpl(Resource.Type.Request);
    requestResource.setProperty("Requests/status", "InProgress");
    status = new RequestStatusImpl(requestResource);
    Assert.assertEquals(RequestStatus.Status.InProgress, status.getStatus());
  }

  @Test
  public void testGetRequestStatusMetadata() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertNull(status.getStatusMetadata());
    RequestStatusMetaData metaData = new RequestStatusMetaData() {};

    status = new RequestStatusImpl(null, null, metaData);
    Assert.assertEquals(metaData, status.getStatusMetadata());
  }
}
