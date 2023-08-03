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

package org.apache.inlong.manager.pojo.source;

import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.stream.StreamField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream source request
 */
@Data
@ApiModel("Stream source request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "sourceType")
public class SourceRequest {

    @ApiModelProperty(value = "Primary key")
    @NotNull(groups = UpdateValidation.class)
    private Integer id;

    @ApiModelProperty("Inlong group id")
    @NotBlank(groups = SaveValidation.class, message = "inlongGroupId cannot be blank")
    @Length(min = 4, max = 200, message = "length must be between 4 and 200")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{4,200}$", message = "only supports letters, numbers, '.', '-', or '_'")
    private String inlongGroupId;

    @ApiModelProperty("Inlong stream id")
    @NotBlank(groups = SaveValidation.class, message = "inlongStreamId cannot be blank")
    @Length(min = 1, max = 200, message = "inlongStreamId length must be between 1 and 200")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{1,200}$", message = "inlongStreamId only supports letters, numbers, '.', '-', or '_'")
    private String inlongStreamId;

    @ApiModelProperty("Source type, including: FILE, KAFKA, etc.")
    @NotBlank(message = "sourceType cannot be blank")
    @Length(min = 1, max = 20, message = "length must be between 1 and 20")
    private String sourceType;

    @ApiModelProperty("Source name, unique in one stream")
    @NotBlank(groups = SaveValidation.class, message = "sourceName cannot be blank")
    @Length(min = 1, max = 100, message = "sourceName length must be between 1 and 100")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{1,100}$", message = "sourceName only supports letters, numbers, '.', '-', or '_'")
    private String sourceName;

    @ApiModelProperty("Ip of the agent running the task")
    @Length(max = 40, message = "length must be less than or equal to 40")
    private String agentIp;

    @ApiModelProperty("Mac uuid of the agent running the task")
    @Length(max = 30, message = "length must be less than or equal to 30")
    private String uuid;

    @ApiModelProperty("Inlong cluster name")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[a-z0-9_.-]{1,128}$", message = "only supports lowercase letters, numbers,'.', '-', or '_'")
    private String inlongClusterName;

    @ApiModelProperty("Inlong cluster node label for filtering stream source collect task")
    @Length(min = 1, max = 128, message = "length must be between 1 and 128")
    @Pattern(regexp = "^[a-z0-9_.-]{1,128}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String inlongClusterNodeGroup;

    @ApiModelProperty("Data node name")
    @Length(max = 128, message = "length must be less than or equal to 128")
    private String dataNodeName;

    @ApiModelProperty("Serialization type, support: csv, json, canal, avro, etc")
    @Length(max = 20, message = "length must be less than or equal to 20")
    private String serializationType;

    @ApiModelProperty("Snapshot of the source task")
    @Length(min = 1, max = 163840, message = "length must be between 1 and 163840")
    private String snapshot;

    @ApiModelProperty("Version")
    @NotNull(groups = UpdateValidation.class, message = "version cannot be null")
    private Integer version;

    @ApiModelProperty("Field list, only support when inlong group in light weight mode")
    private List<StreamField> fieldList;

    @ApiModelProperty("Other properties if needed")
    private Map<String, Object> properties = new LinkedHashMap<>();

    @JsonIgnore
    @ApiModelProperty("Sub source information of existing agents")
    private List<SubSourceDTO> subSourceList;

}
