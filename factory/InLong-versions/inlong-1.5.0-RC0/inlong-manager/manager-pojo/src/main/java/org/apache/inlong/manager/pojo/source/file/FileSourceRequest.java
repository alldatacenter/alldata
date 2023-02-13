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

package org.apache.inlong.manager.pojo.source.file;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.DataFormat;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;

import java.util.List;
import java.util.Map;

/**
 * File source request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = SourceType.FILE)
@ApiModel(value = "File source request")
public class FileSourceRequest extends SourceRequest {

    @ApiModelProperty(value = "Path regex pattern for file, such as /a/b/*.txt", required = true)
    private String pattern;

    @ApiModelProperty(value = "Path blacklist for file, which will be filtered and not collect", required = true)
    private String blackList;

    @ApiModelProperty("TimeOffset for collection, "
            + "'1m' means from one minute after, '-1m' means from one minute before, "
            + "'1h' means from one hour after, '-1h' means from one minute before, "
            + "'1d' means from one day after, '-1d' means from one minute before, "
            + "Null or blank means from current timestamp")
    private String timeOffset;

    @ApiModelProperty("Line end regex pattern, for example: &end&")
    private String lineEndPattern;

    @ApiModelProperty("Type of file content, for example: FULL, INCREMENT")
    private String contentCollectType;

    @ApiModelProperty("File needs to collect environment information, for example: kubernetes")
    private String envList;

    @ApiModelProperty("Metadata of data, for example: "
            + "[{data:field1,field2},{kubernetes:namespace,labels,name,uuid}] and so on")
    private List<Map<String, String>> metaFields;

    @ApiModelProperty(" Type of data result for column separator"
            + "         CSV format, set this parameter to a custom separator: , | : "
            + "         Json format, set this parameter to json ")
    private String dataContentStyle;

    @ApiModelProperty("Metadata filters by label, special parameters for K8S")
    private Map<String, String> filterMetaByLabels;

    public FileSourceRequest() {
        this.setSourceType(SourceType.FILE);
        this.setSerializationType(DataFormat.CSV.getName());
    }

}
