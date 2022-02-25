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

import org.apache.ambari.server.controller.ivory.Feed;
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
 * DR feed resource provider.
 */
public class FeedResourceProvider extends AbstractDRResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String FEED_NAME_PROPERTY_ID                  = PropertyHelper.getPropertyId("Feed", "name");
  protected static final String FEED_DESCRIPTION_PROPERTY_ID           = PropertyHelper.getPropertyId("Feed", "description");
  protected static final String FEED_STATUS_PROPERTY_ID                = PropertyHelper.getPropertyId("Feed", "status");
  protected static final String FEED_SCHEDULE_PROPERTY_ID              = PropertyHelper.getPropertyId("Feed", "frequency");
  protected static final String FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("Feed/sourceCluster", "name");
  protected static final String FEED_SOURCE_CLUSTER_START_PROPERTY_ID  = PropertyHelper.getPropertyId("Feed/sourceCluster/validity", "start");
  protected static final String FEED_SOURCE_CLUSTER_END_PROPERTY_ID    = PropertyHelper.getPropertyId("Feed/sourceCluster/validity", "end");
  protected static final String FEED_SOURCE_CLUSTER_LIMIT_PROPERTY_ID  = PropertyHelper.getPropertyId("Feed/sourceCluster/retention", "limit");
  protected static final String FEED_SOURCE_CLUSTER_ACTION_PROPERTY_ID = PropertyHelper.getPropertyId("Feed/sourceCluster/retention", "action");
  protected static final String FEED_TARGET_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("Feed/targetCluster", "name");
  protected static final String FEED_TARGET_CLUSTER_START_PROPERTY_ID  = PropertyHelper.getPropertyId("Feed/targetCluster/validity", "start");
  protected static final String FEED_TARGET_CLUSTER_END_PROPERTY_ID    = PropertyHelper.getPropertyId("Feed/targetCluster/validity", "end");
  protected static final String FEED_TARGET_CLUSTER_LIMIT_PROPERTY_ID  = PropertyHelper.getPropertyId("Feed/targetCluster/retention", "limit");
  protected static final String FEED_TARGET_CLUSTER_ACTION_PROPERTY_ID = PropertyHelper.getPropertyId("Feed/targetCluster/retention", "action");
  protected static final String FEED_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId("Feed", "properties");

  /**
   * The key property ids for a Feed resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.DRFeed, FEED_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Feed resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      FEED_NAME_PROPERTY_ID,
      FEED_DESCRIPTION_PROPERTY_ID,
      FEED_STATUS_PROPERTY_ID,
      FEED_SCHEDULE_PROPERTY_ID,
      FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID,
      FEED_SOURCE_CLUSTER_START_PROPERTY_ID,
      FEED_SOURCE_CLUSTER_END_PROPERTY_ID,
      FEED_SOURCE_CLUSTER_LIMIT_PROPERTY_ID,
      FEED_SOURCE_CLUSTER_ACTION_PROPERTY_ID,
      FEED_TARGET_CLUSTER_NAME_PROPERTY_ID,
      FEED_TARGET_CLUSTER_START_PROPERTY_ID,
      FEED_TARGET_CLUSTER_END_PROPERTY_ID,
      FEED_TARGET_CLUSTER_LIMIT_PROPERTY_ID,
      FEED_TARGET_CLUSTER_ACTION_PROPERTY_ID,
      FEED_PROPERTIES_PROPERTY_ID);

  /**
   * Construct a provider.
   *
   * @param ivoryService    the ivory service
   */
  public FeedResourceProvider(IvoryService ivoryService) {
    super(propertyIds, keyPropertyIds, ivoryService);
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
             ResourceAlreadyExistsException, NoSuchParentResourceException {
    IvoryService service = getService();

    Set<Map<String, Object>> propertiesSet = request.getProperties();

    for(Map<String, Object> propertyMap : propertiesSet) {
      service.submitFeed(getFeed((String) propertyMap.get(FEED_NAME_PROPERTY_ID), propertyMap));
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    IvoryService  service      = getService();
    List<String>  feedNames    = service.getFeedNames();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    for (String feedName : feedNames ) {

      Feed feed = service.getFeed(feedName);

      Resource resource = new ResourceImpl(Resource.Type.DRFeed);
      setResourceProperty(resource, FEED_NAME_PROPERTY_ID,
          feed.getName(), requestedIds);
      setResourceProperty(resource, FEED_DESCRIPTION_PROPERTY_ID,
          feed.getDescription(), requestedIds);
      setResourceProperty(resource, FEED_STATUS_PROPERTY_ID,
          feed.getStatus(), requestedIds);
      setResourceProperty(resource, FEED_SCHEDULE_PROPERTY_ID,
          feed.getSchedule(), requestedIds);
      setResourceProperty(resource, FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID,
          feed.getSourceClusterName(), requestedIds);
      setResourceProperty(resource, FEED_SOURCE_CLUSTER_START_PROPERTY_ID,
          feed.getSourceClusterStart(), requestedIds);
      setResourceProperty(resource, FEED_SOURCE_CLUSTER_END_PROPERTY_ID,
          feed.getSourceClusterEnd(), requestedIds);
      setResourceProperty(resource, FEED_SOURCE_CLUSTER_LIMIT_PROPERTY_ID,
          feed.getSourceClusterLimit(), requestedIds);
      setResourceProperty(resource, FEED_SOURCE_CLUSTER_ACTION_PROPERTY_ID,
          feed.getSourceClusterAction(), requestedIds);
      setResourceProperty(resource, FEED_TARGET_CLUSTER_NAME_PROPERTY_ID,
          feed.getTargetClusterName(), requestedIds);
      setResourceProperty(resource, FEED_TARGET_CLUSTER_START_PROPERTY_ID,
          feed.getTargetClusterStart(), requestedIds);
      setResourceProperty(resource, FEED_TARGET_CLUSTER_END_PROPERTY_ID,
          feed.getTargetClusterEnd(), requestedIds);
      setResourceProperty(resource, FEED_TARGET_CLUSTER_LIMIT_PROPERTY_ID,
          feed.getTargetClusterLimit(), requestedIds);
      setResourceProperty(resource, FEED_TARGET_CLUSTER_ACTION_PROPERTY_ID,
          feed.getTargetClusterAction(), requestedIds);
      setResourceProperty(resource, FEED_PROPERTIES_PROPERTY_ID,
          feed.getProperties(), requestedIds);

      if (predicate == null || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {

      Map<String, Object> propertyMap = iterator.next();

      String desiredStatus = (String) propertyMap.get(FEED_STATUS_PROPERTY_ID);

      // get all the feeds that pass the predicate check
      Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

      // update all the matching feeds with the property values from the request
      for (Resource resource : resources) {
        IvoryService service = getService();
        if (desiredStatus != null) {
          String status   = (String) resource.getPropertyValue(FEED_STATUS_PROPERTY_ID);
          String feedName = (String) resource.getPropertyValue(FEED_NAME_PROPERTY_ID);

          if (desiredStatus.equals("SCHEDULED")) {
            service.scheduleFeed(feedName);
          } else if (desiredStatus.equals("SUSPENDED")) {
            service.suspendFeed(feedName);
          } else if (status.equals("SUSPENDED") && desiredStatus.equals("RUNNING")) {
            service.resumeFeed(feedName);
          }
        }
        service.updateFeed(getFeed((String) resource.getPropertyValue(FEED_NAME_PROPERTY_ID),
            getUpdateMap(resource, propertyMap)));
      }
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {
    IvoryService service = getService();

    // get all the feeds that pass the predicate check
    Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

    for (Resource resource : resources) {
      // delete all the matching feeds with the property values from the request
      service.deleteFeed((String) resource.getPropertyValue(FEED_NAME_PROPERTY_ID));
    }
    return new RequestStatusImpl(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- helper methods -----------------------------------------------------

  /**
   * Get a new feed object from the given name and map of properties.
   *
   * @param feedName     the feed name
   * @param propertyMap  the properties
   *
   * @return a new feed
   */
  protected static Feed getFeed(String feedName, Map<String, Object> propertyMap) {
    Map<String, String> properties = new HashMap<>();
    for ( Map.Entry<String, Object> entry : propertyMap.entrySet()) {
      String property = entry.getKey();
      String category = PropertyHelper.getPropertyCategory(property);
      if (category.equals(FEED_PROPERTIES_PROPERTY_ID)) {
        properties.put(PropertyHelper.getPropertyName(property), (String) entry.getValue());
      }
    }

    return new Feed(
        feedName,
        (String) propertyMap.get(FEED_DESCRIPTION_PROPERTY_ID),
        (String) propertyMap.get(FEED_STATUS_PROPERTY_ID),
        (String) propertyMap.get(FEED_SCHEDULE_PROPERTY_ID),
        (String) propertyMap.get(FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID),
        (String) propertyMap.get(FEED_SOURCE_CLUSTER_START_PROPERTY_ID),
        (String) propertyMap.get(FEED_SOURCE_CLUSTER_END_PROPERTY_ID),
        (String) propertyMap.get(FEED_SOURCE_CLUSTER_LIMIT_PROPERTY_ID),
        (String) propertyMap.get(FEED_SOURCE_CLUSTER_ACTION_PROPERTY_ID),
        (String) propertyMap.get(FEED_TARGET_CLUSTER_NAME_PROPERTY_ID),
        (String) propertyMap.get(FEED_TARGET_CLUSTER_START_PROPERTY_ID),
        (String) propertyMap.get(FEED_TARGET_CLUSTER_END_PROPERTY_ID),
        (String) propertyMap.get(FEED_TARGET_CLUSTER_LIMIT_PROPERTY_ID),
        (String) propertyMap.get(FEED_TARGET_CLUSTER_ACTION_PROPERTY_ID),
        properties);
  }

  /**
   * Get a property map for an update based on an existing feed resource updated with the
   * given property map.
   *
   * @param resource     the resource
   * @param propertyMap  the map of property updates
   *
   * @return the map of properies to use for the update
   */
  protected static Map<String, Object> getUpdateMap(Resource resource, Map<String, Object> propertyMap) {
    Map<String, Object> updateMap = new HashMap<>();

    updateMap.put(FEED_NAME_PROPERTY_ID, resource.getPropertyValue(FEED_NAME_PROPERTY_ID));
    updateMap.put(FEED_DESCRIPTION_PROPERTY_ID, resource.getPropertyValue(FEED_DESCRIPTION_PROPERTY_ID));
    updateMap.put(FEED_SCHEDULE_PROPERTY_ID, resource.getPropertyValue(FEED_SCHEDULE_PROPERTY_ID));
    updateMap.put(FEED_STATUS_PROPERTY_ID, resource.getPropertyValue(FEED_STATUS_PROPERTY_ID));
    updateMap.put(FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID, resource.getPropertyValue(FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID));
    updateMap.put(FEED_SOURCE_CLUSTER_START_PROPERTY_ID, resource.getPropertyValue(FEED_SOURCE_CLUSTER_START_PROPERTY_ID));
    updateMap.put(FEED_SOURCE_CLUSTER_END_PROPERTY_ID, resource.getPropertyValue(FEED_SOURCE_CLUSTER_END_PROPERTY_ID));
    updateMap.put(FEED_SOURCE_CLUSTER_LIMIT_PROPERTY_ID, resource.getPropertyValue(FEED_SOURCE_CLUSTER_LIMIT_PROPERTY_ID));
    updateMap.put(FEED_SOURCE_CLUSTER_ACTION_PROPERTY_ID, resource.getPropertyValue(FEED_SOURCE_CLUSTER_ACTION_PROPERTY_ID));
    updateMap.put(FEED_TARGET_CLUSTER_NAME_PROPERTY_ID, resource.getPropertyValue(FEED_TARGET_CLUSTER_NAME_PROPERTY_ID));
    updateMap.put(FEED_TARGET_CLUSTER_START_PROPERTY_ID, resource.getPropertyValue(FEED_TARGET_CLUSTER_START_PROPERTY_ID));
    updateMap.put(FEED_TARGET_CLUSTER_END_PROPERTY_ID, resource.getPropertyValue(FEED_TARGET_CLUSTER_END_PROPERTY_ID));
    updateMap.put(FEED_TARGET_CLUSTER_LIMIT_PROPERTY_ID, resource.getPropertyValue(FEED_TARGET_CLUSTER_LIMIT_PROPERTY_ID));
    updateMap.put(FEED_TARGET_CLUSTER_ACTION_PROPERTY_ID, resource.getPropertyValue(FEED_TARGET_CLUSTER_ACTION_PROPERTY_ID));
    updateMap.put(FEED_PROPERTIES_PROPERTY_ID, resource.getPropertyValue(FEED_PROPERTIES_PROPERTY_ID));
    updateMap.putAll(propertyMap);

    return updateMap;
  }

}
