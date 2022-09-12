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

package org.apache.inlong.manager.common.pojo.sink.hdfs;

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
import java.util.List;
import java.util.Map;

/**
 * HDFS sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HdfsSinkDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("File format, support: TextFile, RCFile, SequenceFile, Avro")
    private String fileFormat;

    @ApiModelProperty("Data path, such as: hdfs://ip:port/usr/hive/warehouse/test.db")
    private String dataPath;

    @ApiModelProperty("Compress formt")
    private String compressFormt;

    @ApiModelProperty("Server timeZone")
    private String serverTimeZone;

    @ApiModelProperty("Data field separator")
    private String dataSeparator;

    @ApiModelProperty("Partition field list")
    private List<HdfsPartitionField> partitionFieldList;

    @ApiModelProperty("Properties for hbase")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static HdfsSinkDTO getFromRequest(HdfsSinkRequest request) {
        return HdfsSinkDTO.builder()
                .dataPath(request.getDataPath())
                .dataSeparator(request.getDataSeparator())
                .fileFormat(request.getFileFormat())
                .compressFormt(request.getCompressFormt())
                .serverTimeZone(request.getServerTimeZone())
                .partitionFieldList(request.getPartitionFieldList())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get Hdfs sink info from JSON string
     */
    public static HdfsSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, HdfsSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage());
        }
    }

}
