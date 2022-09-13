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

package org.apache.inlong.manager.pojo.group;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * Inlong group request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group create request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "mqType")
public abstract class InlongGroupRequest {

    @NotBlank(message = "inlongGroupId cannot be blank")
    @ApiModelProperty(value = "Inlong group id", required = true)
    @Length(min = 4, max = 100, message = "inlongGroupId length must be between 4 and 100")
    @Pattern(regexp = "^[a-z0-9_-]{4,100}$",
            message = "inlongGroupId only supports lowercase letters, numbers, '-', or '_'")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong group name", required = true)
    private String name;

    @ApiModelProperty(value = "Inlong group description")
    private String description;

    @Deprecated
    @ApiModelProperty(value = "MQ type, replaced by mqType")
    private String middlewareType;

    @NotBlank(message = "mqType cannot be blank")
    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "MQ resource",
            notes = "in inlong group, TubeMQ corresponds to Topic, Pulsar corresponds to Namespace")
    private String mqResource;

    @ApiModelProperty(value = "TubeMQ master URL")
    private String tubeMaster;

    @ApiModelProperty(value = "Whether to enable zookeeper? 0: disable, 1: enable")
    private Integer enableZookeeper = 0;

    @ApiModelProperty(value = "Whether to enable create resource? 0: disable, 1: enable")
    private Integer enableCreateResource = 1;

    @ApiModelProperty(value = "Whether to use lightweight mode, 0: no, 1: yes")
    private Integer lightweight = 0;

    @ApiModelProperty(value = "Inlong cluster tag, which links to inlong_cluster table")
    private String inlongClusterTag;

    @ApiModelProperty(value = "Number of access items per day, unit: 10,000 items per day")
    private Integer dailyRecords;

    @ApiModelProperty(value = "Access size per day, unit: GB per day")
    private Integer dailyStorage;

    @ApiModelProperty(value = "peak access per second, unit: bars per second")
    private Integer peakRecords;

    @ApiModelProperty(value = "The maximum length of a single piece of data, unit: Byte")
    private Integer maxLength;

    @NotBlank(message = "inCharges cannot be blank")
    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    private String inCharges;

    @ApiModelProperty(value = "Name of followers, separated by commas")
    private String followers;

    @ApiModelProperty(value = "Name of creator")
    private String creator;

    @ApiModelProperty(value = "Inlong group Extension properties")
    private List<InlongGroupExtInfo> extList;

    @ApiModelProperty(value = "Version number")
    private Integer version;

}
