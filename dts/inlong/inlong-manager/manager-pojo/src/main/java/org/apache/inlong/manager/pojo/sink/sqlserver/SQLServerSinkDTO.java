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

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

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
    public static SQLServerSinkDTO getFromRequest(SQLServerSinkRequest request, String extParams) {
        SQLServerSinkDTO dto = StringUtils.isNotBlank(extParams)
                ? SQLServerSinkDTO.getFromJson(extParams)
                : new SQLServerSinkDTO();
        return CommonBeanUtils.copyProperties(request, dto, true);
    }

    /**
     * Get the dto instance from json
     */
    public static SQLServerSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, SQLServerSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of SQLServer SinkDTO failure: %s", e.getMessage()));
        }
    }

    /**
     * Get SQLServer table info
     *
     * @param sinkDTO SQLServer sink dto
     * @param columnList SQLServer column info list
     * @return SQLServer table info
     */
    public static SQLServerTableInfo getTableInfo(SQLServerSinkDTO sinkDTO, List<SQLServerColumnInfo> columnList) {
        SQLServerTableInfo tableInfo = new SQLServerTableInfo();
        tableInfo.setTableName(sinkDTO.getTableName());
        tableInfo.setPrimaryKey(sinkDTO.getPrimaryKey());
        tableInfo.setUserName(sinkDTO.getUsername());
        tableInfo.setSchemaName(sinkDTO.getSchemaName());
        tableInfo.setColumns(columnList);
        return tableInfo;
    }
}
