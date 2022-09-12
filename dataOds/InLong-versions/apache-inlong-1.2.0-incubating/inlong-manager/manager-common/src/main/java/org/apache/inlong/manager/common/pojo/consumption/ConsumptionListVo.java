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

package org.apache.inlong.manager.common.pojo.consumption;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Data consumption list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Data consumption list")
public class ConsumptionListVo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Consumer Group")
    private String consumerGroup;

    @ApiModelProperty(value = "Person in charge of consumption")
    private String inCharges;

    @ApiModelProperty(value = "Consumption target inlong group id")
    private String inlongGroupId;

    @ApiModelProperty(value = "MQ type, high throughput: TUBE, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "Consumption target TOPIC")
    private String topic;

    @ApiModelProperty(value = "Status: Pending distribution: 10,"
            + " Pending approval: 11, Approval rejected: 20, Approval passed: 21")
    private Integer status;

    @ApiModelProperty(value = "Recent consumption time")
    private Date lastConsumptionTime;

    @ApiModelProperty(value = "Consumption status: normal: 0, abnormal: 1, shielded: 2, no: 3,")
    private Integer lastConsumptionStatus;
}
