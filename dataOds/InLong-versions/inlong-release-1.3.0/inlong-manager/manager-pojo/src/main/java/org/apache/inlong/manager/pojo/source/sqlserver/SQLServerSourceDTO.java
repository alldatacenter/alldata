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

package org.apache.inlong.manager.pojo.source.sqlserver;

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
import java.util.Map;

/**
 * SQLServer source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SQLServerSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Username of the SQLServer server")
    private String username;

    @ApiModelProperty("Password of the SQLServer server")
    private String password;

    @ApiModelProperty("Hostname of the SQLServer server")
    private String hostname;

    @ApiModelProperty("Port of the SQLServer server")
    private Integer port;

    @ApiModelProperty("Database name")
    private String database;

    @ApiModelProperty("Schema name")
    private String schemaName;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("Database time zone, default is UTC")
    private String serverTimezone;

    @ApiModelProperty("Whether to migrate all databases")
    private boolean allMigration;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    @ApiModelProperty("Properties for SQLServer")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static SQLServerSourceDTO getFromRequest(SQLServerSourceRequest request) {
        return SQLServerSourceDTO.builder()
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
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from the JSON string
     */
    public static SQLServerSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, SQLServerSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
