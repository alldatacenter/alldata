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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
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

    @ApiModelProperty("Host of the Elasticsearch server")
    private String host;

    @ApiModelProperty("Port of the Elasticsearch server")
    private Integer port;

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

    @ApiModelProperty("Password encrypt version")
    private Integer encryptVersion;

    @ApiModelProperty("Properties for elasticsearch")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static ElasticsearchSinkDTO getFromRequest(ElasticsearchSinkRequest request) throws Exception {
        Integer encryptVersion = AESUtils.getCurrentVersion(null);
        String passwd = null;
        if (StringUtils.isNotEmpty(request.getPassword())) {
            passwd = AESUtils.encryptToString(request.getPassword().getBytes(StandardCharsets.UTF_8),
                    encryptVersion);
        }
        return ElasticsearchSinkDTO.builder()
                .host(request.getHost())
                .username(request.getUsername())
                .password(passwd)
                .indexName(request.getIndexName())
                .flushInterval(request.getFlushInterval())
                .flushRecord(request.getFlushRecord())
                .retryTimes(request.getRetryTimes())
                .documentType(request.getDocumentType())
                .primaryKey(request.getPrimaryKey())
                .esVersion(request.getEsVersion())
                .encryptVersion(encryptVersion)
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from the json
     */
    public static ElasticsearchSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, ElasticsearchSinkDTO.class).decryptPassword();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    private ElasticsearchSinkDTO decryptPassword() throws Exception {
        if (StringUtils.isNotEmpty(this.password)) {
            byte[] passwordBytes = AESUtils.decryptAsString(this.password, this.encryptVersion);
            this.password = new String(passwordBytes, StandardCharsets.UTF_8);
        }
        return this;
    }

}
