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

package org.apache.inlong.manager.pojo.node.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * MySQL data node info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("MySQL data node info")
public class MySQLDataNodeDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLDataNodeDTO.class);
    private static final String MYSQL_JDBC_PREFIX = "jdbc:mysql://";

    @ApiModelProperty("URL of backup DB server")
    private String backupUrl;

    /**
     * Get the dto instance from the request
     */
    public static MySQLDataNodeDTO getFromRequest(MySQLDataNodeRequest request) throws Exception {
        return MySQLDataNodeDTO.builder()
                .backupUrl(request.getBackupUrl())
                .build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static MySQLDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, MySQLDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT,
                    String.format("Failed to parse extParams for MySQL node: %s", e.getMessage()));
        }
    }

    /**
     * Convert ip:post to jdbcurl.
     */
    public static String convertToJdbcurl(String url) {
        String jdbcUrl = url;
        if (StringUtils.isNotBlank(jdbcUrl) && !jdbcUrl.startsWith(MYSQL_JDBC_PREFIX)) {
            jdbcUrl = MYSQL_JDBC_PREFIX + jdbcUrl;
        }
        return MySQLSinkDTO.filterSensitive(jdbcUrl);
    }
}
