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

package org.apache.inlong.manager.pojo.cluster;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.validation.UpdateValidation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Inlong cluster node request
 */
@Data
@ApiModel("Cluster node request")
public class ClusterNodeRequest {

    @NotNull(groups = UpdateValidation.class)
    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @NotNull(message = "parentId cannot be null")
    @ApiModelProperty(value = "ID of the parent cluster")
    private Integer parentId;

    @NotBlank(message = "type cannot be blank")
    @ApiModelProperty(value = "Cluster type, including AGENT, DATAPROXY, etc.")
    private String type;

    @NotBlank(message = "ip cannot be blank")
    @ApiModelProperty(value = "Cluster IP")
    private String ip;

    @NotNull(message = "port cannot be null")
    @ApiModelProperty(value = "Cluster port")
    private Integer port;

    @ApiModelProperty(value = "Extended params")
    private String extParams;

    @ApiModelProperty(value = "Description of the cluster node")
    private String description;

    @ApiModelProperty(value = "Version number")
    private Integer version;

}
