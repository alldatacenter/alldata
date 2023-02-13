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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.validation.UpdateValidation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Base inlong consume request
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Base inlong consume request")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "mqType")
public abstract class InlongConsumeRequest extends BaseInlongConsume {

    @NotNull(groups = UpdateValidation.class)
    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @NotBlank(message = "consumerGroup cannot be null")
    @ApiModelProperty(value = "Consumer group, only support [a-zA-Z0-9_]")
    private String consumerGroup;

    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "The target topic of this consume")
    private String topic;

    @NotBlank(message = "inlong group id cannot be null")
    @ApiModelProperty(value = "The target inlong group id of this consume")
    private String inlongGroupId;

    @ApiModelProperty(value = "Whether to filter consumption, 0-not filter, 1-filter")
    private Integer filterEnabled = 0;

    @ApiModelProperty(value = "The target inlong stream id of this consume, needed if the filterEnabled=1")
    private String inlongStreamId;

    @NotBlank(message = "inCharges cannot be null")
    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    private String inCharges;

    @ApiModelProperty(value = "Version number")
    private Integer version;

}
