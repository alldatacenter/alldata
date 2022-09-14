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

package org.apache.inlong.manager.pojo.sink.ck;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Sink info of ClickHouse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickHouseSinkDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("JDBC URL of the ClickHouse server")
    private String jdbcUrl;

    @ApiModelProperty("Username of the ClickHouse server")
    private String username;

    @ApiModelProperty("User password of the ClickHouse server")
    private String password;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Flush interval, unit: second, default is 1s")
    private Integer flushInterval;

    @ApiModelProperty("Flush when record number reaches flushRecord")
    private Integer flushRecord;

    @ApiModelProperty("Write max retry times, default is 3")
    private Integer retryTimes;

    @ApiModelProperty("Whether distributed table? 0: no, 1: yes")
    private Integer isDistributed;

    @ApiModelProperty("Partition strategy, support: BALANCE, RANDOM, HASH")
    private String partitionStrategy;

    @ApiModelProperty(value = "Partition files, separate with commas",
            notes = "Necessary when partitionStrategy is HASH, must be one of the field list")
    private String partitionFields;

    @ApiModelProperty("Key field names, separate with commas")
    private String keyFieldNames;

    @ApiModelProperty("ClickHouse table engine, support MergeTree Mem and so on")
    private String engine;

    @ApiModelProperty("Table partition information")
    private String partitionBy;

    @ApiModelProperty("Table order information")
    private String orderBy;

    @ApiModelProperty("Table primary key")
    private String primaryKey;

    @ApiModelProperty("Password encrypt version")
    private Integer encryptVersion;

    @ApiModelProperty("Properties for clickhouse")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static ClickHouseSinkDTO getFromRequest(ClickHouseSinkRequest request) throws Exception {
        Integer encryptVersion = AESUtils.getCurrentVersion(null);
        String passwd = null;
        if (StringUtils.isNotEmpty(request.getPassword())) {
            passwd = AESUtils.encryptToString(request.getPassword().getBytes(StandardCharsets.UTF_8),
                    encryptVersion);
        }
        return ClickHouseSinkDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .username(request.getUsername())
                .password(passwd)
                .dbName(request.getDbName())
                .tableName(request.getTableName())
                .flushInterval(request.getFlushInterval())
                .flushRecord(request.getFlushRecord())
                .retryTimes(request.getRetryTimes())
                .isDistributed(request.getIsDistributed())
                .partitionStrategy(request.getPartitionStrategy())
                .partitionFields(request.getPartitionFields())
                .keyFieldNames(request.getKeyFieldNames())
                .engine(request.getEngine())
                .partitionBy(request.getPartitionBy())
                .primaryKey(request.getPrimaryKey())
                .orderBy(request.getOrderBy())
                .encryptVersion(encryptVersion)
                .properties(request.getProperties())
                .build();
    }

    public static ClickHouseSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, ClickHouseSinkDTO.class).decryptPassword();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    public static ClickHouseTableInfo getClickHouseTableInfo(ClickHouseSinkDTO ckInfo,
            List<ClickHouseColumnInfo> columnList) {
        ClickHouseTableInfo tableInfo = new ClickHouseTableInfo();
        tableInfo.setDbName(ckInfo.getDbName());
        tableInfo.setTableName(ckInfo.getTableName());
        tableInfo.setEngine(ckInfo.getEngine());
        tableInfo.setOrderBy(ckInfo.getOrderBy());
        tableInfo.setPartitionBy(ckInfo.getPartitionBy());
        tableInfo.setPrimaryKey(ckInfo.getPrimaryKey());
        tableInfo.setColumns(columnList);

        return tableInfo;
    }

    private ClickHouseSinkDTO decryptPassword() throws Exception {
        if (StringUtils.isNotEmpty(this.password)) {
            byte[] passwordBytes = AESUtils.decryptAsString(this.password, this.encryptVersion);
            this.password = new String(passwordBytes, StandardCharsets.UTF_8);
        }
        return this;
    }

}
