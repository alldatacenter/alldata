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

package org.apache.inlong.manager.pojo.sink.hdfs;

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
 * HDFS sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HDFSSinkDTO {

    @ApiModelProperty("File format, support: TextFile, RCFile, SequenceFile, Avro")
    private String fileFormat;

    @ApiModelProperty("Data path, such as: hdfs://ip:port/usr/hive/warehouse/test.db")
    private String dataPath;

    @ApiModelProperty("Compress format")
    private String compressFormat;

    @ApiModelProperty("Server timeZone")
    private String serverTimeZone;

    @ApiModelProperty("Data field separator")
    private String dataSeparator;

    @ApiModelProperty("Partition field list")
    private List<HDFSPartitionField> partitionFieldList;

    @ApiModelProperty("Properties for hbase")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static HDFSSinkDTO getFromRequest(HDFSSinkRequest request) {
        return HDFSSinkDTO.builder()
                .dataPath(request.getDataPath())
                .dataSeparator(request.getDataSeparator())
                .fileFormat(request.getFileFormat())
                .compressFormat(request.getCompressFormat())
                .serverTimeZone(request.getServerTimeZone())
                .partitionFieldList(request.getPartitionFieldList())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get HDFS sink info from JSON string
     */
    public static HDFSSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, HDFSSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
