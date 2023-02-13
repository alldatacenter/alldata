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

package org.apache.inlong.manager.pojo.sink.hive;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Hive sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HiveSinkDTO {

    @ApiModelProperty("Hive JDBC URL, such as jdbc:hive2://${ip}:${port}")
    private String jdbcUrl;

    @ApiModelProperty("Username of the Hive server")
    private String username;

    @ApiModelProperty("User password of the Hive server")
    private String password;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Data path, such as: hdfs://ip:port/user/hive/warehouse/test.db")
    private String dataPath;

    @ApiModelProperty("Partition interval, support: 1 H, 1 D, 30 I, 10 I")
    private Integer partitionInterval;

    @ApiModelProperty("Partition field list")
    private List<HivePartitionField> partitionFieldList;

    @ApiModelProperty("Partition creation strategy, partition start, partition close")
    private String partitionCreationStrategy;

    @ApiModelProperty("File format, support: TextFile, ORCFile, RCFile, SequenceFile, Avro, Parquet, etc")
    private String fileFormat;

    @ApiModelProperty("Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty("Data separator")
    private String dataSeparator;

    @ApiModelProperty("Properties for hive")
    private Map<String, Object> properties;

    @ApiModelProperty("Version for Hive, such as: 3.2.1")
    private String hiveVersion;

    @ApiModelProperty("Password encrypt version")
    private Integer encryptVersion;

    @ApiModelProperty("Config directory of Hive on HDFS, needed by sort in light mode, must include hive-site.xml")
    private String hiveConfDir;

    /**
     * Get the dto instance from the request
     */
    public static HiveSinkDTO getFromRequest(HiveSinkRequest request) throws Exception {
        Integer encryptVersion = AESUtils.getCurrentVersion(null);
        String passwd = null;
        if (StringUtils.isNotEmpty(request.getPassword())) {
            passwd = AESUtils.encryptToString(request.getPassword().getBytes(StandardCharsets.UTF_8),
                    encryptVersion);
        }
        return HiveSinkDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .username(request.getUsername())
                .password(passwd)
                .dbName(request.getDbName())
                .tableName(request.getTableName())
                .dataPath(request.getDataPath())
                .partitionInterval(request.getPartitionInterval())
                .partitionFieldList(request.getPartitionFieldList())
                .partitionCreationStrategy(request.getPartitionCreationStrategy())
                .fileFormat(request.getFileFormat())
                .dataEncoding(request.getDataEncoding())
                .dataSeparator(request.getDataSeparator())
                .hiveVersion(request.getHiveVersion())
                .hiveConfDir(request.getHiveConfDir())
                .encryptVersion(encryptVersion)
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get Hive sink info from JSON string
     */
    public static HiveSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, HiveSinkDTO.class).decryptPassword();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Get Hive table info
     */
    public static HiveTableInfo getHiveTableInfo(HiveSinkDTO hiveInfo, List<HiveColumnInfo> columnList) {
        HiveTableInfo tableInfo = new HiveTableInfo();
        tableInfo.setDbName(hiveInfo.getDbName());
        tableInfo.setTableName(hiveInfo.getTableName());

        // Set partition fields
        if (CollectionUtils.isNotEmpty(hiveInfo.getPartitionFieldList())) {
            for (HivePartitionField field : hiveInfo.getPartitionFieldList()) {
                HiveColumnInfo columnInfo = new HiveColumnInfo();
                columnInfo.setName(field.getFieldName());
                columnInfo.setPartition(true);
                columnInfo.setType("string");
                columnList.add(columnInfo);
            }
        }
        tableInfo.setColumns(columnList);

        // set terminated symbol
        if (hiveInfo.getDataSeparator() != null) {
            char ch = (char) Integer.parseInt(hiveInfo.getDataSeparator());
            tableInfo.setFieldTerSymbol(String.valueOf(ch));
        }

        return tableInfo;
    }

    private HiveSinkDTO decryptPassword() throws Exception {
        if (StringUtils.isNotEmpty(this.password)) {
            byte[] passwordBytes = AESUtils.decryptAsString(this.password, this.encryptVersion);
            this.password = new String(passwordBytes, StandardCharsets.UTF_8);
        }
        return this;
    }

}
