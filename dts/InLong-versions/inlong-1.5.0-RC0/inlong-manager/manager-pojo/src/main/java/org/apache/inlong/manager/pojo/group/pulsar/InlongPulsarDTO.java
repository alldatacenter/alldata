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

package org.apache.inlong.manager.pojo.group.pulsar;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.group.BaseInlongGroup;

import javax.validation.constraints.NotNull;

/**
 * Inlong group info for Pulsar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("Inlong group info for Pulsar")
public class InlongPulsarDTO extends BaseInlongGroup {

    @ApiModelProperty(value = "Pulsar tenant")
    private String tenant;

    @ApiModelProperty(value = "Queue model, parallel: multiple partitions, high throughput, out-of-order messages;"
            + "serial: single partition, low throughput, and orderly messages")
    private String queueModule;

    @ApiModelProperty(value = "Number of partitions of Topic, 1-20")
    private Integer partitionNum;

    @ApiModelProperty(value = "Ledger's number of writable nodes")
    private Integer ensemble;

    @ApiModelProperty(value = "Ledger's number of copies")
    private Integer writeQuorum;

    @ApiModelProperty(value = "Number of responses requested")
    private Integer ackQuorum;

    @ApiModelProperty(value = "Message time-to-live duration")
    private Integer ttl;

    @ApiModelProperty(value = "The unit of message's time-to-live duration")
    private String ttlUnit;

    @ApiModelProperty(value = "Message storage time")
    private Integer retentionTime;

    @ApiModelProperty(value = "The unit of the message storage time")
    private String retentionTimeUnit;

    @ApiModelProperty(value = "Message size")
    private Integer retentionSize;

    @ApiModelProperty(value = "The unit of message size")
    private String retentionSizeUnit;

    /**
     * Get the dto instance from the request
     */
    public static InlongPulsarDTO getFromRequest(InlongPulsarRequest request) {
        return CommonBeanUtils.copyProperties(request, InlongPulsarDTO::new, true);
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static InlongPulsarDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, InlongPulsarDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("parse extParams of Pulsar failure: %s", e.getMessage()));
        }
    }

}
