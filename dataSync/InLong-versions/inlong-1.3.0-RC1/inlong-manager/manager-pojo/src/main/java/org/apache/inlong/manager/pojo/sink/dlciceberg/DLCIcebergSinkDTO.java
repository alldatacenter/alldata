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

package org.apache.inlong.manager.pojo.sink.dlciceberg;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * DLCIceberg sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DLCIcebergSinkDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Catalog URI of the DLCIceberg server")
    private String catalogUri;

    @ApiModelProperty("Data warehouse of the DLCIceberg server")
    private String warehouse;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty(value = "Primary key")
    private String primaryKey;

    @ApiModelProperty("Properties for DLCIceberg")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static DLCIcebergSinkDTO getFromRequest(DLCIcebergSinkRequest request) {
        return DLCIcebergSinkDTO.builder()
                .catalogUri(request.getCatalogUri())
                .warehouse(request.getWarehouse())
                .dbName(request.getDbName())
                .tableName(request.getTableName())
                .primaryKey(request.getPrimaryKey())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from json
     */
    public static DLCIcebergSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, DLCIcebergSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
