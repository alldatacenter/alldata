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

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.AlertGroupResourceProvider;
import org.apache.ambari.server.controller.internal.AlertTargetResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request / response schema for AlertTarget API Swagger documentation generation. The interface only serves documentation
 * generation purposes, it is not meant to be implemented.
 */
public interface AlertTargetSwagger extends ApiModel {

    @ApiModelProperty(name = AlertTargetResourceProvider.ALERT_TARGET)
    AlertTargetInfo getAlertTargetInfo();

    interface AlertTargetInfo {

        @ApiModelProperty(name = AlertTargetResourceProvider.ID_PROPERTY_ID)
        Long getId();

        @ApiModelProperty(name = AlertTargetResourceProvider.NAME_PROPERTY_ID)
        String getName();

        @ApiModelProperty(name = AlertTargetResourceProvider.DESCRIPTION_PROPERTY_ID)
        String getDescription();

        @ApiModelProperty(name = AlertTargetResourceProvider.NOTIFICATION_TYPE_PROPERTY_ID)
        String getNotificationType();

        @ApiModelProperty(name = AlertTargetResourceProvider.ENABLED_PROPERTY_ID)
        Boolean isEnabled();

        @ApiModelProperty(name = AlertTargetResourceProvider.PROPERTIES_PROPERTY_ID)
        Map<String, String> getProperties();

        // it should be Set<AlertState> but Swagger doesn't support list of enums
        @ApiModelProperty(name = AlertTargetResourceProvider.STATES_PROPERTY_ID)
        List<String> getAlertStates();

        @ApiModelProperty(name = AlertTargetResourceProvider.GLOBAL_PROPERTY_ID)
        Boolean isGlobal();

        @ApiModelProperty(name = AlertTargetResourceProvider.GROUPS_PROPERTY_ID)
        List<AlertGroup> getAlertGroups();

        interface AlertGroup {

            @ApiModelProperty(name = AlertGroupResourceProvider.ID_PROPERTY_ID)
            Long getId();

            @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_ID)
            Long getClusterId();

            @ApiModelProperty(name = AlertGroupResourceProvider.NAME_PROPERTY_ID)
            String getGroupName();

            @ApiModelProperty(name = AlertGroupResourceProvider.DEFAULT_PROPERTY_ID)
            Boolean isDefault();
        }
    }
}
