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

package org.apache.inlong.manager.common.pojo.sink.es;

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
 * Sink info of Elasticsearch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElasticsearchSinkDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Elasticsearch Host")
    private String host;

    @ApiModelProperty("Elasticsearch Port")
    private Integer port;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
    private String password;

    @ApiModelProperty("Elasticsearch index name")
    private String indexName;

    @ApiModelProperty("Flush interval, unit: second, default is 1s")
    private Integer flushInterval;

    @ApiModelProperty("Flush when record number reaches flushRecord")
    private Integer flushRecord;

    @ApiModelProperty("Write max retry times, default is 3")
    private Integer retryTimes;

    @ApiModelProperty("Document Type")
    private String documentType;

    @ApiModelProperty("Primary Key")
    private String primaryKey;

    @ApiModelProperty("version")
    private Integer version;

    @ApiModelProperty("Properties for elasticsearch")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static ElasticsearchSinkDTO getFromRequest(ElasticsearchSinkRequest request) {
        return ElasticsearchSinkDTO.builder()
                .host(request.getHost())
                .username(request.getUsername())
                .password(request.getPassword())
                .indexName(request.getIndexName())
                .flushInterval(request.getFlushInterval())
                .flushRecord(request.getFlushRecord())
                .retryTimes(request.getRetryTimes())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from the json
     */
    public static ElasticsearchSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, ElasticsearchSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage());
        }
    }

    public static String getElasticSearchIndexName(ElasticsearchSinkDTO esInfo,
            List<ElasticsearchFieldInfo> fieldList) {
        return esInfo.getIndexName();
    }

}
