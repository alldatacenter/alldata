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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A request for a scaling an existing cluster.
 */
public class ScaleClusterRequest extends BaseClusterRequest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScaleClusterRequest.class);

  /**
   * cluster name
   */
  private String clusterName;

  /**
   * Constructor.
   *
   * @param propertySet  set of request properties
   *
   * @throws InvalidTopologyTemplateException if any validation of properties fails
   */
  public ScaleClusterRequest(Set<Map<String, Object>> propertySet) throws InvalidTopologyTemplateException {
    for (Map<String, Object> properties : propertySet) {
      // can only operate on a single cluster per logical request
      if (getClusterName() == null) {
        setClusterName(String.valueOf(properties.get(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID)));
      }
      // currently don't allow cluster scoped configuration in scaling operation
      setConfiguration(new Configuration(Collections.emptyMap(),
          Collections.emptyMap()));

      parseHostGroups(properties);
    }
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public Type getType() {
    return Type.SCALE;
  }

  @Override
  public String getDescription() {
    return String.format("Scale Cluster '%s' (+%s hosts)", clusterName, getTotalRequestedHostCount());
  }

  /**
   * Parse and set host group information.
   *
   * @param properties  request properties
   * @throws InvalidTopologyTemplateException if any property validation fails
   */
  //todo: need to use fully qualified host group name.  For now, disregard name collisions across BP's
  private void parseHostGroups(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    String blueprintName = String.valueOf(properties.get(HostResourceProvider.BLUEPRINT_PROPERTY_ID));
    if (blueprintName == null || blueprintName.equals("null")) {
      throw new InvalidTopologyTemplateException("Blueprint name must be specified for all host groups");
    }

    String hgName = String.valueOf(properties.get(HostResourceProvider.HOST_GROUP_PROPERTY_ID));
    if (hgName == null || hgName.equals("null")) {
      throw new InvalidTopologyTemplateException("A name must be specified for all host groups");
    }

    Blueprint blueprint = getBlueprint();
    if (getBlueprint() == null) {
      blueprint = parseBlueprint(blueprintName);
      setBlueprint(blueprint);
    } else if (! blueprintName.equals(blueprint.getName())) {
      throw new InvalidTopologyTemplateException(
          "Currently, a scaling request may only refer to a single blueprint");
    }

    String hostName = HostResourceProvider.getHostNameFromProperties(properties);
    boolean containsHostCount = properties.containsKey(HostResourceProvider.HOST_COUNT_PROPERTY_ID);
    boolean containsHostPredicate = properties.containsKey(HostResourceProvider.HOST_PREDICATE_PROPERTY_ID);

    if (hostName != null && (containsHostCount || containsHostPredicate)) {
      throw new InvalidTopologyTemplateException(
          "Can't specify host_count or host_predicate if host_name is specified in hostgroup: " + hgName);
    }

    if (hostName == null && ! containsHostCount) {
      throw new InvalidTopologyTemplateException(
          "Must specify either host_name or host_count for hostgroup: " + hgName);
    }

    HostGroupInfo hostGroupInfo = getHostGroupInfo().get(hgName);
    if (hostGroupInfo == null) {
      if (blueprint.getHostGroup(hgName) == null) {
        throw new InvalidTopologyTemplateException("Invalid host group specified in request: " + hgName);
      }
      hostGroupInfo = new HostGroupInfo(hgName);
      getHostGroupInfo().put(hgName, hostGroupInfo);
    }

    // specifying configuration is scaling request isn't permitted
    hostGroupInfo.setConfiguration(new Configuration(Collections.emptyMap(),
        Collections.emptyMap()));

    // process host_name and host_count
    if (containsHostCount) {
      //todo: host_count and host_predicate up one level
      if (containsHostPredicate) {
        String predicate = String.valueOf(properties.get(HostResourceProvider.HOST_PREDICATE_PROPERTY_ID));
        validateHostPredicateProperties(predicate);
        try {
          hostGroupInfo.setPredicate(predicate);
        } catch (InvalidQueryException e) {
          throw new InvalidTopologyTemplateException(
              String.format("Unable to compile host predicate '%s': %s", predicate, e), e);
        }
      }

      if (! hostGroupInfo.getHostNames().isEmpty()) {
        throw new InvalidTopologyTemplateException(
            "Can't specify both host_name and host_count for the same hostgroup: " + hgName);
      }
      hostGroupInfo.setRequestedCount(Integer.parseInt(String.valueOf(
          properties.get(HostResourceProvider.HOST_COUNT_PROPERTY_ID))));
    } else {
      if (hostGroupInfo.getRequestedHostCount() != hostGroupInfo.getHostNames().size()) {
        // host_name specified in one host block and host_count in another for the same group
        throw new InvalidTopologyTemplateException("Invalid host group specified in request: " + hgName);
      }
      hostGroupInfo.addHost(hostName);
      hostGroupInfo.addHostRackInfo(hostName, processRackInfo(properties));
    }
  }

  private String processRackInfo(Map<String, Object> properties) {
    String rackInfo = null;
    if (properties.containsKey(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID)) {
      rackInfo = (String) properties.get(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID);
    } else if (properties.containsKey(HostResourceProvider.RACK_INFO_PROPERTY_ID)) {
      rackInfo = (String) properties.get(HostResourceProvider.RACK_INFO_PROPERTY_ID);
    } else {
      LOGGER.debug("No rack info provided");
    }
    return rackInfo;
  }


  /**
   * Parse blueprint.
   *
   * @param blueprintName  blueprint name
   * @return blueprint instance
   *
   * @throws InvalidTopologyTemplateException if specified blueprint or stack doesn't exist
   */
  private Blueprint parseBlueprint(String blueprintName) throws InvalidTopologyTemplateException  {
    Blueprint blueprint;
    try {
      blueprint = getBlueprintFactory().getBlueprint(blueprintName);
    } catch (NoSuchStackException e) {
      throw new InvalidTopologyTemplateException("Invalid stack specified in the blueprint: " + blueprintName);
    }

    if (blueprint == null) {
      throw new InvalidTopologyTemplateException("The specified blueprint doesn't exist: " + blueprintName);
    }
    return blueprint;
  }

  /**
   * Get the total number of requested hosts for the request.
   * @return  total requested host count
   */
  private int getTotalRequestedHostCount() {
    int count = 0;
    for (HostGroupInfo groupInfo : getHostGroupInfo().values()) {
      count += groupInfo.getRequestedHostCount();
    }
    return count;
  }
}
