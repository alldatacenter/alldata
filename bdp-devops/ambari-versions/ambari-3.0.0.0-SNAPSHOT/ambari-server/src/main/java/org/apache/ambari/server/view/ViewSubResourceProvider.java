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

package org.apache.ambari.server.view;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.AbstractResourceProvider;
import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.ReadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An SPI resource provider implementation used to adapt a
 * view resource provider to the SPI interfaces for view
 * sub-resources.
 */
public class ViewSubResourceProvider extends AbstractResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ViewSubResourceProvider.class);

  private static final String VIEW_NAME_PROPERTY_ID     = "view_name";
  private static final String VIEW_VERSION_PROPERTY_ID  = "version";
  private static final String INSTANCE_NAME_PROPERTY_ID = "instance_name";

  private final ViewEntity viewDefinition;
  private final String pkField;
  private final Resource.Type type;
  private final Map<String, PropertyDescriptor> descriptorMap;

  private final Set<String> pkPropertyIds;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view resource provider for the given resource type and bean class.
   *
   * @param type            the resource type
   * @param clazz           the resource bean class
   * @param pkField         the primary key field name
   * @param viewDefinition  the associated view definition
   *
   * @throws IntrospectionException if an exception occurs during introspection of the resource bean class
   */
  public ViewSubResourceProvider(Resource.Type type, Class<?> clazz, String pkField, ViewEntity viewDefinition)
      throws IntrospectionException {

    super(discoverPropertyIds(clazz), getKeyPropertyIds(pkField, type));
    this.pkField        = pkField;
    this.viewDefinition = viewDefinition;
    this.pkPropertyIds  = new HashSet<>(getKeyPropertyIds().values());
    this.type           = type;
    this.descriptorMap  = getDescriptorMap(clazz);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    Set<Map<String, Object>> properties = request.getProperties();

    for (Map<String, Object> propertyMap : properties) {
      String resourceId   = (String) propertyMap.get(pkField);
      String instanceName = (String) propertyMap.get(INSTANCE_NAME_PROPERTY_ID);

      try {
        getResourceProvider(instanceName).createResource(resourceId, propertyMap);
      } catch (org.apache.ambari.view.NoSuchResourceException e) {
        throw new NoSuchParentResourceException(e.getMessage(), e);
      } catch (org.apache.ambari.view.UnsupportedPropertyException e) {
        throw new UnsupportedPropertyException(getResourceType(e), e.getPropertyIds());
      } catch (org.apache.ambari.view.ResourceAlreadyExistsException e) {
        throw new ResourceAlreadyExistsException(e.getMessage());
      } catch (Exception e) {
        LOG.error("Caught exception creating view sub resources.", e);
        throw new SystemException(e.getMessage(), e);
      }
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<ViewInstanceEntity> instanceDefinitions = new HashSet<>();

    try {

      Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
      int size = propertyMaps.size();

      Collection<ViewInstanceEntity> viewInstanceDefinitions = viewDefinition.getInstances();
      if (size == 0) {
        instanceDefinitions.addAll(viewInstanceDefinitions);
      } else {
        for (Map<String, Object> propertyMap : propertyMaps) {
          String instanceName = (String) propertyMap.get(INSTANCE_NAME_PROPERTY_ID);
          if (size == 1 && instanceName != null) {
            String resourceId = (String) propertyMap.get(pkField);
            if (resourceId != null) {
              Object bean = getResourceProvider(instanceName).getResource(resourceId, requestedIds);
              return Collections.singleton(getResource(bean, viewDefinition.getCommonName(),
                  viewDefinition.getVersion(), instanceName, requestedIds));
            }
          }
          if (instanceName == null) {
            instanceDefinitions.addAll(viewInstanceDefinitions);
            break;
          } else {
            ViewInstanceEntity instanceDefinition = viewDefinition.getInstanceDefinition(instanceName);
            if (instanceDefinition != null) {
              instanceDefinitions.add(instanceDefinition);
            }
          }
        }
      }

      Set<Resource> results = new HashSet<>();
      ReadRequest readRequest = new ViewReadRequest(request, requestedIds, predicate == null ? "" : predicate.toString());
      for (ViewInstanceEntity instanceDefinition : instanceDefinitions) {

        Set<?> beans = instanceDefinition.getResourceProvider(type).getResources(readRequest);

        for (Object bean : beans) {
          Resource resource = getResource(bean, viewDefinition.getCommonName(),
              viewDefinition.getVersion(), instanceDefinition.getName(), requestedIds);
          if (predicate.evaluate(resource)) {
            results.add(resource);
          }
        }
      }
      return results;
    } catch (org.apache.ambari.view.NoSuchResourceException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (org.apache.ambari.view.UnsupportedPropertyException e) {
      throw new UnsupportedPropertyException(getResourceType(e), e.getPropertyIds());
    } catch (Exception e) {
      LOG.error("Caught exception getting view sub resources.", e);
      throw new SystemException(e.getMessage(), e);
    }
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();

    if (iterator.hasNext()) {
      Map<String,Object> propertyMap = iterator.next();
      Set<Resource>      resources   = getResources(request, predicate);

      for (Resource resource : resources) {
        String resourceId   = (String) resource.getPropertyValue(pkField);
        String instanceName = (String) resource.getPropertyValue(INSTANCE_NAME_PROPERTY_ID);

        try {
          getResourceProvider(instanceName).updateResource(resourceId, propertyMap);
        } catch (org.apache.ambari.view.NoSuchResourceException e) {
          throw new NoSuchParentResourceException(e.getMessage(), e);
        } catch (org.apache.ambari.view.UnsupportedPropertyException e) {
          throw new UnsupportedPropertyException(getResourceType(e), e.getPropertyIds());
        } catch (Exception e) {
          LOG.error("Caught exception updating view sub resources.", e);
          throw new SystemException(e.getMessage(), e);
        }
      }
    }
    return new RequestStatusImpl(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);
    for (Resource resource : resources) {
      String resourceId   = (String) resource.getPropertyValue(pkField);
      String instanceName = (String) resource.getPropertyValue(INSTANCE_NAME_PROPERTY_ID);

      try {
        getResourceProvider(instanceName).deleteResource(resourceId);
      } catch (org.apache.ambari.view.NoSuchResourceException e) {
        throw new NoSuchParentResourceException(e.getMessage(), e);
      } catch (org.apache.ambari.view.UnsupportedPropertyException e) {
        throw new UnsupportedPropertyException(getResourceType(e), e.getPropertyIds());
      } catch (Exception e) {
        LOG.error("Caught exception deleting view sub resources.", e);
        throw new SystemException(e.getMessage(), e);
      }
    }
    return new RequestStatusImpl(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- helper methods ----------------------------------------------------

  // get a Resource from the bean
  private Resource getResource(Object bean, String viewName, String viewVersion,
                               String instanceName, Set<String> requestedIds)
      throws InvocationTargetException, IllegalAccessException {

    Resource resource = new ResourceImpl(type);

    resource.setProperty(VIEW_NAME_PROPERTY_ID, viewName);
    resource.setProperty(VIEW_VERSION_PROPERTY_ID, viewVersion);
    resource.setProperty(INSTANCE_NAME_PROPERTY_ID, instanceName);

    for (Map.Entry<String, PropertyDescriptor> entry : descriptorMap.entrySet()) {
      Object value = entry.getValue().getReadMethod().invoke(bean);

      setResourceProperty(resource, entry.getKey(), value, requestedIds);
    }
    return resource;
  }

  // get the resource provider associated with the given instance name for this resource type
  private org.apache.ambari.view.ResourceProvider<?> getResourceProvider(String instanceName) {
    return viewDefinition.getInstanceDefinition(instanceName).getResourceProvider(type);
  }

  // get the resource type associated with the given UnsupportedPropertyException
  private Resource.Type getResourceType(org.apache.ambari.view.UnsupportedPropertyException e) {
    return Resource.Type.valueOf(e.getType());
  }

  // discover the property ids for the given bean class
  private static Set<String> discoverPropertyIds(Class<?> clazz) throws IntrospectionException {
    Set<String> propertyIds = new HashSet<>(getDescriptorMap(clazz).keySet());
    propertyIds.add(INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_VERSION_PROPERTY_ID);

    return propertyIds;
  }

  // get a descriptor map for the given bean class
  private static Map<String, PropertyDescriptor> getDescriptorMap(Class<?> clazz) throws IntrospectionException {
    Map<String, PropertyDescriptor> descriptorMap = new HashMap<>();

    for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
      String name = pd.getName();
      if (pd.getReadMethod() != null && !name.equals("class")) {
        descriptorMap.put(name, pd);
      }
    }
    return descriptorMap;
  }

  // get the key property ids for the resource
  private static Map<Resource.Type, String> getKeyPropertyIds(String pkField, Resource.Type type) {

    Map<Resource.Type, String> keyPropertyIds = new HashMap<>();

    keyPropertyIds.put(Resource.Type.View, VIEW_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewVersion, VIEW_VERSION_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewInstance, INSTANCE_NAME_PROPERTY_ID);
    keyPropertyIds.put(type, pkField);

    return keyPropertyIds;
  }


  // ----- inner class : ViewReadRequest -------------------------------------

  /**
   * A read request to pass the the view resource provider.  Serves
   * as a bridge from the ambari-server API framework request to the
   * view framework request.
   */
  private static class ViewReadRequest implements ReadRequest {

    /**
     * The original request.
     */
    private final Request request;

    /**
     * The property ids of the request.
     */
    private final Set<String> propertyIds;

    /**
     * The request predicate.
     */
    private final String predicate;


    // ----- Constructors ----------------------------------------------------

    /**
     * Construct a view read request.
     *
     * @param propertyIds  the property ids
     * @param predicate    the predicate
     */
    private ViewReadRequest(Request request, Set<String> propertyIds, String predicate) {
      this.request     = request;
      this.propertyIds = propertyIds;
      this.predicate   = predicate;
    }


    // ----- ReadRequest -----------------------------------------------------

    @Override
    public Set<String> getPropertyIds() {
      return propertyIds;
    }

    @Override
    public String getPredicate() {
      return predicate;
    }

    @Override
    public TemporalInfo getTemporalInfo(String id) {

      org.apache.ambari.server.controller.spi.TemporalInfo temporalInfo =
          request.getTemporalInfo(id);

      return temporalInfo == null ? null : new ViewReadRequestTemporalInfo(temporalInfo);
    }
  }

  /**
   * Temporal information to pass to the view resource provider.  Serves as a
   * bridge from the ambari-server API framework temporal info object to the
   * view framework temporal info object.
   */
  private static class ViewReadRequestTemporalInfo implements ReadRequest.TemporalInfo {

    /**
     * The original temporal information object.
     */
    private final org.apache.ambari.server.controller.spi.TemporalInfo temporalInfo;


    // ----- Constructors ----------------------------------------------------

    private ViewReadRequestTemporalInfo(TemporalInfo temporalInfo) {
      this.temporalInfo = temporalInfo;
    }


    // ----- TemporalInfo ----------------------------------------------------

    @Override
    public Long getStartTime() {
      return temporalInfo.getStartTime();
    }

    @Override
    public Long getEndTime() {
      return temporalInfo.getEndTime();
    }

    @Override
    public Long getStep() {
      return temporalInfo.getStep();
    }
  }
}
