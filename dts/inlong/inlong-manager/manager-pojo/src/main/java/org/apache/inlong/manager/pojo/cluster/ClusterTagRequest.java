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

import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateValidation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Inlong cluster tag request
 */
@Data
@ApiModel("Cluster tag request")
public class ClusterTagRequest {

    @ApiModelProperty(value = "Primary key")
    @NotNull(groups = UpdateValidation.class, message = "id cannot be null")
    private Integer id;

    @ApiModelProperty(value = "Cluster tag")
    @NotBlank(groups = SaveValidation.class, message = "clusterTag cannot be blank")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[a-z0-9_.-]{1,128}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String clusterTag;

    @ApiModelProperty(value = "Extended params")
    @Length(min = 1, max = 163840, message = "length must be between 1 and 163840")
    private String extParams;

    @ApiModelProperty(value = "Description of the cluster tag")
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String description;

    @ApiModelProperty(value = "Inlong tenant of cluster tag", hidden = true)
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String tenant;

    @ApiModelProperty(value = "Name of in charges, separated by commas")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String inCharges;

    @ApiModelProperty(value = "Version number")
    @NotNull(groups = UpdateValidation.class, message = "version cannot be null")
    private Integer version;

}
