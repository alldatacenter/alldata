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

package org.apache.inlong.manager.pojo.cluster.dataproxy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;

/**
 * DataProxy cluster info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("DataProxy cluster info")
public class DataProxyClusterDTO {

    @ApiModelProperty("Is the DataProxy cluster an intranet? 0: no, 1: yes")
    private Integer isIntranet = 1;

    @ApiModelProperty("Is the DataProxy cluster in a switch status? 0: no, 1: yes")
    private Integer isSwitch = 0;

    @ApiModelProperty("Load of the DataProxy cluster, default is 20")
    private Integer load = 20;

    /**
     * Get the dto instance from the request
     */
    public static DataProxyClusterDTO getFromRequest(DataProxyClusterRequest request) {
        return DataProxyClusterDTO.builder()
                .isIntranet(request.getIsIntranet())
                .isSwitch(request.getIsSwitch())
                .load(request.getLoad())
                .build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static DataProxyClusterDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, DataProxyClusterDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
