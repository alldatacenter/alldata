/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.views.ViewInstanceService;
import org.apache.ambari.server.controller.internal.ViewInstanceResourceProvider;
import org.apache.ambari.view.ClusterType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request body schema for endpoint {@link ViewInstanceService#createService(String, HttpHeaders, UriInfo, String, String, String)}
 */
public class ViewInstanceRequest implements ApiModel{
  private final ViewInstanceRequestInfo viewInstanceRequestInfo;

  /**
   * @param viewInstanceRequestInfo  {@link ViewInstanceRequestInfo}
   */
  public ViewInstanceRequest(ViewInstanceRequestInfo viewInstanceRequestInfo) {
    this.viewInstanceRequestInfo = viewInstanceRequestInfo;
  }

  /**
   * Returns wrapper class for view instance information
   * @return {@link #viewInstanceRequestInfo}
   */
  @ApiModelProperty(name = ViewInstanceResourceProvider.VIEW_INSTANCE_INFO)
  public ViewInstanceRequestInfo getViewInstanceInfo() {
    return viewInstanceRequestInfo;
  }

  /**
   * static class that wraps all view instance information
   */
  public static class ViewInstanceRequestInfo {
    protected final String viewName;
    protected final String version;
    protected final String instanceName;
    private final String label;
    private final String description;
    private final boolean visible;
    private final String iconPath;
    private final String icon64Path;
    private final Map<String, String> properties;
    private final Map<String, String> instanceData;
    private final Integer clusterHandle;
    private final ClusterType clusterType;

    /**
     *
     * @param viewName        view name
     * @param version         view version
     * @param instanceName    instance name
     * @param label           view label
     * @param description     view description
     * @param visible         visibility for view
     * @param iconPath        icon path
     * @param icon64Path      icon64 path
     * @param properties      properties
     * @param instanceData    instance data
     * @param clusterHandle   cluster handle
     * @param clusterType     cluster type (local|remote|none)
     */
    public ViewInstanceRequestInfo(String viewName, String version, String instanceName, String label, String description,
                                   boolean visible, String iconPath, String icon64Path, Map<String, String> properties,
                                   Map<String, String> instanceData, Integer clusterHandle, ClusterType clusterType) {
      this.viewName = viewName;
      this.version = version;
      this.instanceName = instanceName;
      this.label = label;
      this.description = description;
      this.visible = visible;
      this.iconPath = iconPath;
      this.icon64Path = icon64Path;
      this.properties = properties;
      this.instanceData = instanceData;
      this.clusterHandle = clusterHandle;
      this.clusterType = clusterType;
    }

    /**
     * Returns view name
     * @return view name
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID, hidden = true)
    public String getViewName() {
      return viewName;
    }

    /**
     * Returns view version
     * @return view version
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.VERSION_PROPERTY_ID, hidden = true)
    public String getVersion() {
      return version;
    }

    /**
     * Returns instance name
     * @return instance name
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.INSTANCE_NAME_PROPERTY_ID, hidden = true)
    public String getInstanceName() {
      return instanceName;
    }

    /**
     * Returns view label
     * @return view label
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.LABEL_PROPERTY_ID)
    public String getLabel() {
      return label;
    }

    /**
     * Returns view description
     * @return view description
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.DESCRIPTION_PROPERTY_ID)
    public String getDescription() {
      return description;
    }

    /**
     * Returns visibility for view
     * @return visibility for view
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.VISIBLE_PROPERTY_ID)
    public boolean isVisible() {
      return visible;
    }

    /**
     * Returns icon path
     * @return icon path
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.ICON_PATH_PROPERTY_ID)
    public String getIconPath() {
      return iconPath;
    }

    /**
     * Returns icon64 patch
     * @return icon64 path
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.ICON64_PATH_PROPERTY_ID)
    public String getIcon64Path() {
      return icon64Path;
    }

    /**
     * Returns all view properties
     * @return view properties
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.PROPERTIES_PROPERTY_ID)
    public Map<String, String> getProperties() {
      return properties;
    }

    /**
     * Returns instance data
     * @return instance data
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.INSTANCE_DATA_PROPERTY_ID)
    public Map<String, String> getInstanceData() {
      return instanceData;
    }

    /**
     * Returns cluster handle
     * @return cluster handle
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.CLUSTER_HANDLE_PROPERTY_ID)
    public Integer getClusterHandle() {
      return clusterHandle;
    }

    /**
     * Returns cluster type {@link ClusterType}
     * @return cluster type
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.CLUSTER_TYPE_PROPERTY_ID)
    public ClusterType getClusterType() {
      return clusterType;
    }

  }

}
