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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * Inlong cluster info
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong cluster info")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
public abstract class ClusterInfo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Cluster name")
    private String name;

    @ApiModelProperty(value = "Cluster type, including TUBEMQ, PULSAR, KAFKA, DATAPROXY, etc.")
    private String type;

    @ApiModelProperty(value = "Cluster url")
    private String url;

    @ApiModelProperty(value = "Cluster tags, separated by commas")
    private String clusterTags;

    @ApiModelProperty(value = "Extension tag")
    private String extTag;

    @ApiModelProperty(value = "Cluster token")
    private String token;

    @ApiModelProperty(value = "Cluster heartbeat info")
    private String heartbeat;

    @ApiModelProperty(value = "Extended params")
    private String extParams;

    @ApiModelProperty(value = "Description of the cluster")
    private String description;

    @ApiModelProperty(value = "Name of in charges, separated by commas")
    private String inCharges;

    @ApiModelProperty(value = "Cluster status")
    private Integer status;

    @ApiModelProperty(value = "Name of in creator")
    private String creator;

    @ApiModelProperty(value = "Name of in modifier")
    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @ApiModelProperty(value = "Version number")
    private Integer version;

    public abstract ClusterRequest genRequest();

}
