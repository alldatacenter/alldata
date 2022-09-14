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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.stream.StreamField;
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

    @NotNull(groups = UpdateValidation.class)
    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @NotBlank(message = "inlongGroupId cannot be blank")
    @ApiModelProperty("Inlong group id")
    private String inlongGroupId;

    @NotBlank(message = "inlongStreamId cannot be blank")
    @ApiModelProperty("Inlong stream id")
    private String inlongStreamId;

    @NotBlank(message = "sourceType cannot be blank")
    @ApiModelProperty("Source type, including: FILE, KAFKA, etc.")
    private String sourceType;

    @NotBlank(message = "sourceName cannot be blank")
    @Length(min = 1, max = 100, message = "sourceName length must be between 1 and 100")
    @Pattern(regexp = "^[a-z0-9_-]{1,100}$",
            message = "sourceName only supports lowercase letters, numbers, '-', or '_'")
    @ApiModelProperty("Source name, unique in one stream")
    private String sourceName;

    @ApiModelProperty("Ip of the agent running the task")
    private String agentIp;

    @ApiModelProperty("Mac uuid of the agent running the task")
    private String uuid;

    @ApiModelProperty("Inlong cluster name")
    private String inlongClusterName;

    @ApiModelProperty("Data node name")
    private String dataNodeName;

    @ApiModelProperty("Serialization type, support: csv, json, canal, avro, etc")
    private String serializationType;

    @ApiModelProperty("Snapshot of the source task")
    private String snapshot;

    @ApiModelProperty("Version")
    private Integer version;

    @ApiModelProperty("Field list, only support when inlong group in light weight mode")
    private List<StreamField> fieldList;

    @ApiModelProperty("Other properties if needed")
    private Map<String, Object> properties = new LinkedHashMap<>();

    @JsonIgnore
    @ApiModelProperty("Sub source information of existing agents")
    private List<SubSourceDTO> subSourceList;

}
