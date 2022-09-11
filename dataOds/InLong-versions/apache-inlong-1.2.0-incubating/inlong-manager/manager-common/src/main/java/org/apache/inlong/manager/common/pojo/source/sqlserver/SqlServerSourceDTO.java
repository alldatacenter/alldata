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

package org.apache.inlong.manager.common.pojo.source.sqlserver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;

/**
 * Sqlserver source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlServerSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Username of the SqlServer")
    private String username;

    @ApiModelProperty("Password of the SqlServer")
    private String password;

    @ApiModelProperty("Hostname of the SqlServer")
    private String hostname;

    @ApiModelProperty("Exposed port of the SqlServer")
    private int port;

    @ApiModelProperty("Database of the SqlServer")
    private String database;

    @ApiModelProperty("Schema name of the SqlServer")
    private String schemaName;

    @ApiModelProperty("Table name of the SqlServer")
    private String tableName;

    @ApiModelProperty("Database time zone, default is UTC")
    private String serverTimezone;

    @ApiModelProperty("Whether to migrate all databases")
    private boolean allMigration;

    @ApiModelProperty(value = "Primary key must be shared by all tables")
    private String primaryKey;

    /**
     * Get the dto instance from the request
     */
    public static SqlServerSourceDTO getFromRequest(SqlServerSourceRequest request) {
        return SqlServerSourceDTO.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .hostname(request.getHostname())
                .port(request.getPort())
                .database(request.getDatabase())
                .schemaName(request.getSchemaName())
                .tableName(request.getTableName())
                .serverTimezone(request.getServerTimezone())
                .allMigration(request.isAllMigration())
                .primaryKey(request.getPrimaryKey())
                .build();
    }

    /**
     * Get the dto instance from the JSON string
     */
    public static SqlServerSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, SqlServerSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage());
        }
    }

}
