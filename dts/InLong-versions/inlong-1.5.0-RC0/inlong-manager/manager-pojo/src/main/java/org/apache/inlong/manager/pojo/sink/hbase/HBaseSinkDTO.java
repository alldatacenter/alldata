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

package org.apache.inlong.manager.pojo.sink.hbase;

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
 * HBase sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HBaseSinkDTO {

    @ApiModelProperty("Target namespace")
    private String namespace;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Row key")
    private String rowKey;

    @ApiModelProperty("ZooKeeper quorum")
    private String zkQuorum;

    @ApiModelProperty("ZooKeeper node parent")
    private String zkNodeParent;

    @ApiModelProperty("Sink buffer flush maxsize")
    private String bufferFlushMaxSize;

    @ApiModelProperty("Sink buffer flush max rows")
    private String bufferFlushMaxRows;

    @ApiModelProperty("Sink buffer flush interval")
    private String bufferFlushInterval;

    @ApiModelProperty("Properties for hbase")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static HBaseSinkDTO getFromRequest(HBaseSinkRequest request) {
        return HBaseSinkDTO.builder()
                .tableName(request.getTableName())
                .namespace(request.getNamespace())
                .rowKey(request.getRowKey())
                .zkQuorum(request.getZkQuorum())
                .bufferFlushMaxSize(request.getBufferFlushMaxSize())
                .zkNodeParent(request.getZkNodeParent())
                .bufferFlushMaxRows(request.getBufferFlushMaxRows())
                .bufferFlushInterval(request.getBufferFlushInterval())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get hbase sink info from JSON string
     */
    public static HBaseSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, HBaseSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Get hbase table info
     */
    public static HBaseTableInfo getHbaseTableInfo(HBaseSinkDTO hbaseInfo, List<HBaseColumnFamilyInfo> columnFamilies) {
        HBaseTableInfo info = new HBaseTableInfo();
        info.setNamespace(hbaseInfo.getNamespace());
        info.setTableName(hbaseInfo.getTableName());
        info.setTblProperties(hbaseInfo.getProperties());
        info.setColumnFamilies(columnFamilies);
        return info;
    }

}
