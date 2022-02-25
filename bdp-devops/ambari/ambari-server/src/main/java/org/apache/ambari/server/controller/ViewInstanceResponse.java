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
import org.apache.ambari.view.validation.ValidationResult;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link ViewInstanceService#getServices(String, HttpHeaders, UriInfo, String, String)}
 */
public class ViewInstanceResponse implements ApiModel{
  private final ViewInstanceResponseInfo viewInstanceResponseInfo;

  public ViewInstanceResponse(ViewInstanceResponseInfo viewInstanceResponseInfo) {
    this.viewInstanceResponseInfo = viewInstanceResponseInfo;
  }

  @ApiModelProperty(name = ViewInstanceResourceProvider.VIEW_INSTANCE_INFO)
  public ViewInstanceResponseInfo getViewInstanceInfo() {
    return viewInstanceResponseInfo;
  }

  public class ViewInstanceResponseInfo extends ViewInstanceRequest.ViewInstanceRequestInfo {
    private final String contextPath;
    private final boolean staticDriven;
    private String shortUrl;
    private String shortUrlName;
    private ValidationResult validationResult;
    private Map<String, ValidationResult> propertyValidationResults;

    /**
     *
     * @param viewName        view name
     * @param version         view version
     * @param instanceName    view instance name
     * @param label           view label
     * @param description     view description
     * @param visible         visible
     * @param iconPath        icon path
     * @param icon64Path      icon64 path
     * @param properties      view properties
     * @param instanceData    view instance data
     * @param clusterHandle   cluster handle
     * @param clusterType     cluster type (local|remote|none)
     * @param contextPath     context path
     * @param staticDriven    is static driven
     */
    public ViewInstanceResponseInfo(String viewName, String version, String instanceName, String label, String description,
                                    boolean visible, String iconPath, String icon64Path, Map<String, String> properties,
                                    Map<String, String> instanceData, Integer clusterHandle, ClusterType clusterType,
                                    String contextPath, boolean staticDriven) {
      super(viewName, version, instanceName, label, description, visible, iconPath, icon64Path, properties, instanceData, clusterHandle, clusterType);
      this.contextPath = contextPath;
      this.staticDriven = staticDriven;
    }

    /**
     * Return view name.
     * @return view name
     */
    @Override
    @ApiModelProperty(name = ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID)
    public String getViewName() {
      return viewName;
    }

    /**
     * Return view version
     * @return view version
     */
    @Override
    @ApiModelProperty(name = ViewInstanceResourceProvider.VERSION_PROPERTY_ID)
    public String getVersion() {
      return version;
    }

    /**
     * Return view instance name
     * @return view instance name
     */
    @Override
    @ApiModelProperty(name = ViewInstanceResourceProvider.INSTANCE_NAME_PROPERTY_ID)
    public String getInstanceName() {
      return instanceName;
    }

    /**
     * Return view context path
     * @return context path
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.CONTEXT_PATH_PROPERTY_ID)
    public String getContextPath() {
      return contextPath;
    }

    /**
     * Return if the view is static driven
     * @return  {{@link #staticDriven}
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.STATIC_PROPERTY_ID)
    public boolean isStaticDriven() {
      return staticDriven;
    }

    /**
     * Return short url for the view
     * @return short url for the view
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.SHORT_URL_PROPERTY_ID)
    public String getShortUrl() {
      return shortUrl;
    }

    /**
     * set short url for the view
     * @param shortUrl  short url
     */
    public void setShortUrl(String shortUrl) {
      this.shortUrl = shortUrl;
    }

    /**
     * Returns short url name
     * @return short url name
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.SHORT_URL_NAME_PROPERTY_ID)
    public String getShortUrlName() {
      return shortUrlName;
    }

    /**
     * Sets short url name
     * @param shortUrlName short url name
     */
    public void setShortUrlName(String shortUrlName) {
      this.shortUrlName = shortUrlName;
    }

    /**
     * Returns validation result
     * @return {@link ValidationResult}
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.VALIDATION_RESULT_PROPERTY_ID)
    public ValidationResult getValidationResult() {
      return validationResult;
    }

    /**
     * sets validation result
     * @param validationResult {@link ValidationResult}
     */
    public void setValidationResult(ValidationResult validationResult) {
      this.validationResult = validationResult;
    }

    /**
     * Returns map of property->ValidationResult
     * @return Map
     */
    @ApiModelProperty(name = ViewInstanceResourceProvider.PROPERTY_VALIDATION_RESULTS_PROPERTY_ID)
    public Map<String, ValidationResult> getPropertyValidationResults() {
      return propertyValidationResults;
    }

    /**
     * Sets map of property->ValidationResult
     * @param propertyValidationResults  Map
     */
    public void setPropertyValidationResults(Map<String, ValidationResult> propertyValidationResults) {
      this.propertyValidationResults = propertyValidationResults;
    }

  }
}
