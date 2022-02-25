/**
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

package org.apache.ambari.view;

import org.apache.ambari.view.cluster.Cluster;

import java.util.Collection;
import java.util.Map;

/**
 * Context object available to the view components to provide access to
 * the view and instance attributes as well as run time information about
 * the current execution context.
 */
public interface ViewContext {

  /**
   * Key for mapping a view context as a property.
   */
  public static final String CONTEXT_ATTRIBUTE = "ambari-view-context";

  /**
   * Get the current user name after auth_to_local conversion
   *
   * @return the current user name
   */
  public String getUsername();

  /**
   * Get the current ambari user.
   *
   * @return the current user name
   */
  public String getLoggedinUser();

  /**
   * Determine whether or not the access specified by the given permission name
   * is permitted for the given user.
   *
   * @param userName        the user name
   * @param permissionName  the permission name
   *
   * @throws SecurityException if the access specified by the given permission name
   *         is not permitted
   */
  public void hasPermission(String userName, String permissionName) throws SecurityException;

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName();

  /**
   * Get the view definition associated with this context.
   *
   * @return the view definition
   */
  public ViewDefinition getViewDefinition();

  /**
   * Get the view instance name.
   *
   * @return the view instance name; null if no instance is associated
   */
  public String getInstanceName();

  /**
   * Get the view instance definition associated with this context.
   *
   * @return the view instance definition; null if no instance is associated
   */
  public ViewInstanceDefinition getViewInstanceDefinition();

  /**
   * Get the property values specified to create the view instance.
   *
   * @return the view instance property values; null if no instance is associated
   */
  public Map<String, String> getProperties();

  /**
   * Save an instance data value for the given key.
   *
   * @param key    the key
   * @param value  the value
   *
   * @throws IllegalStateException    if no instance is associated
   * @throws IllegalArgumentException if updating the view instance fails validation
   */
  public void putInstanceData(String key, String value);

  /**
   * Get the instance data value for the given key.
   *
   * @param key  the key
   *
   * @return the instance data value; null if no instance is associated
   */
  public String getInstanceData(String key);

  /**
   * Get the instance data values.
   *
   * @return the view instance property values; null if no instance is associated
   */
  public Map<String, String> getInstanceData();

  /**
   * Remove the instance data value for the given key.
   *
   * @param key  the key
   *
   * @throws IllegalStateException if no instance is associated
   */
  public void removeInstanceData(String key);

  /**
   * Get a property for the given key from the ambari configuration.
   *
   * @param key  the property key
   *
   * @return the property value; null indicates that the configuration contains no mapping for the key
   */
  public String getAmbariProperty(String key);

  /**
   * Get the view resource provider for the given resource type.
   *
   * @param type  the resource type
   *
   * @return the resource provider; null if no instance is associated
   */
  public ResourceProvider<?> getResourceProvider(String type);

  /**
   * Get a URL stream provider.
   *
   * @return a stream provider
   */
  public URLStreamProvider getURLStreamProvider();

  /**
   * Get a URL connection provider.
   *
   * @return a connection provider
   */
  public URLConnectionProvider getURLConnectionProvider();

  /**
   * Get an Ambari stream provider.
   *
   * @return an Ambari stream provider
   */
  public AmbariStreamProvider getAmbariStreamProvider();

  /**
   * Get Ambari stream provider attached as cluster to the view
   * If view is not attached to ambari managed cluster then it will be null
   *
   * @return stream provider for the cluster
   */
  public AmbariStreamProvider getAmbariClusterStreamProvider();

  /**
   * Get a data store for view persistence entities.
   *
   * @return a data store; null if no instance is associated
   */
  public DataStore getDataStore();

  /**
   * Get all of the available view definitions.
   *
   * @return the view definitions
   */
  public Collection<ViewDefinition> getViewDefinitions();

  /**
   * Get all of the available view instance definitions.
   *
   * @return the view instance definitions
   */
  public Collection<ViewInstanceDefinition> getViewInstanceDefinitions();

  /**
   * Get a view controller associated with this context.
   *
   * @return the view controller
   */
  public ViewController getController();

  /**
   * Get the HTTP Impersonator.
   *
   * @return the HTTP Impersonator, which internally uses the App Cookie Manager
   *
   * @deprecated  As of release 2.0
   */
  @Deprecated
  public HttpImpersonator getHttpImpersonator();

  /**
   * Get the default settings for the Impersonator.
   *
   * @return the Impersonator settings.
   *
   * @deprecated  As of release 2.0
   */
  @Deprecated
  public ImpersonatorSetting getImpersonatorSetting();

  /**
   * Get the cluster associated with this view instance.
   *
   * @return the associated cluster; <code>null</code> if no cluster is associated
   */
  public Cluster getCluster();
}
