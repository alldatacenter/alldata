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
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Inlong stream request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("Inlong stream request")
public class InlongStreamRequest extends BaseInlongStream {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id")
    @NotBlank(message = "inlongGroupId cannot be blank")
    @Length(min = 4, max = 100, message = "length must be between 4 and 100")
    @Pattern(regexp = "^[a-z0-9_.-]{4,100}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong stream id")
    @NotBlank(message = "inlongStreamId cannot be blank")
    @Length(min = 1, max = 100, message = "inlongStreamId length must be between 1 and 100")
    @Pattern(regexp = "^[a-z0-9_.-]{1,100}$", message = "inlongStreamId only supports lowercase letters, numbers, '-', or '_'")
    private String inlongStreamId;

    @ApiModelProperty(value = "Inlong stream name", required = true)
    @Length(max = 64, message = "length must be less than or equal to 64")
    private String name;

    @ApiModelProperty(value = "Inlong stream description")
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String description;

    @ApiModelProperty(value = "MQ resource")
    @Length(max = 64, message = "length must be less than or equal to 64")
    private String mqResource;

    @ApiModelProperty(value = "Data type, including: TEXT, KV, etc.")
    @Length(max = 20, message = "length must be less than or equal to 20")
    private String dataType;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    @Length(max = 8, message = "length must be less than or equal to 8")
    private String dataEncoding = StandardCharsets.UTF_8.toString();

    @ApiModelProperty(value = "Data separator")
    @Length(max = 8, message = "length must be less than or equal to 8")
    private String dataSeparator = String.valueOf((int) '|');

    @ApiModelProperty(value = "Data field escape symbol")
    @Length(max = 8, message = "length must be less than or equal to 8")
    private String dataEscapeChar;

    @ApiModelProperty(value = "Whether to send synchronously, 0: no, 1: yes", notes = "Each task under this stream sends data synchronously, "
            + "which will affect the throughput of data collection, please choose carefully")
    @Range(min = 0, max = 1, message = "default is 0, only supports [0: no, 1: yes]")
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
    @Length(min = 1, max = 163840, message = "length must be between 1 and 163840")
    private String extParams;

    @ApiModelProperty(value = "Field list")
    private List<StreamField> fieldList;

    @ApiModelProperty(value = "Inlong stream Extension properties")
    private List<InlongStreamExtInfo> extList;

    @ApiModelProperty(value = "Version number")
    @NotNull(groups = UpdateValidation.class, message = "version cannot be null")
    private Integer version;

    @ApiModelProperty(value = "Whether to ignore the parse errors of field value")
    private boolean ignoreParseError = true;

    @ApiModelProperty(value = "Whether the message body wrapped with InlongMsg")
    private boolean wrapWithInlongMsg = true;
}
