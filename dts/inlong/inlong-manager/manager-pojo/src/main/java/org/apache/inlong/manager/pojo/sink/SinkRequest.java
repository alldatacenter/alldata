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

package org.apache.inlong.manager.pojo.sink;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.common.validation.UpdateByKeyValidation;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

/**
 * Stream sink request
 */
@Data
@ApiModel("Stream sink request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "sinkType")
public abstract class SinkRequest {

    @ApiModelProperty(value = "Primary key")
    @NotNull(groups = UpdateByIdValidation.class, message = "id cannot be null")
    private Integer id;

    @ApiModelProperty("Inlong group id")
    @NotBlank(groups = {SaveValidation.class, UpdateByKeyValidation.class}, message = "inlongGroupId cannot be blank")
    @Length(min = 4, max = 100, message = "length must be between 4 and 100")
    @Pattern(regexp = "^[a-z0-9_.-]{4,100}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String inlongGroupId;

    @ApiModelProperty("Inlong stream id")
    @NotBlank(groups = {SaveValidation.class, UpdateByKeyValidation.class}, message = "inlongStreamId cannot be blank")
    @Length(min = 1, max = 100, message = "inlongStreamId length must be between 1 and 100")
    @Pattern(regexp = "^[a-z0-9_.-]{1,100}$", message = "inlongStreamId only supports lowercase letters, numbers, '-', or '_'")
    private String inlongStreamId;

    @ApiModelProperty("Sink type, including: HIVE, ES, etc.")
    @NotBlank(message = "sinkType cannot be blank")
    @Length(max = 15, message = "length must be less than or equal to 15")
    private String sinkType;

    @ApiModelProperty("Sink name, unique in one stream")
    @NotBlank(groups = {SaveValidation.class, UpdateByKeyValidation.class}, message = "sinkName cannot be blank")
    @Length(min = 1, max = 100, message = "sinkName length must be between 1 and 100")
    @Pattern(regexp = "^[a-z0-9_-]{1,100}$", message = "sinkName only supports lowercase letters, numbers, '-', or '_'")
    private String sinkName;

    @ApiModelProperty("Sink description")
    @Length(max = 500, message = "length must be less than or equal to 500")
    private String description;

    @ApiModelProperty("Inlong cluster name")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[a-z0-9_.-]{1,128}$", message = "only supports lowercase letters, numbers, '.', '-', or '_'")
    private String inlongClusterName;

    @ApiModelProperty("Data node name")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,128}$", message = "only supports letters, numbers, '-', or '_'")
    private String dataNodeName;

    @ApiModelProperty("Sort task name")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String sortTaskName;

    @ApiModelProperty("Sort consumer group")
    @Length(max = 512, message = "length must be less than or equal to 512")
    private String sortConsumerGroup;

    @ApiModelProperty(value = "Whether to enable create sink resource? 0: disable, 1: enable. Default is 1")
    @Range(min = 0, max = 1, message = "default is 1, only supports [0: disable, 1: enable]")
    private Integer enableCreateResource = 1;

    @ApiModelProperty(value = "Whether to start the process after saving or updating. Default is false")
    private Boolean startProcess = false;

    @ApiModelProperty("Sink field list")
    private List<SinkField> sinkFieldList;

    @ApiModelProperty("Other properties if needed")
    private Map<String, Object> properties = Maps.newHashMap();

    @ApiModelProperty(value = "Version number")
    @NotNull(groups = {UpdateByIdValidation.class, UpdateByKeyValidation.class}, message = "version cannot be null")
    private Integer version;

}
