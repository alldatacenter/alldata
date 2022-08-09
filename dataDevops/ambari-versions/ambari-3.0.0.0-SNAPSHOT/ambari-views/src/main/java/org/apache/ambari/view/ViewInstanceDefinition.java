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

import java.util.Map;

/**
 * The view instance definition.
 */
public interface ViewInstanceDefinition {

  /**
   * Get the name of this instance.
   *
   * @return the instance name
   */
  public String getInstanceName();

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName();

  /**
   * Get the view instance label (display name).
   *
   * @return the view instance label
   */
  public String getLabel();

  /**
   * Get the view instance description.
   *
   * @return the description
   */
  public String getDescription();

  /**
   * Get the Id of cluster associated with this view instance.  For a local cluster reference,
   * the cluster handle is simply the unique cluster id.
   *
   * @return the associated cluster handle; <code>null</code> if no cluster is associated
   */
  public Long getClusterHandle();

  /**
   *  Get the type of cluster the view instance is attached to
   *
   * @return clusterType the type of cluster for cluster handle
   */
  public ClusterType getClusterType();


  /**
   * Indicates whether or not the view instance should be visible.
   *
   * @return true if the view instance should be visible; false otherwise
   */
  public boolean isVisible();

  /**
   * Get the instance property map.
   *
   * @return the map of instance properties
   */
  public Map<String, String> getPropertyMap();

  /**
   * Get the view instance application data.
   *
   * @return the view instance application data map
   */
  public Map<String, String> getInstanceDataMap();

  /**
   * Get the associated view definition.
   *
   * @return the view definition
   */
  public ViewDefinition getViewDefinition();

}
