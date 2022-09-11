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

package org.apache.inlong.manager.common.pojo.group;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Inlong group approval info
 */
@Data
@ApiModel("Inlong group approval request")
public class InlongGroupApproveRequest {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "MQ Type")
    private String mqType;

    @ApiModelProperty(value = "MQ resource, for Tube, it is Topic, for Pulsar, it is Namespace")
    private String mqResource;

    @ApiModelProperty(value = "Inlong cluster tag, inlong group will be associated with the cluster")
    private String inlongClusterTag;

    @ApiModelProperty(value = "The partition num of Pulsar topic, between 1-20")
    private Integer topicPartitionNum;

    @ApiModelProperty(value = "Ledger's number of writable nodes")
    private Integer ensemble;

    @ApiModelProperty(value = "Ledger's number of copies")
    private Integer writeQuorum;

    @ApiModelProperty(value = "Number of responses requested")
    private Integer ackQuorum;

    @ApiModelProperty(value = "Message storage time")
    private Integer retentionTime;

    @ApiModelProperty(value = "The unit of the message storage time")
    private String retentionTimeUnit;

    @ApiModelProperty(value = "Message time-to-live duration")
    private Integer ttl;

    @ApiModelProperty(value = "The unit of message's time-to-live duration")
    private String ttlUnit;

    @ApiModelProperty(value = "Message size")
    private Integer retentionSize;

    @ApiModelProperty(value = "The unit of message size")
    private String retentionSizeUnit;

}
