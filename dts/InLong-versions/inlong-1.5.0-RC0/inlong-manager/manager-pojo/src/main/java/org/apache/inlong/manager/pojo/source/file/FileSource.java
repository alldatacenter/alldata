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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;

import java.util.List;
import java.util.Map;

/**
 * File source info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "File source info")
@JsonTypeDefine(value = SourceType.FILE)
public class FileSource extends StreamSource {

    @ApiModelProperty("Path regex pattern for file, such as /a/b/*.txt")
    private String pattern;

    @ApiModelProperty("Path blacklist for file, which will be filtered and not collect")
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

    public FileSource() {
        this.setSourceType(SourceType.FILE);
    }

    @Override
    public SourceRequest genSourceRequest() {
        return CommonBeanUtils.copyProperties(this, FileSourceRequest::new);
    }

}
