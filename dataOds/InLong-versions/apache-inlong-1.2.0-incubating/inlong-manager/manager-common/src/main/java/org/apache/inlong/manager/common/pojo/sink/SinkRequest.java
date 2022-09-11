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

package org.apache.inlong.manager.common.pojo.sink;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request of sink
 */
@Data
@ApiModel("Request of sink")
@JsonTypeInfo(use = Id.NAME, visible = true, property = "sinkType")
public class SinkRequest {

    @ApiModelProperty("Sink id")
    private Integer id;

    @NotNull(message = "inlongGroupId cannot be null")
    @ApiModelProperty("Inlong group id")
    private String inlongGroupId;

    @NotNull(message = "inlongStreamId cannot be null")
    @ApiModelProperty("Inlong stream id")
    private String inlongStreamId;

    @NotNull(message = "sinkType cannot be null")
    @ApiModelProperty("Sink type, including: HIVE, ES, etc.")
    private String sinkType;

    @NotNull(message = "sinkName cannot be null")
    @ApiModelProperty("Sink name, unique in one stream")
    private String sinkName;

    @ApiModelProperty("Sink description")
    private String description;

    @ApiModelProperty("Inlong cluster name")
    private String inlongClusterName;

    @ApiModelProperty("Data node name")
    private String dataNodeName;

    @ApiModelProperty("Sort task name")
    private String sortTaskName;

    @ApiModelProperty("Sort consumer group")
    private String sortConsumerGroup;

    @ApiModelProperty(value = "Whether to enable create sink resource? 0: disable, 1: enable. Default is 1")
    private Integer enableCreateResource = 1;

    @ApiModelProperty("Sink field list")
    private List<SinkField> sinkFieldList;

    @ApiModelProperty("Other properties if needed")
    private Map<String, Object> properties = Maps.newHashMap();

}
