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

package org.apache.inlong.manager.pojo.consume;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;

import java.util.Date;
import java.util.List;

/**
 * Base inlong consume info
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Base inlong consume info")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "mqType")
public abstract class InlongConsumeInfo extends BaseInlongConsume {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Consumer group, only support [a-zA-Z0-9_]")
    private String consumerGroup;

    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "The target topic of this consume")
    private String topic;

    @ApiModelProperty(value = "The target inlong group id of this consume")
    private String inlongGroupId;

    @ApiModelProperty(value = "Whether to filter consumption, 0-not filter, 1-filter")
    private Integer filterEnabled = 0;

    @ApiModelProperty(value = "The target inlong stream id of this consume, needed if the filterEnabled=1")
    private String inlongStreamId;

    @ApiModelProperty(value = "Cluster URL of message queue")
    private String clusterUrl;

    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    private String inCharges;

    @ApiModelProperty(value = "Consume status")
    private Integer status;

    @ApiModelProperty(value = "Previous status")
    private Integer previousStatus;

    @ApiModelProperty(value = "Name of creator")
    private String creator;

    @ApiModelProperty(value = "Name of modifier")
    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @ApiModelProperty(value = "Version number")
    private Integer version;

    @ApiModelProperty(value = "MQ cluster info list")
    private List<? extends ClusterInfo> clusterInfos;

    public abstract InlongConsumeRequest genRequest();

}
