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

package org.apache.inlong.manager.pojo.sink;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sink field info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Sink field info")
public class SinkField {

    @ApiModelProperty("Field index")
    private Integer id;

    @ApiModelProperty(value = "inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "inlong stream id", required = true)
    private String inlongStreamId;

    @ApiModelProperty(value = "Field name", required = true)
    private String fieldName;

    @ApiModelProperty(value = "Field type", required = true)
    private String fieldType;

    @ApiModelProperty(value = "Field comment")
    private String fieldComment;

    @ApiModelProperty("Is this field a meta field, 0: no, 1: yes")
    @Builder.Default
    private Integer isMetaField = 0;

    @ApiModelProperty(value = "Meta field name")
    private String metaFieldName;

    @ApiModelProperty("Field format, including: MICROSECONDS, MILLISECONDS, SECONDS, SQL, ISO_8601"
            + " and custom such as 'yyyy-MM-dd HH:mm:ss'. This is mainly used for time format")
    private String fieldFormat;

    @ApiModelProperty("Origin node name which stream fields belong")
    private String originNodeName;

    @ApiModelProperty("Origin field name before transform operation")
    private String originFieldName;

    @ApiModelProperty("Source field name")
    private String sourceFieldName;

    @ApiModelProperty("Source field type")
    private String sourceFieldType;

    @ApiModelProperty("Extra Param in JSON style")
    private String extParams;

    public SinkField(int index, String fieldType, String fieldName, String sourceFieldType, String sourceFieldName) {
        this(index, fieldType, fieldName, null, sourceFieldName, sourceFieldType, 0, null, null);
    }

    public SinkField(int index, String fieldType, String fieldName, String fieldComment,
            String sourceFieldName, String sourceFieldType,
            Integer isMetaField, String metaFieldName, String fieldFormat) {
        this(index, fieldType, fieldName, fieldComment, isMetaField, metaFieldName, fieldFormat);
        this.sourceFieldName = sourceFieldName;
        this.sourceFieldType = sourceFieldType;
    }

    public SinkField(int index, String fieldType, String fieldName, String fieldComment,
            Integer isMetaField, String metaFieldName, String originNodeName) {
        this(fieldType, index, fieldName, fieldComment);
        this.isMetaField = isMetaField;
        this.metaFieldName = metaFieldName;
        this.originNodeName = originNodeName;
    }

    public SinkField(String fieldType, int index, String fieldName, String fieldComment) {
        this.id = index;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.fieldComment = fieldComment;
    }
}
