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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.source.StreamSource;

import java.util.Date;
import java.util.List;

/**
 * Inlong group brief info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group brief info")
public class InlongGroupBriefInfo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong group name")
    private String name;

    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "MQ resource")
    private String mqResource;

    @ApiModelProperty(value = "Inlong cluster tag, which links to inlong_cluster table")
    private String inlongClusterTag;

    @ApiModelProperty(value = "Inlong cluster tag, which links to inlong_cluster table")
    private String extParams;

    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    private String inCharges;

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

    @ApiModelProperty(value = "Stream sources in the inlong group")
    private List<StreamSource> streamSources;

}
