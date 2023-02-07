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
 * Inlong consume query request
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong consume query request")
public class InlongConsumePageRequest extends PageRequest {

    @ApiModelProperty(value = "Keyword, can be consumer group")
    private String keyword;

    @ApiModelProperty(value = "Consumer group name")
    private String consumerGroup;

    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "The target topic of inlong consume")
    private String topic;

    @ApiModelProperty(value = "The target inlongGroupId of inlong consume")
    private String inlongGroupId;

    @ApiModelProperty(value = "Consume status")
    private Integer status;

    @ApiModelProperty(value = "Consume status list")
    private List<Integer> statusList;

    @ApiModelProperty(value = "Current user", hidden = true)
    private String currentUser;

    @ApiModelProperty(value = "Whether the current user is in the administrator role", hidden = true)
    private Boolean isAdminRole;

}
