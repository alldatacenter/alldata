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

package org.apache.inlong.manager.pojo.stream;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.enums.DataSeparator;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Inlong stream request.
 */
@Data
@ApiModel("Inlong stream request")
public class InlongStreamRequest {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @NotBlank(message = "inlongGroupId cannot be blank")
    @ApiModelProperty(value = "Inlong group id")
    private String inlongGroupId;

    @NotBlank(message = "inlongStreamId cannot be blank")
    @Length(min = 4, max = 100, message = "inlongStreamId length must be between 4 and 100")
    @Pattern(regexp = "^[a-z0-9_-]{4,100}$",
            message = "inlongStreamId only supports lowercase letters, numbers, '-', or '_'")
    @ApiModelProperty(value = "Inlong stream id")
    private String inlongStreamId;

    @ApiModelProperty(value = "Inlong stream name", required = true)
    private String name;

    @ApiModelProperty(value = "Inlong stream description")
    private String description;

    @ApiModelProperty(value = "MQ resource")
    private String mqResource;

    @ApiModelProperty(value = "Data type, including: TEXT, KV, etc.")
    private String dataType;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding = StandardCharsets.UTF_8.toString();

    @ApiModelProperty(value = "Data separator, stored as ASCII code")
    private String dataSeparator = DataSeparator.VERTICAL_BAR.getAsciiCode().toString();

    @ApiModelProperty(value = "Data field escape symbol, stored as ASCII code")
    private String dataEscapeChar;

    @ApiModelProperty(value = "Whether to send synchronously, 0: no, 1: yes",
            notes = "Each task under this stream sends data synchronously, "
                    + "which will affect the throughput of data collection, please choose carefully")
    private Integer syncSend = 0;

    @ApiModelProperty(value = "Number of access items per day, unit: 10,000 items per day")
    private Integer dailyRecords;

    @ApiModelProperty(value = "Access size per day, unit: GB per day")
    private Integer dailyStorage;

    @ApiModelProperty(value = "peak access per second, unit: bars per second")
    private Integer peakRecords;

    @ApiModelProperty(value = "The maximum length of a single piece of data, unit: Byte")
    private Integer maxLength;

    @ApiModelProperty(value = "Data storage period, unit: day")
    private Integer storagePeriod;

    @ApiModelProperty(value = "Extended params, will be saved as JSON string")
    private String extParams;

    @ApiModelProperty(value = "Field list")
    private List<StreamField> fieldList;

    @ApiModelProperty(value = "Inlong stream Extension properties")
    private List<InlongStreamExtInfo> extList;

    @ApiModelProperty(value = "Version number")
    private Integer version;

}
