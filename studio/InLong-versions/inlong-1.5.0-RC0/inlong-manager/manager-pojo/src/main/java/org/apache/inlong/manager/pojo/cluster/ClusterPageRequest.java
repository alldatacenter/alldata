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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.common.PageRequest;

import java.util.List;

/**
 * Inlong cluster paging query conditions
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong cluster paging query request")
public class ClusterPageRequest extends PageRequest {

    @ApiModelProperty(value = "Cluster type, including TUBEMQ, PULSAR, DATAPROXY, etc.")
    private String type;

    @ApiModelProperty(value = "Cluster type list")
    private List<String> typeList;

    @ApiModelProperty(value = "Cluster name")
    private String name;

    @ApiModelProperty(value = "Parent cluster ID, used for cluster node")
    private Integer parentId;

    @ApiModelProperty(value = "Keywords, name, url, cluster tag, etc.")
    private String keyword;

    @ApiModelProperty(value = "Cluster tag")
    private String clusterTag;

    @ApiModelProperty(value = "Extend tag")
    private String extTag;

    @ApiModelProperty(value = "Protocol type, such as: TCP, HTTP")
    private String protocolType;

    @ApiModelProperty(value = "The inlong cluster tag list")
    private List<String> clusterTagList;

    @ApiModelProperty(value = "Status")
    private Integer status;

    @ApiModelProperty(value = "Current user", hidden = true)
    private String currentUser;

}
