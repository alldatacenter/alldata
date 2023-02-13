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

package org.apache.inlong.manager.pojo.sink.sqlserver;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * SQLServer source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SQLServerSinkDTO {

    @ApiModelProperty("Username of the SQLServer")
    private String username;

    @ApiModelProperty("Password of the SQLServer")
    private String password;

    @ApiModelProperty("SQLServer meta db URL, etc jdbc:sqlserver://host:port;databaseName=database")
    private String jdbcUrl;

    @ApiModelProperty("Schema name of the SQLServer")
    private String schemaName;

    @ApiModelProperty("Table name of the SQLServer")
    private String tableName;

    @ApiModelProperty("Database time zone, Default is UTC")
    private String serverTimezone;

    @ApiModelProperty("Whether to migrate all databases")
    private boolean allMigration;

    @ApiModelProperty(value = "Primary key must be shared by all tables")
    private String primaryKey;

    /**
     * Get the dto instance from the request
     */
    public static SQLServerSinkDTO getFromRequest(SQLServerSinkRequest request) {
        return SQLServerSinkDTO.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .jdbcUrl(request.getJdbcUrl())
                .schemaName(request.getSchemaName())
                .tableName(request.getTableName())
                .serverTimezone(request.getServerTimezone())
                .allMigration(request.isAllMigration())
                .primaryKey(request.getPrimaryKey())
                .build();
    }

    /**
     * Get the dto instance from json
     */
    public static SQLServerSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, SQLServerSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Get SqlServer table info
     *
     * @param sqlServerSink SqlServer sink dto,{@link OracleSinkDTO}
     * @param columnList SqlServer column info list,{@link OracleColumnInfo}
     * @return {@link SQLServerTableInfo}
     */
    public static SQLServerTableInfo getTableInfo(SQLServerSinkDTO sqlServerSink,
            List<SQLServerColumnInfo> columnList) {
        SQLServerTableInfo tableInfo = new SQLServerTableInfo();
        tableInfo.setTableName(sqlServerSink.getTableName());
        tableInfo.setPrimaryKey(sqlServerSink.getPrimaryKey());
        tableInfo.setUserName(sqlServerSink.getUsername());
        tableInfo.setSchemaName(sqlServerSink.getSchemaName());
        tableInfo.setColumns(columnList);
        return tableInfo;
    }
}
