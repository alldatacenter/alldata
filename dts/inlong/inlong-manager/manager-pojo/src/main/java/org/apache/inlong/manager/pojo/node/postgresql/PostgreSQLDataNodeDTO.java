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

package org.apache.inlong.manager.pojo.node.postgresql;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;

/**
 * PostgreSQL data node info
 */
@Data
@Builder
@ApiModel("PostgreSQL data node info")
public class PostgreSQLDataNodeDTO {

    private static final String POSTGRESQL_JDBC_PREFIX = "jdbc:postgresql://";

    /**
     * Get the dto instance from the request
     */
    public static PostgreSQLDataNodeDTO getFromRequest(PostgreSQLDataNodeRequest request) throws Exception {
        return PostgreSQLDataNodeDTO.builder().build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static PostgreSQLDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, PostgreSQLDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("Failed to parse extParams for PostgreSQL node: %s", e.getMessage()));
        }
    }

    /**
     * Convert ip:post to jdbcUrl.
     */
    public static String convertToJdbcUrl(String url) {
        String jdbcUrl = url;
        if (StringUtils.isNotBlank(jdbcUrl) && !jdbcUrl.startsWith(POSTGRESQL_JDBC_PREFIX)) {
            jdbcUrl = POSTGRESQL_JDBC_PREFIX + jdbcUrl;
        }
        return jdbcUrl;
    }
}
