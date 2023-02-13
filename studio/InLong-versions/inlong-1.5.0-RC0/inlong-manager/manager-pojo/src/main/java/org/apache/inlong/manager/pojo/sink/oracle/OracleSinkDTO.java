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

package org.apache.inlong.manager.pojo.sink.oracle;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Oracle sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OracleSinkDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleSinkDTO.class);

    @ApiModelProperty("Oracle JDBC URL,Such as jdbc:oracle:thin@host:port:sid "
            + "or jdbc:oracle:thin@host:port/service_name")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
    private String password;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Properties for Oracle")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static OracleSinkDTO getFromRequest(OracleSinkRequest request) {
        return OracleSinkDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .tableName(request.getTableName())
                .username(request.getUsername())
                .password(request.getPassword())
                .primaryKey(request.getPrimaryKey())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get Oracle sink info from JSON string
     */
    public static OracleSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, OracleSinkDTO.class);
        } catch (Exception e) {
            LOGGER.error("fetch oracle sink info failed from json params: " + extParams, e);
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Get Oracle table info
     *
     * @param oracleSink Oracle sink dto,{@link OracleSinkDTO}
     * @param columnList Oracle column info list,{@link OracleColumnInfo}
     * @return {@link OracleTableInfo}
     */
    public static OracleTableInfo getTableInfo(OracleSinkDTO oracleSink, List<OracleColumnInfo> columnList) {
        OracleTableInfo tableInfo = new OracleTableInfo();
        tableInfo.setTableName(oracleSink.getTableName());
        tableInfo.setPrimaryKey(oracleSink.getPrimaryKey());
        tableInfo.setUserName(oracleSink.getUsername());
        tableInfo.setColumns(columnList);
        return tableInfo;
    }
}
