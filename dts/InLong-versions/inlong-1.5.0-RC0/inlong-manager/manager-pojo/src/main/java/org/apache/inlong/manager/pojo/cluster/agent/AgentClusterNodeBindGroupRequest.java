/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.cluster.agent;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Inlong cluster node bind or unbind group.Group is used to distinguish which stream source tasks are collected
 */
@Data
@ApiModel("Cluster node bind and unbind stream source label request, stream source label is a filter to judge "
        + "whether to accept the stream source task")
public class AgentClusterNodeBindGroupRequest {

    @NotBlank(message = "Cluster agent group cannot be blank")
    @ApiModelProperty(value = "Cluster agent group")
    private String agentGroup;

    @NotBlank(message = "clusterName cannot be blank")
    @ApiModelProperty(value = "Cluster name")
    private String clusterName;

    @ApiModelProperty(value = "Cluster node ip list which needs to bind tag")
    private List<String> bindClusterNodes;

    @ApiModelProperty(value = "Cluster node ip list which needs to unbind tag")
    private List<String> unbindClusterNodes;
}
