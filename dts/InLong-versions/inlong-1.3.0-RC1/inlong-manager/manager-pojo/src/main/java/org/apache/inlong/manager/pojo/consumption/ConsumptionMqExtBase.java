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

package org.apache.inlong.manager.pojo.consumption;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Extended consumption information of different MQs
 */
@Data
@ApiModel("Extended consumption information of different MQs")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "mqType",
        defaultImpl = ConsumptionMqExtBase.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConsumptionPulsarInfo.class, name = "PULSAR"),
        @JsonSubTypes.Type(value = ConsumptionPulsarInfo.class, name = "TDMQ_PULSAR")
})
public class ConsumptionMqExtBase {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Consumption ID")
    private Integer consumptionId;

    @ApiModelProperty(value = "Consumer group")
    private String consumerGroup;

    @ApiModelProperty(value = "Consumption target inlong group id")
    private String inlongGroupId;

    @ApiModelProperty("Whether to delete, 0: not deleted, 1: deleted")
    private Integer isDeleted = 0;

    @ApiModelProperty("The type of MQ")
    private String mqType;
}
