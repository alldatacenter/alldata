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

package org.apache.inlong.manager.common.pojo.sink.hive;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // thread safe

    @ApiModelProperty("Hive JDBC URL")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
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

    @ApiModelProperty("File format, support: TextFile, RCFile, SequenceFile, Avro")
    private String fileFormat;

    @ApiModelProperty("Data encoding type")
    private String dataEncoding;

    @ApiModelProperty("Data field separator")
    private String dataSeparator;

    @ApiModelProperty("Properties for hive")
    private Map<String, Object> properties;

    @ApiModelProperty("Version for hive")
    private String hiveVersion;

    @ApiModelProperty("Config directory of hive, needed by sort in light mode")
    private String hiveConfDir;

    /**
     * Get the dto instance from the request
     */
    public static HiveSinkDTO getFromRequest(HiveSinkRequest request) {
        return HiveSinkDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .username(request.getUsername())
                .password(request.getPassword())
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
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get Hive sink info from JSON string
     */
    public static HiveSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, HiveSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage());
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

}
