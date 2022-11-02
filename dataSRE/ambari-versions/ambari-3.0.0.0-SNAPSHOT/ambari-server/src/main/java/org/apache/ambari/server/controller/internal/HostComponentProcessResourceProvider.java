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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.HostComponentProcessResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponentHost;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource Provider for HostComponent process resources.
 */
public class HostComponentProcessResourceProvider extends ReadOnlyResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String HOST_COMPONENT_PROCESS = "HostComponentProcess";

  public static final String NAME_PROPERTY_ID = "name";
  public static final String STATUS_PROPERTY_ID = "status";

  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String HOST_NAME_PROPERTY_ID = "host_name";
  public static final String COMPONENT_NAME_PROPERTY_ID = "component_name";

  public static final String NAME = HOST_COMPONENT_PROCESS + PropertyHelper.EXTERNAL_PATH_SEP + NAME_PROPERTY_ID;
  public static final String STATUS = HOST_COMPONENT_PROCESS + PropertyHelper.EXTERNAL_PATH_SEP + STATUS_PROPERTY_ID;
  
  public static final String CLUSTER_NAME = HOST_COMPONENT_PROCESS + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME_PROPERTY_ID;
  public static final String HOST_NAME = HOST_COMPONENT_PROCESS + PropertyHelper.EXTERNAL_PATH_SEP + HOST_NAME_PROPERTY_ID;
  public static final String COMPONENT_NAME = HOST_COMPONENT_PROCESS + PropertyHelper.EXTERNAL_PATH_SEP + COMPONENT_NAME_PROPERTY_ID;

  /**
   * The key property ids for a HostComponentProcess resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, CLUSTER_NAME)
      .put(Resource.Type.Host, HOST_NAME)
      .put(Resource.Type.Component, COMPONENT_NAME)
      .put(Resource.Type.HostComponent, COMPONENT_NAME)
      .put(Resource.Type.HostComponentProcess, NAME)
      .build();

  /**
   * The property ids for a HostComponentProcess resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      NAME,
      STATUS,
      CLUSTER_NAME,
      HOST_NAME,
      COMPONENT_NAME);

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param amc the management controller
   */
  HostComponentProcessResourceProvider(AmbariManagementController amc) {
    super(Resource.Type.HostComponentProcess, propertyIds, keyPropertyIds, amc);
  }


  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    final Set<Map<String, Object>> requestMaps = getPropertyMaps(predicate);

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<HostComponentProcessResponse> responses = getResources(new Command<Set<HostComponentProcessResponse>>() {
      @Override
      public Set<HostComponentProcessResponse> invoke() throws AmbariException {
        return getHostComponentProcesses(requestMaps);
      }
    });
    
    Set<Resource> resources = new HashSet<>();
    
    for (HostComponentProcessResponse response : responses) {
      Resource r = new ResourceImpl(Resource.Type.HostComponentProcess);
      
      setResourceProperty(r, CLUSTER_NAME, response.getCluster(),
          requestedIds);
      setResourceProperty(r, HOST_NAME, response.getHost(),
          requestedIds);
      setResourceProperty(r, COMPONENT_NAME, response.getComponent(), requestedIds);
      
      setResourceProperty(r, NAME, response.getValueMap().get("name"),
          requestedIds);
      setResourceProperty(r, STATUS, response.getValueMap().get("status"),
          requestedIds);
      
      // set the following even if they aren't defined
      for (Entry<String, String> entry : response.getValueMap().entrySet()) {
        // these are already set
        if (entry.getKey().equals("name") || entry.getKey().equals("status"))
          continue;
        
        setResourceProperty(r, "HostComponentProcess/" + entry.getKey(),
            entry.getValue(), requestedIds);
          
      }

      resources.add(r);
    }

    return resources;
  }
  
  // ----- Instance Methods ------------------------------------------------

  private Set<HostComponentProcessResponse> getHostComponentProcesses(
      Set<Map<String, Object>> requestMaps)
    throws AmbariException {
    
    Set<HostComponentProcessResponse> results = new HashSet<>();
    
    Clusters clusters = getManagementController().getClusters();

    for (Map<String, Object> requestMap : requestMaps) {
      
      String cluster = (String) requestMap.get(CLUSTER_NAME);
      String component = (String) requestMap.get(COMPONENT_NAME);
      String host = (String) requestMap.get(HOST_NAME);

      Cluster c = clusters.getCluster(cluster);
      
      Collection<ServiceComponentHost> schs = c.getServiceComponentHosts(host);
      
      for (ServiceComponentHost sch : schs) {
        if (!sch.getServiceComponentName().equals(component))
          continue;
        
        for (Map<String, String> proc : sch.getProcesses()) {
          results.add(new HostComponentProcessResponse(cluster, sch.getHostName(),
              component, proc));
        }
      }
      
    }
    return results;
  }
  


}
