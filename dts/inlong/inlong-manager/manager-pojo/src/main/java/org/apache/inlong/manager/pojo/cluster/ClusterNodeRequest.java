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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Inlong cluster node request
 */
@Data
@ApiModel("Cluster node request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type", defaultImpl = ClusterNodeRequest.class)
public class ClusterNodeRequest {

    @ApiModelProperty(value = "Primary key")
    @NotNull(groups = UpdateValidation.class, message = "id cannot be null")
    private Integer id;

    @ApiModelProperty(value = "ID of the parent cluster")
    @NotNull(message = "parentId cannot be null")
    private Integer parentId;

    @ApiModelProperty(value = "Cluster type, including AGENT, DATAPROXY, etc.")
    @NotBlank(message = "type cannot be blank")
    @Length(min = 1, max = 20, message = "length must be between 1 and 20")
    private String type;

    @ApiModelProperty(value = "Cluster IP")
    @NotBlank(message = "ip cannot be blank")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String ip;

    @ApiModelProperty(value = "Cluster port")
    @NotNull(message = "port cannot be null")
    private Integer port;

    @ApiModelProperty(value = "Cluster protocol type")
    @Length(min = 1, max = 20, message = "length must be less than or equal to 20")
    private String protocolType;

    @ApiModelProperty(value = "Current load value of the node")
    private Integer nodeLoad;

    @ApiModelProperty(value = "Extended params")
    @Length(min = 1, max = 163840, message = "length must be between 1 and 163840")
    private String extParams;

    @ApiModelProperty(value = "Description of the cluster node")
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String description;

    @ApiModelProperty(value = "Version number")
    @NotNull(groups = UpdateValidation.class, message = "version cannot be null")
    private Integer version;

}
