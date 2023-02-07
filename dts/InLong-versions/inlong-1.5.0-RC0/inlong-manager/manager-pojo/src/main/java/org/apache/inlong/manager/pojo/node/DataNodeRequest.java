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

import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.common.validation.UpdateByKeyValidation;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Data node request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Data node request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
public abstract class DataNodeRequest {

    @ApiModelProperty(value = "Primary key")
    @NotNull(groups = UpdateByIdValidation.class, message = "id cannot be null")
    private Integer id;

    @ApiModelProperty(value = "Data node name")
    @NotBlank(groups = {SaveValidation.class, UpdateByKeyValidation.class}, message = "node name cannot be blank")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,128}$", message = "only supports letters, numbers, '-', or '_'")
    private String name;

    @ApiModelProperty(value = "Data node type, including MYSQL, HIVE, KAFKA, ES, etc.")
    @NotBlank(message = "node type cannot be blank")
    @Length(max = 20, message = "length must be less than or equal to 20")
    private String type;

    @ApiModelProperty(value = "Data node URL")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String url;

    @ApiModelProperty(value = "Data node username")
    @Length(max = 128, message = "length must be less than or equal to 128")
    private String username;

    @ApiModelProperty(value = "Data node token if needed")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String token;

    @ApiModelProperty(value = "Extended params")
    @Length(min = 1, max = 16384, message = "length must be between 1 and 16384")
    private String extParams;

    @ApiModelProperty(value = "Description of the data node")
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String description;

    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    @NotBlank(groups = SaveValidation.class, message = "inCharges cannot be blank")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String inCharges;

    @ApiModelProperty(value = "Version number")
    @NotNull(groups = {UpdateByIdValidation.class, UpdateByKeyValidation.class}, message = "version cannot be null")
    private Integer version;

}
