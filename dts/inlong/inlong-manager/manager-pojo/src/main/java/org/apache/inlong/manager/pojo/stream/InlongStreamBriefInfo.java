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

import org.apache.inlong.manager.pojo.sink.SinkBriefInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Inlong stream brief info
 */
@Data
@ApiModel("Inlong stream brief info")
public class InlongStreamBriefInfo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong stream id")
    private String inlongStreamId;

    @ApiModelProperty(value = "Inlong stream name")
    private String name;

    @ApiModelProperty(value = "Inlong stream description")
    private String description;

    @ApiModelProperty(value = "MQ resource")
    private String mqResource;

    @ApiModelProperty(value = "Data type, including: TEXT, KV, etc.")
    private String dataType;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator;

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    @ApiModelProperty(value = "Whether to send synchronously, 0: no, 1: yes")
    private Integer syncSend;

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

    @ApiModelProperty(value = "Whether the message body wrapped with InlongMsg")
    private Boolean wrapWithInlongMsg;

    @ApiModelProperty(value = "Whether to ignore the parse errors of field value")
    private Boolean ignoreParseError;

    @ApiModelProperty(value = "Status")
    private Integer status;

    @ApiModelProperty(value = "Name of creator")
    private String creator;

    @ApiModelProperty(value = "Name of modifier")
    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @ApiModelProperty(value = "Version number")
    private Integer version;

    @ApiModelProperty(value = "Sink brief list")
    private List<SinkBriefInfo> sinkList;

    @ApiModelProperty(value = "Field list")
    private List<StreamField> fieldList;

    @ApiModelProperty(value = "Inlong stream Extension properties")
    private List<InlongStreamExtInfo> extList;

}
