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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.controller.internal.ViewURLResourceProvider;

import io.swagger.annotations.ApiModelProperty;


public interface ViewUrlResponseSwagger extends ApiModel {

    @ApiModelProperty(name = ViewURLResourceProvider.VIEW_URL_INFO)
    ViewUrlInfo getViewUrlInfo();

    interface ViewUrlInfo {

        @ApiModelProperty(name = ViewURLResourceProvider.URL_NAME_PROPERTY_ID)
        String getUrlName();

        @ApiModelProperty(name = ViewURLResourceProvider.URL_SUFFIX_PROPERTY_ID)
        String getUrlSuffix();

        @ApiModelProperty(name = ViewURLResourceProvider.VIEW_INSTANCE_COMMON_NAME_PROPERTY_ID)
        String getViewInstanceCommonName();

        @ApiModelProperty(name = ViewURLResourceProvider.VIEW_INSTANCE_NAME_PROPERTY_ID)
        String getViewInstanceName();

        @ApiModelProperty(name = ViewURLResourceProvider.VIEW_INSTANCE_VERSION_PROPERTY_ID)
        String getViewInstanceVersion();

    }

}
