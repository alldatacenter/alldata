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

package org.apache.inlong.manager.pojo.sink.hudi;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

/**
 * Hudi sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HudiSinkDTO {

    @ApiModelProperty("Catalog type, like: HIVE, HADOOP, default is HIVE")
    @Builder.Default
    private String catalogType = "HIVE";

    @ApiModelProperty("Catalog uri, such as hive metastore thrift://ip:port")
    private String catalogUri;

    @ApiModelProperty("Hudi data warehouse dir")
    private String warehouse;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Data path, such as: hdfs://ip:port/user/hive/warehouse/test.db")
    private String dataPath;

    @ApiModelProperty("File format, support: Parquet, Orc, Avro")
    private String fileFormat;

    @ApiModelProperty("Partition type, like: H-hour, D-day, W-week, M-month, O-once, R-regulation")
    private String partitionType;

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Properties for Hudi")
    private Map<String, Object> properties;

    @ApiModelProperty("Extended properties")
    private List<HashMap<String, String>> extList;

    @ApiModelProperty("Partition field list")
    private String partitionKey;

    /**
     * Get the dto instance from the request
     */
    public static HudiSinkDTO getFromRequest(HudiSinkRequest request) {
        return HudiSinkDTO.builder()
                .catalogUri(request.getCatalogUri())
                .warehouse(request.getWarehouse())
                .dbName(request.getDbName())
                .tableName(request.getTableName())
                .dataPath(request.getDataPath())
                .partitionKey(request.getPartitionKey())
                .fileFormat(request.getFileFormat())
                .catalogType(request.getCatalogType())
                .properties(request.getProperties())
                .extList(request.getExtList())
                .primaryKey(request.getPrimaryKey())
                .build();
    }

    public static HudiSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, HudiSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Get Hudi table info
     */
    public static HudiTableInfo getHudiTableInfo(HudiSinkDTO hudiInfo, List<HudiColumnInfo> columnList) {
        HudiTableInfo tableInfo = new HudiTableInfo();
        tableInfo.setDbName(hudiInfo.getDbName());
        tableInfo.setTableName(hudiInfo.getTableName());

        tableInfo.setPartitionKey(hudiInfo.getPartitionKey());
        tableInfo.setColumns(columnList);
        tableInfo.setPrimaryKey(hudiInfo.getPrimaryKey());
        tableInfo.setFileFormat(hudiInfo.getFileFormat());
        tableInfo.setTblProperties(hudiInfo.getProperties());
        return tableInfo;
    }

}
