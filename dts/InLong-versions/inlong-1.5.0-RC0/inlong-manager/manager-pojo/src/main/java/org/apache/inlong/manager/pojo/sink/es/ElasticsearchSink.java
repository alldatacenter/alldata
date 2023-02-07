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

package org.apache.inlong.manager.pojo.sink.es;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;

/**
 * Elasticsearch sink info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Elasticsearch sink info")
@JsonTypeDefine(value = SinkType.ELASTICSEARCH)
public class ElasticsearchSink extends StreamSink {

    @ApiModelProperty("Host of the Elasticsearch server")
    private String hosts;

    @ApiModelProperty("Username of the Elasticsearch server")
    private String username;

    @ApiModelProperty("User password of the Elasticsearch server")
    private String password;

    @ApiModelProperty("Elasticsearch index name")
    private String indexName;

    @ApiModelProperty("Flush interval, unit: second, default is 1s")
    private Integer flushInterval;

    @ApiModelProperty("Flush when record number reaches flushRecord")
    private Integer flushRecord;

    @ApiModelProperty("Write max retry times, default is 3")
    private Integer retryTimes;

    @ApiModelProperty("Key field names, separate with commas")
    private String keyFieldNames;

    @ApiModelProperty("Document Type")
    private String documentType;

    @ApiModelProperty("Primary Key")
    private String primaryKey;

    @ApiModelProperty("Elasticsearch version")
    private Integer esVersion;

    public ElasticsearchSink() {
        this.setSinkType(SinkType.ELASTICSEARCH);
    }

    @Override
    public SinkRequest genSinkRequest() {
        return CommonBeanUtils.copyProperties(this, ElasticsearchSinkRequest::new);
    }

}
