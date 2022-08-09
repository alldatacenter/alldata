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
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Default request status implementation.
 */
public class RequestStatusImpl implements RequestStatus{

  private final Resource requestResource;
  private final Set<Resource> associatedResources;
  private final RequestStatusMetaData requestStatusMetaData;
  public RequestStatusImpl(Resource requestResource) {
    this(requestResource, null, null);
  }

  public RequestStatusImpl(Resource requestResource, Set<Resource> associatedResources) {
    this(requestResource, associatedResources, null);
  }

  public RequestStatusImpl(Resource requestResource, Set<Resource> associatedResources, RequestStatusMetaData requestStatusMetaData) {
    this.requestResource     = requestResource;
    this.associatedResources = associatedResources == null ?
        Collections.emptySet() : associatedResources;
    this.requestStatusMetaData = requestStatusMetaData;
  }

  @Override
  public Set<Resource> getAssociatedResources() {
    return associatedResources;
  }

  @Override
  public Resource getRequestResource() {
    return requestResource;
  }

  @Override
  public Status getStatus() {

    return requestResource == null ? Status.Complete :
        Status.valueOf((String) requestResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));
  }

  @Override
  public RequestStatusMetaData getStatusMetadata() {
    return requestStatusMetaData;
  }
}
