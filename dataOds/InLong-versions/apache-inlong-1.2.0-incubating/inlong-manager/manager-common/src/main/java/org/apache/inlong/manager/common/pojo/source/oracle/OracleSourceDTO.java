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

package org.apache.inlong.manager.common.pojo.source.oracle;

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
 * Oracle source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OracleSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Hostname of the DB server, for example: 127.0.0.1")
    private String hostname;

    @ApiModelProperty("Exposed port of the DB server")
    private Integer port = 1521;

    @ApiModelProperty("Username of the DB server")
    private String username;

    @ApiModelProperty("Password of the DB server")
    private String password;

    @ApiModelProperty("Database name")
    private String database;

    @ApiModelProperty("Schema name")
    private String schemaName;

    @ApiModelProperty("table name")
    private String tableName;

    @ApiModelProperty("Scan startup mode")
    private String scanStartupMode;

    @ApiModelProperty(value = "Primary key must be shared by all tables")
    private String primaryKey;

    /**
     * Get the dto instance from the request
     */
    public static OracleSourceDTO getFromRequest(OracleSourceRequest request) {
        return OracleSourceDTO.builder()
                .database(request.getDatabase())
                .hostname(request.getHostname())
                .port(request.getPort())
                .username(request.getUsername())
                .password(request.getPassword())
                .schemaName(request.getSchemaName())
                .tableName(request.getTableName())
                .primaryKey(request.getPrimaryKey())
                .scanStartupMode(request.getScanStartupMode())
                .build();
    }

    public static OracleSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, OracleSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage());
        }
    }

}
