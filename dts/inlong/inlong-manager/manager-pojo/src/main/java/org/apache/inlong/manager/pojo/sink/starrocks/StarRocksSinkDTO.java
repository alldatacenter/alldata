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

package org.apache.inlong.manager.pojo.sink.starrocks;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sink info of StarRocks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarRocksSinkDTO {

    @ApiModelProperty("StarRocks jdbc url")
    private String jdbcUrl;

    @ApiModelProperty("StarRocks FE http address")
    private String loadUrl;

    @ApiModelProperty("Username for StarRocks accessing")
    private String username;

    @ApiModelProperty("Password for StarRocks accessing")
    private String password;

    @ApiModelProperty("Database name")
    private String databaseName;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("The primary key of sink table")
    private String primaryKey;

    @ApiModelProperty("The multiple enable of sink")
    private Boolean sinkMultipleEnable = false;

    @ApiModelProperty("The multiple format of sink")
    private String sinkMultipleFormat;

    @ApiModelProperty("The multiple database-pattern of sink")
    private String databasePattern;

    @ApiModelProperty("The multiple table-pattern of sink")
    private String tablePattern;

    @ApiModelProperty("The table engine,  like: OLAP, MYSQL, ELASTICSEARCH, etc, default is OLAP")
    private String tableEngine = "OLAP";

    @ApiModelProperty("The table replication num")
    private Integer replicationNum = 3;

    @ApiModelProperty("The table barrel size")
    private Integer barrelSize = 8;

    @ApiModelProperty("Password encrypt version")
    private Integer encryptVersion;

    @ApiModelProperty("Properties for StarRocks")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static StarRocksSinkDTO getFromRequest(StarRocksSinkRequest request) throws Exception {
        Integer encryptVersion = AESUtils.getCurrentVersion(null);
        String passwd = null;
        if (StringUtils.isNotEmpty(request.getPassword())) {
            passwd = AESUtils.encryptToString(request.getPassword().getBytes(StandardCharsets.UTF_8),
                    encryptVersion);
        }
        return StarRocksSinkDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .loadUrl(request.getLoadUrl())
                .username(request.getUsername())
                .password(passwd)
                .databaseName(request.getDatabaseName())
                .tableName(request.getTableName())
                .sinkMultipleEnable(request.getSinkMultipleEnable())
                .sinkMultipleFormat(request.getSinkMultipleFormat())
                .databasePattern(request.getDatabasePattern())
                .tablePattern(request.getTablePattern())
                .tableEngine(request.getTableEngine())
                .replicationNum(request.getReplicationNum())
                .barrelSize(request.getBarrelSize())
                .encryptVersion(encryptVersion)
                .properties(request.getProperties())
                .build();
    }

    public static StarRocksSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return Objects.requireNonNull(JsonUtils.parseObject(
                    extParams, StarRocksSinkDTO.class)).decryptPassword();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of StarRocks SinkDTO failure: %s", e.getMessage()));
        }
    }

    /**
     * Get StarRocks table info
     *
     * @param sinkDTO StarRocks sink dto,{@link StarRocksSinkDTO}
     * @param columnList StarRocks column info list,{@link StarRocksColumnInfo}
     * @return {@link StarRocksTableInfo}
     */
    public static StarRocksTableInfo getTableInfo(StarRocksSinkDTO sinkDTO, List<StarRocksColumnInfo> columnList) {
        StarRocksTableInfo tableInfo = new StarRocksTableInfo();
        tableInfo.setDbName(sinkDTO.getDatabaseName());
        tableInfo.setTableName(sinkDTO.getTableName());
        tableInfo.setColumns(columnList);
        tableInfo.setPrimaryKey(sinkDTO.getPrimaryKey());
        tableInfo.setTableEngine(sinkDTO.getTableEngine());
        tableInfo.setReplicationNum(sinkDTO.getReplicationNum());
        tableInfo.setBarrelSize(sinkDTO.getBarrelSize());
        return tableInfo;
    }

    private StarRocksSinkDTO decryptPassword() throws Exception {
        if (StringUtils.isNotEmpty(this.password)) {
            byte[] passwordBytes = AESUtils.decryptAsString(this.password, this.encryptVersion);
            this.password = new String(passwordBytes, StandardCharsets.UTF_8);
        }
        return this;
    }

}
