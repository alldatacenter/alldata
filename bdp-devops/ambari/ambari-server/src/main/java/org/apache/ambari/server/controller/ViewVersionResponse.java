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

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.views.ViewVersionService;
import org.apache.ambari.server.controller.internal.ViewVersionResourceProvider;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.view.ViewDefinition;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link ViewVersionService#getVersions(String, HttpHeaders, UriInfo, String)}
 */
public class ViewVersionResponse implements ApiModel {

  ViewVersionInfo viewVersionInfo;

  /**
   *
   * @param viewVersionInfo  view version information {@link #viewVersionInfo}
   */
  public ViewVersionResponse(ViewVersionInfo viewVersionInfo) {
    this.viewVersionInfo = viewVersionInfo;
  }

  /**
   * Returns wrapper class instance for view version information
   * @return {@link #viewVersionInfo}
   */
  @ApiModelProperty(name = ViewVersionResourceProvider.VIEW_VERSION_INFO)
  public ViewVersionInfo getViewVersionInfo() {
    return viewVersionInfo;
  }

  /**
   * static wrapper class for view version information
   */
  public static class ViewVersionInfo implements ApiModel {
    private final String archive;
    private final String buildNumber;
    private final boolean clusterConfigurable;
    private final String description;
    private final String label;
    private final String maskerClass;
    private final String maxAmbariVersion;
    private final String minAmbariVersion;
    private final List<ParameterConfig> parameters;
    private final ViewDefinition.ViewStatus status;
    private final String statusDetail;
    private final boolean system;
    private final String version;
    private final String viewName;

    /**
     *
     * @param archive               archive
     * @param buildNumber           build number
     * @param clusterConfigurable   cluster configurable
     * @param description           version description
     * @param label                 version label
     * @param maskerClass           masker class
     * @param maxAmbariVersion      maximum ambari version
     * @param minAmbariVersion      minimum ambari version
     * @param parameters            version parameters
     * @param status                status
     * @param statusDetail          status details
     * @param system                system
     * @param version               version number
     * @param viewName              view name
     */
    public ViewVersionInfo(String archive, String buildNumber, boolean clusterConfigurable, String description,
                           String label, String maskerClass, String maxAmbariVersion, String minAmbariVersion,
                           List<ParameterConfig> parameters, ViewDefinition.ViewStatus status, String statusDetail,
                           boolean system, String version, String viewName) {
      this.archive = archive;
      this.buildNumber = buildNumber;
      this.clusterConfigurable = clusterConfigurable;
      this.description = description;
      this.label = label;
      this.maskerClass = maskerClass;
      this.maxAmbariVersion = maxAmbariVersion;
      this.minAmbariVersion = minAmbariVersion;
      this.parameters = parameters;
      this.status = status;
      this.statusDetail = statusDetail;
      this.system = system;
      this.version = version;
      this.viewName = viewName;
    }

    /**
     * Returns archive string
     * @return  archive
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.ARCHIVE_PROPERTY_ID)
    public String getArchive() {
      return archive;
    }

    /**
     * Returns build number
     * @return build number
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.BUILD_NUMBER_PROPERTY_ID)
    public String getBuildNumber() {
      return buildNumber;
    }

    /**
     * Checks if cluster is configurable
     * @return {@code true} if cluster is configurable
     *         {@code false} otherwise.
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.CLUSTER_CONFIGURABLE_PROPERTY_ID)
    public boolean isClusterConfigurable() {
      return clusterConfigurable;
    }

    /**
     * Returns view description
     * @return view description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Returns view label
     * @return view label
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.LABEL_PROPERTY_ID)
    public String getLabel() {
      return label;
    }

    /**
     * Returns masker class
     * @return masker class
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.MASKER_CLASS_PROPERTY_ID)
    public String getMaskerClass() {
      return maskerClass;
    }

    /**
     * Returns maximum ambari version
     * @return maximum ambari version
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.MAX_AMBARI_VERSION_PROPERTY_ID)
    public String getMaxAmbariVersion() {
      return maxAmbariVersion;
    }

    /**
     * Returns minimum ambari version
     * @return minimum ambari version
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.MIN_AMBARI_VERSION_PROPERTY_ID)
    public String getMinAmbariVersion() {
      return minAmbariVersion;
    }

    /**
     * Returns view parameters
     * @return list of {@link ParameterConfig}
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.PARAMETERS_PROPERTY_ID)
    public List<ParameterConfig> getParameters() {
      return parameters;
    }

    /**
     * Returns view status
     * @return {@link ViewDefinition.ViewStatus}
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.STATUS_PROPERTY_ID)
    public ViewDefinition.ViewStatus getStatus() {
      return status;
    }

    /**
     * Returns views status details
     * @return status details
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.STATUS_DETAIL_PROPERTY_ID)
    public String getStatusDetail() {
      return statusDetail;
    }

    /**
     * Checks if system
     * @return {@code true} if system view
     *         {@code false} otherwise.
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.SYSTEM_PROPERTY_ID)
    public boolean isSystem() {
      return system;
    }

    /**
     * Returns view version
     * @return view version
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.VERSION_PROPERTY_ID)
    public String getVersion() {
      return version;
    }

    /**
     * Returns view name
     * @return view name
     */
    @ApiModelProperty(name = ViewVersionResourceProvider.VIEW_NAME_PROPERTY_ID)
    public String getViewName() {
      return viewName;
    }

  }
}
