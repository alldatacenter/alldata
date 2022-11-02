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

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.ClusterNameChangeRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ConfigurationChangeRequestAuditEvent;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceConfigVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * This creator handles operation requests (start, stop, install, etc)
 * For resource type {@link Resource.Type#HostComponent}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class ConfigurationChangeEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Cluster).build();

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return resourceTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    if (!request.getBody().getPropertySets().isEmpty()) {
      Map<String, Object> map = Iterables.getFirst(request.getBody().getPropertySets(), null);
      if (map != null && map.size() == 1 && map.containsKey(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID)) {
        String newName = String.valueOf(map.get(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));
        String oldName = request.getResource().getKeyValueMap().get(Resource.Type.Cluster);
        return ClusterNameChangeRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withOldName(oldName)
          .withNewName(newName)
          .build();
      }
    }

    return ConfigurationChangeRequestAuditEvent.builder()
      .withTimestamp(System.currentTimeMillis())
      .withRequestType(request.getRequestType())
      .withResultStatus(result.getStatus())
      .withUrl(request.getURI())
      .withRemoteIp(request.getRemoteAddress())
      .withVersionNote(getServiceConfigVersionNote(result))
      .withVersionNumber(getServiceConfigVersion(result))
      .build();
  }

  /**
   * Returns service configuration version from the result
   * @param result
   * @return
   */
  private String getServiceConfigVersion(Result result) {
    Map<String, Object> map = getServiceConfigMap(result);
    return map == null ? null : String.valueOf(map.get(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_PROPERTY_ID));
  }

  /**
   * Returns service configurationv ersion note from the result
   * @param result
   * @return
   */
  private String getServiceConfigVersionNote(Result result) {
    Map<String, Object> map = getServiceConfigMap(result);
    return map == null ? null : String.valueOf(map.get(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID));
  }

  /**
   * Returns service configuration map from the result. This map contains version number and version info
   * @param result
   * @return
   */
  private Map<String, Object> getServiceConfigMap(Result result) {
    if (result.getResultTree().getChild("resources") != null) {
      TreeNode<Resource> first = Iterables.getFirst(result.getResultTree().getChild("resources").getChildren(), null);
      if(first != null && first.getObject() != null) {
        return first.getObject().getPropertiesMap().get("");
      }
    }
    return null;
  }

}
