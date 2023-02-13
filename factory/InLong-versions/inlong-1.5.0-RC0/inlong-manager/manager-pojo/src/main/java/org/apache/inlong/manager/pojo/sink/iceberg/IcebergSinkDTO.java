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

package org.apache.inlong.manager.pojo.sink.iceberg;

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
 * Iceberg sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcebergSinkDTO {

    @ApiModelProperty("Catalog type, like: HIVE, HADOOP, default is HIVE")
    @Builder.Default
    private String catalogType = "HIVE";

    @ApiModelProperty("Catalog uri, such as hive metastore thrift://ip:port")
    private String catalogUri;

    @ApiModelProperty("Iceberg data warehouse dir")
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

    @ApiModelProperty("Properties for iceberg")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static IcebergSinkDTO getFromRequest(IcebergSinkRequest request) {
        return IcebergSinkDTO.builder()
                .catalogUri(request.getCatalogUri())
                .warehouse(request.getWarehouse())
                .dbName(request.getDbName())
                .tableName(request.getTableName())
                .dataPath(request.getDataPath())
                .fileFormat(request.getFileFormat())
                .catalogType(request.getCatalogType())
                .primaryKey(request.getPrimaryKey())
                .properties(request.getProperties())
                .build();
    }

    public static IcebergSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, IcebergSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Get Iceberg table info
     */
    public static IcebergTableInfo getIcebergTableInfo(IcebergSinkDTO icebergInfo, List<IcebergColumnInfo> columnList) {
        IcebergTableInfo info = new IcebergTableInfo();
        info.setDbName(icebergInfo.getDbName());
        info.setTableName(icebergInfo.getTableName());
        info.setFileFormat(icebergInfo.getFileFormat());
        info.setTblProperties(icebergInfo.getProperties());
        info.setColumns(columnList);
        return info;
    }

}
