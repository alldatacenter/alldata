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

package org.apache.inlong.manager.pojo.node.iceberg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * Iceberg data node info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Iceberg data node info")
public class IcebergDataNodeDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergDataNodeDTO.class);

    @ApiModelProperty("Catalog type, like: HIVE, HADOOP, default is HIVE")
    @Builder.Default
    private String catalogType = "HIVE";

    @ApiModelProperty("Iceberg data warehouse dir")
    private String warehouse;

    /**
     * Get the dto instance from the request
     */
    public static IcebergDataNodeDTO getFromRequest(IcebergDataNodeRequest request) throws Exception {
        return IcebergDataNodeDTO.builder()
                .catalogType(request.getCatalogType())
                .warehouse(request.getWarehouse())
                .build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static IcebergDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, IcebergDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("Failed to parse extParams for Iceberg node: %s", e.getMessage()));
        }
    }

}
