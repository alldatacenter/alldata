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

package org.apache.inlong.manager.pojo.node;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.validation.UpdateValidation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Data node request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Data node  request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
public abstract class DataNodeRequest {

    @NotNull(groups = UpdateValidation.class)
    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @NotBlank(message = "node name cannot be blank")
    @ApiModelProperty(value = "Data node name")
    private String name;

    @NotBlank(message = "node type cannot be blank")
    @ApiModelProperty(value = "Data node type, including MYSQL, HIVE, KAFKA, ES, etc.")
    private String type;

    @ApiModelProperty(value = "Data node URL")
    private String url;

    @ApiModelProperty(value = "Data node username")
    private String username;

    @ApiModelProperty(value = "Data node token if needed")
    private String token;

    @ApiModelProperty(value = "Extended params")
    private String extParams;

    @ApiModelProperty(value = "Description of the data node")
    private String description;

    @NotBlank(message = "inCharges cannot be blank")
    @ApiModelProperty(value = "Name of responsible person, separated by commas", required = true)
    private String inCharges;

    @ApiModelProperty(value = "Version number")
    private Integer version;

}
