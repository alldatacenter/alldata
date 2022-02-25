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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ivory.Cluster;
import org.apache.ambari.server.controller.ivory.IvoryService;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * DR target cluster resource provider.
 */
public class TargetClusterResourceProvider extends AbstractDRResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String CLUSTER_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("Cluster", "name");
  protected static final String CLUSTER_COLO_PROPERTY_ID       = PropertyHelper.getPropertyId("Cluster", "colo");
  protected static final String CLUSTER_INTERFACES_PROPERTY_ID = PropertyHelper.getPropertyId("Cluster", "interfaces");
  protected static final String CLUSTER_LOCATIONS_PROPERTY_ID  = PropertyHelper.getPropertyId("Cluster", "locations");
  protected static final String CLUSTER_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId("Cluster", "properties");

  /**
   * The key property ids for a TargetCluster resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, CLUSTER_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a TargetCluster resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      CLUSTER_NAME_PROPERTY_ID,
      CLUSTER_COLO_PROPERTY_ID,
      CLUSTER_INTERFACES_PROPERTY_ID,
      CLUSTER_LOCATIONS_PROPERTY_ID,
      CLUSTER_PROPERTIES_PROPERTY_ID);

  /**
   * Construct a provider.
   *
   * @param ivoryService    the ivory service
   */
  public TargetClusterResourceProvider(IvoryService ivoryService) {
    super(propertyIds, keyPropertyIds, ivoryService);
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {
    IvoryService service = getService();

    Set<Map<String, Object>> propertiesSet = request.getProperties();

    for(Map<String, Object> propertyMap : propertiesSet) {
      service.submitCluster(getCluster((String) propertyMap.get(CLUSTER_NAME_PROPERTY_ID), propertyMap));
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    IvoryService  service         = getService();
    List<String>  clusterNames    = service.getClusterNames();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    for (String clusterName : clusterNames ) {

      Cluster cluster = service.getCluster(clusterName);

      Resource resource = new ResourceImpl(Resource.Type.DRTargetCluster);
      setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID,
          cluster.getName(), requestedIds);
      setResourceProperty(resource, CLUSTER_COLO_PROPERTY_ID,
          cluster.getColo(), requestedIds);
      setResourceProperty(resource, CLUSTER_INTERFACES_PROPERTY_ID,
          cluster.getInterfaces(), requestedIds);
      setResourceProperty(resource, CLUSTER_LOCATIONS_PROPERTY_ID,
          cluster.getLocations(), requestedIds);
      setResourceProperty(resource, CLUSTER_PROPERTIES_PROPERTY_ID,
          cluster.getProperties(), requestedIds);

      if (predicate == null || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    IvoryService service = getService();

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {

      Map<String, Object> propertyMap = iterator.next();

      // get all the clusters that pass the predicate check
      Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

      for (Resource resource : resources) {
        // update all the matching clusters with the property values from the request
        service.updateCluster(getCluster((String) resource.getPropertyValue(CLUSTER_NAME_PROPERTY_ID), propertyMap));
      }
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    IvoryService service = getService();

    // get all the clusters that pass the predicate check
    Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

    for (Resource resource : resources) {
      // delete all the matching clusters with the property values from the request
      service.deleteCluster((String) resource.getPropertyValue(CLUSTER_NAME_PROPERTY_ID));
    }
    return new RequestStatusImpl(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- helper methods -----------------------------------------------------

  protected static Cluster getCluster(String clusterName, Map<String, Object> propertyMap) {

    Map<String, String> properties = new HashMap<>();
    for ( Map.Entry<String, Object> entry : propertyMap.entrySet()) {
      String property = entry.getKey();
      String category = PropertyHelper.getPropertyCategory(property);
      if (category.equals(CLUSTER_PROPERTIES_PROPERTY_ID)) {
        properties.put(PropertyHelper.getPropertyName(property), (String) entry.getValue());
      }
    }

    return new Cluster(
        clusterName,
        (String) propertyMap.get(CLUSTER_COLO_PROPERTY_ID),
        getInterfaces((Set<Map<String, Object>>) propertyMap.get(CLUSTER_INTERFACES_PROPERTY_ID)),
        getLocations((Set<Map<String, Object>>) propertyMap.get(CLUSTER_LOCATIONS_PROPERTY_ID)),
        properties);
  }

  protected static Set<Cluster.Interface> getInterfaces(Set<Map<String, Object>> maps) {
    Set<Cluster.Interface> interfaces = new HashSet<>();
    for (Map<String, Object> map : maps) {
      interfaces.add(new Cluster.Interface((String) map.get("type"), (String) map.get("endpoint"), (String) map.get("version")));
    }
    return interfaces;
  }

  protected static Set<Cluster.Location> getLocations(Set<Map<String, Object>> maps) {
    Set<Cluster.Location> locations = new HashSet<>();
    for (Map<String, Object> map : maps) {
      locations.add(new Cluster.Location((String) map.get("name"), (String) map.get("path")));
    }
    return locations;
  }

}
