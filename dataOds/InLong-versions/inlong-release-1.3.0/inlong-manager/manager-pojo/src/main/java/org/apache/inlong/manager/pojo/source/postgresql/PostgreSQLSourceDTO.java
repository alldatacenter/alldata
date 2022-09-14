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

package org.apache.inlong.manager.pojo.source.postgresql;

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
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostgreSQLSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // thread safe

    @ApiModelProperty("Username of the PostgreSQL server")
    private String username;

    @ApiModelProperty("Password of the PostgreSQL server")
    private String password;

    @ApiModelProperty("Hostname of the PostgreSQL server")
    private String hostname;

    @ApiModelProperty("Port of the PostgreSQL server")
    private Integer port;

    @ApiModelProperty("Database name")
    private String database;

    @ApiModelProperty("Schema name")
    private String schema;

    @ApiModelProperty("Decoding plugin name")
    private String decodingPluginName;

    @ApiModelProperty("List of table name")
    private List<String> tableNameList;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    @ApiModelProperty("Properties for PostgreSQL")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static PostgreSQLSourceDTO getFromRequest(PostgreSQLSourceRequest request) {
        return PostgreSQLSourceDTO.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .hostname(request.getHostname())
                .port(request.getPort())
                .schema(request.getSchema())
                .database(request.getDatabase())
                .tableNameList(request.getTableNameList())
                .primaryKey(request.getPrimaryKey())
                .decodingPluginName(request.getDecodingPluginName())
                .properties(request.getProperties())
                .build();
    }

    public static PostgreSQLSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, PostgreSQLSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
