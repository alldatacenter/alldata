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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ivory.Instance;
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
 * DR instance resource provider.
 */
public class InstanceResourceProvider extends AbstractDRResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String INSTANCE_FEED_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("Instance", "feedName");
  protected static final String INSTANCE_ID_PROPERTY_ID         = PropertyHelper.getPropertyId("Instance", "id");
  protected static final String INSTANCE_STATUS_PROPERTY_ID     = PropertyHelper.getPropertyId("Instance", "status");
  protected static final String INSTANCE_START_TIME_PROPERTY_ID = PropertyHelper.getPropertyId("Instance", "startTime");
  protected static final String INSTANCE_END_TIME_PROPERTY_ID   = PropertyHelper.getPropertyId("Instance", "endTime");
  protected static final String INSTANCE_DETAILS_PROPERTY_ID    = PropertyHelper.getPropertyId("Instance", "details");
  protected static final String INSTANCE_LOG_PROPERTY_ID        = PropertyHelper.getPropertyId("Instance", "log");



  /**
   * The key property ids for a Instance resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.DRInstance, INSTANCE_FEED_NAME_PROPERTY_ID)
      .put(Resource.Type.Workflow, INSTANCE_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Instance resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      INSTANCE_FEED_NAME_PROPERTY_ID,
      INSTANCE_ID_PROPERTY_ID,
      INSTANCE_STATUS_PROPERTY_ID,
      INSTANCE_START_TIME_PROPERTY_ID,
      INSTANCE_END_TIME_PROPERTY_ID,
      INSTANCE_DETAILS_PROPERTY_ID,
      INSTANCE_LOG_PROPERTY_ID);

  /**
   * Construct a provider.
   *
   * @param ivoryService    the ivory service
   */
  public InstanceResourceProvider(IvoryService ivoryService) {
    super(propertyIds, keyPropertyIds, ivoryService);
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    // we can't create instances directly
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();
    List<String>  feedNames    = new LinkedList<>();

    IvoryService service = getService();
    if (predicate == null) {
      feedNames = service.getFeedNames();
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        String feedName = (String) propertyMap.get(INSTANCE_FEED_NAME_PROPERTY_ID);
        if (feedName == null) {
          // if any part of the predicate doesn't include feed name then we have to check them all
          feedNames = service.getFeedNames();
          break;
        }
        feedNames.add(feedName);
      }
    }

    for (String feedName : feedNames) {
      List<Instance> instances = service.getInstances(feedName);
      for (Instance instance : instances) {
        Resource resource = new ResourceImpl(Resource.Type.DRInstance);
        setResourceProperty(resource, INSTANCE_FEED_NAME_PROPERTY_ID,
            instance.getFeedName(), requestedIds);
        setResourceProperty(resource, INSTANCE_ID_PROPERTY_ID,
            instance.getId(), requestedIds);
        setResourceProperty(resource, INSTANCE_STATUS_PROPERTY_ID,
            instance.getStatus(), requestedIds);
        setResourceProperty(resource, INSTANCE_START_TIME_PROPERTY_ID,
            instance.getStartTime(), requestedIds);
        setResourceProperty(resource, INSTANCE_END_TIME_PROPERTY_ID,
            instance.getEndTime(), requestedIds);
        setResourceProperty(resource, INSTANCE_DETAILS_PROPERTY_ID,
            instance.getDetails(), requestedIds);
        setResourceProperty(resource, INSTANCE_LOG_PROPERTY_ID,
            instance.getLog(), requestedIds);

        if (predicate == null || predicate.evaluate(resource)) {
          resources.add(resource);
        }
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

      String desiredStatus = (String) propertyMap.get(INSTANCE_STATUS_PROPERTY_ID);

      if (desiredStatus != null) {
        // get all the instances that pass the predicate check
        Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

        // update all the matching instances with the property values from the request
        for (Resource resource : resources) {
          String status   = (String) resource.getPropertyValue(INSTANCE_STATUS_PROPERTY_ID);
          String feedName = (String) resource.getPropertyValue(INSTANCE_FEED_NAME_PROPERTY_ID);
          String id       = (String) resource.getPropertyValue(INSTANCE_ID_PROPERTY_ID);

          if (desiredStatus.equals("SUSPENDED")) {
            service.suspendInstance(feedName, id);
          } else if (status.equals("SUSPENDED") && desiredStatus.equals("RUNNING")) {
            service.resumeInstance(feedName, id);
          }
        }
      }
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    IvoryService service = getService();

    // get all the instances that pass the predicate check
    Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

    for (Resource resource : resources) {
      // delete all the matching instances with the property values from the request
      service.killInstance((String) resource.getPropertyValue(INSTANCE_FEED_NAME_PROPERTY_ID),
          (String) resource.getPropertyValue(INSTANCE_ID_PROPERTY_ID));
    }
    return new RequestStatusImpl(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- helper methods -----------------------------------------------------

  protected static Instance getInstance(String feedName, String instanceId, Map<String, Object> propertyMap) {
    return new Instance(
        feedName,
        instanceId,
        (String) propertyMap.get(INSTANCE_STATUS_PROPERTY_ID),
        (String) propertyMap.get(INSTANCE_START_TIME_PROPERTY_ID),
        (String) propertyMap.get(INSTANCE_END_TIME_PROPERTY_ID),
        (String) propertyMap.get(INSTANCE_DETAILS_PROPERTY_ID),
        (String) propertyMap.get(INSTANCE_LOG_PROPERTY_ID));
  }

}
