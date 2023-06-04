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

package org.apache.inlong.manager.pojo.sink.greenplum;

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
import java.util.Map;

/**
 * Greenplum sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreenplumSinkDTO {

    @ApiModelProperty("JDBC URL of Greenplum server, such as: jdbc:postgresql://host:port/database")
    private String jdbcUrl;

    @ApiModelProperty("Username of Greenplum server")
    private String username;

    @ApiModelProperty("User password of Greenplum server")
    private String password;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Properties for greenplum")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static GreenplumSinkDTO getFromRequest(GreenplumSinkRequest request) {
        return GreenplumSinkDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .username(request.getUsername())
                .password(request.getPassword())
                .primaryKey(request.getPrimaryKey())
                .tableName(request.getTableName())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from the json
     */
    public static GreenplumSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, GreenplumSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of Greenplum SinkDTO failure: %s", e.getMessage()));
        }
    }

    /**
     * Get Greenplum table info
     *
     * @param greenplumSink Greenplum sink dto,{@link GreenplumSinkDTO}
     * @param columnList Greenplum column info list,{@link GreenplumColumnInfo}
     * @return {@link GreenplumTableInfo}
     */
    public static GreenplumTableInfo getTableInfo(GreenplumSinkDTO greenplumSink,
            List<GreenplumColumnInfo> columnList) {
        GreenplumTableInfo tableInfo = new GreenplumTableInfo();
        tableInfo.setTableName(greenplumSink.getTableName());
        tableInfo.setPrimaryKey(greenplumSink.getPrimaryKey());
        tableInfo.setUserName(greenplumSink.getUsername());
        tableInfo.setColumns(columnList);
        return tableInfo;
    }
}
