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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.auth.Authentication;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;

import java.util.Date;
import java.util.List;

/**
 * Inlong group info
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group info")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "mqType")
public abstract class InlongGroupInfo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong group name")
    private String name;

    @ApiModelProperty(value = "Inlong group description")
    private String description;

    @Deprecated
    @ApiModelProperty(value = "MQ type, replaced by mqType")
    private String middlewareType;

    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "MQ resource",
            notes = "in inlong group, TubeMQ corresponds to Topic, Pulsar corresponds to Namespace")
    private String mqResource;

    @ApiModelProperty(value = "Whether to enable zookeeper? 0: disable, 1: enable")
    @Builder.Default
    private Integer enableZookeeper = 0;

    @ApiModelProperty(value = "Whether to enable create resource? 0: disable, 1: enable")
    private Integer enableCreateResource;

    @ApiModelProperty(value = "Whether to use lightweight mode, 0: no, 1: yes")
    private Integer lightweight;

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

    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    private String inCharges;

    @ApiModelProperty(value = "Name of followers, separated by commas")
    private String followers;

    @ApiModelProperty(value = "Status")
    private Integer status;

    @ApiModelProperty(value = "Name of creator")
    private String creator;

    @ApiModelProperty(value = "Name of modifier")
    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @ApiModelProperty(value = "Inlong group Extension properties")
    private List<InlongGroupExtInfo> extList;

    @JsonIgnore
    @ApiModelProperty("Authentication info, will transfer into extList")
    private Authentication authentication;

    @ApiModelProperty("Sort configuration, will transfer into extList")
    private BaseSortConf sortConf;

    @ApiModelProperty(value = "Version number")
    private Integer version;

    public abstract InlongGroupRequest genRequest();

}
