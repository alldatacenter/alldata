/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.domain.request.datasource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "Virtual Table Field Request", description = "create ")
public class VirtualTableFieldReq {

    @ApiModelProperty(value = "field name", required = true, dataType = "String")
    private String fieldName;

    @ApiModelProperty(
            value = "field type",
            required = true,
            dataType = "String",
            example = "varchar")
    private String fieldType;

    @ApiModelProperty(
            value = "field length",
            dataType = "Boolean",
            example = "true",
            notes = "default false")
    private Boolean nullable;

    @ApiModelProperty(value = "field default value", dataType = "String", example = "10")
    private String defaultValue;

    @ApiModelProperty(
            value = "primary key",
            dataType = "Boolean",
            example = "true",
            notes = "default false, only one field can be primary key")
    private Boolean primaryKey;

    @ApiModelProperty(value = "field comment", dataType = "String", example = "this is a comment")
    private String fieldComment;

    @ApiModelProperty(value = "field extra", dataType = "String", example = "this is a extra")
    private String fieldExtra;
}
