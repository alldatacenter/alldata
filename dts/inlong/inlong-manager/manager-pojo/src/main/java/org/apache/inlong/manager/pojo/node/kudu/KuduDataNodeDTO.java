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

package org.apache.inlong.manager.pojo.node.kudu;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

/**
 * Kudu data node info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Kudu data node info")
public class KuduDataNodeDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(KuduDataNodeDTO.class);

    @ApiModelProperty("Kudu masters, a comma separated list of 'host:port' pairs")
    private String masters;

    @ApiModelProperty("Sets the default timeout used for administrative operations (e.g. createTable, deleteTable, etc). Optional. If not provided, defaults to 30s. A value of 0 disables the timeout")
    private Integer defaultAdminOperationTimeoutMs;

    @ApiModelProperty("Sets the default timeout used for user operations (using sessions and scanners). Optional. If not provided, defaults to 30s. A value of 0 disables the timeout")
    private Integer defaultOperationTimeoutMs = 30000;

    @ApiModelProperty("Default socket read timeout in ms, default is 10000")
    private Integer defaultSocketReadTimeoutMs = 10000;

    @ApiModelProperty("Disable this client's collection of statistics. Statistics are enabled by default")
    private Boolean statisticsDisabled = false;

    /**
     * Get the dto instance from the request
     */
    public static KuduDataNodeDTO getFromRequest(KuduDataNodeRequest request) {
        return CommonBeanUtils.copyProperties(request, KuduDataNodeDTO::new, true);
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static KuduDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, KuduDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("Failed to parse extParams for Kudu node: %s", e.getMessage()));
        }
    }

}
