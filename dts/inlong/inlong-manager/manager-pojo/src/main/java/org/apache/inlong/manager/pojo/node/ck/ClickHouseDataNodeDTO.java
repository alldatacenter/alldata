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

package org.apache.inlong.manager.pojo.node.ck;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;

/**
 * ClickHouse data node info
 */
@Data
@Builder
@ApiModel("ClickHouse data node info")
public class ClickHouseDataNodeDTO {

    private static final String CLICKHOUSE_JDBC_PREFIX = "jdbc:clickhouse://";

    /**
     * Get the dto instance from the request
     */
    public static ClickHouseDataNodeDTO getFromRequest(ClickHouseDataNodeRequest request) throws Exception {
        return ClickHouseDataNodeDTO.builder().build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static ClickHouseDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, ClickHouseDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("Failed to parse extParams for ClickHouse node: %s", e.getMessage()));
        }
    }

    /**
     * Convert ip:post to jdbcUrl.
     */
    public static String convertToJdbcUrl(String url) {
        String jdbcUrl = url;
        if (StringUtils.isNotBlank(jdbcUrl) && !jdbcUrl.startsWith(CLICKHOUSE_JDBC_PREFIX)) {
            jdbcUrl = CLICKHOUSE_JDBC_PREFIX + jdbcUrl;
        }
        return jdbcUrl;
    }

}
