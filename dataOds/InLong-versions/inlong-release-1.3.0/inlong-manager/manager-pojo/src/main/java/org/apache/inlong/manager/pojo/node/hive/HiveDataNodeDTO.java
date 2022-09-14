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

package org.apache.inlong.manager.pojo.node.hive;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * Hive data node info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Hive data node info")
public class HiveDataNodeDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(HiveDataNodeDTO.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // thread safe

    @ApiModelProperty("Hive JDBC URL, such as jdbc:hive2://${ip}:${port}")
    private String jdbcUrl;

    @ApiModelProperty("Version for Hive, such as: 3.2.1")
    private String hiveVersion;

    @ApiModelProperty("Config directory of Hive on HDFS, needed by sort in light mode, must include hive-site.xml")
    private String hiveConfDir;

    @ApiModelProperty("HDFS default FS, such as: hdfs://127.0.0.1:9000")
    private String hdfsPath;

    @ApiModelProperty("Hive warehouse path, such as: /user/hive/warehouse/")
    private String warehouse;

    @ApiModelProperty("User and group information for writing data to HDFS")
    private String hdfsUgi;

    /**
     * Get the dto instance from the request
     */
    public static HiveDataNodeDTO getFromRequest(HiveDataNodeRequest request) throws Exception {
        return HiveDataNodeDTO.builder()
                .jdbcUrl(request.getJdbcUrl())
                .hiveVersion(request.getHiveVersion())
                .hiveConfDir(request.getHiveConfDir())
                .hdfsPath(request.getHdfsPath())
                .warehouse(request.getWarehouse())
                .hdfsUgi(request.getHdfsUgi())
                .build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static HiveDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, HiveDataNodeDTO.class);
        } catch (Exception e) {
            LOGGER.error("Failed to extract additional parameters for hive data node: ", e);
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT.getMessage());
        }
    }

}
