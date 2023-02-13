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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.pojo.stream.StreamNode;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream source info, including source name, agent ip, etc.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "sourceType")
@ApiModel("Stream source info")
public abstract class StreamSource extends StreamNode {

    @ApiModelProperty("Source id")
    private Integer id;

    @ApiModelProperty("Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty("Inlong stream id")
    private String inlongStreamId;

    @ApiModelProperty("Source type, including: FILE, KAFKA, etc.")
    private String sourceType;

    @ApiModelProperty("Source name, unique in one stream")
    private String sourceName;

    @ApiModelProperty("Ip of the agent running the task")
    private String agentIp;

    @ApiModelProperty("Mac uuid of the agent running the task")
    private String uuid;

    @ApiModelProperty("Inlong cluster name")
    private String inlongClusterName;

    @ApiModelProperty("Inlong cluster node tag")
    private String inlongClusterNodeTag;

    @ApiModelProperty("Data node name")
    private String dataNodeName;

    @ApiModelProperty("Data Serialization, support: csv, json, canal, avro, etc")
    private String serializationType;

    @ApiModelProperty("Snapshot of this source task")
    private String snapshot;

    @ApiModelProperty("Version")
    private Integer version;

    @ApiModelProperty("Status")
    private Integer status;

    @ApiModelProperty("Previous status")
    private Integer previousStatus;

    @ApiModelProperty("Creator")
    private String creator;

    @ApiModelProperty("Modifier")
    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @Builder.Default
    @ApiModelProperty("Properties for source")
    private Map<String, Object> properties = new LinkedHashMap<>();

    @ApiModelProperty("Null if not a sub source")
    private Integer templateId;

    @ApiModelProperty("Sub source information of existing agents")
    private List<SubSourceDTO> subSourceList;

    @ApiModelProperty(value = "Whether to ignore the parse errors of field value, true as default")
    private boolean ignoreParseError;

    public SourceRequest genSourceRequest() {
        return null;
    }

}
