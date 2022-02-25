/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.services;

import java.util.Map;

import org.apache.ambari.server.controller.ApiModel;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request data model for {@link org.apache.ambari.server.api.services.RootServiceComponentConfigurationService}
 */
public interface RootServiceComponentConfigurationRequestSwagger extends ApiModel {

  @ApiModelProperty(name = "Configuration")
  RootServiceComponentConfigurationRequestInfo getRootServiceComponentConfigurationRequestInfo();

  interface RootServiceComponentConfigurationRequestInfo {
    @ApiModelProperty
    String getServiceName();

    @ApiModelProperty
    String getComponentName();

    @ApiModelProperty
    String getCategoryName();

    @ApiModelProperty
    Map<String, String> getProperties();
  }
}
