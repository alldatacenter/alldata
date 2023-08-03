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

package org.apache.inlong.manager.pojo.audit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Audit source request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Audit source request")
public class AuditSourceRequest {

    @NotBlank
    @ApiModelProperty(value = "Audit source name")
    private String name;

    @NotBlank
    @ApiModelProperty(value = "Audit source type, including: MYSQL, CLICKHOUSE, ELASTICSEARCH", required = true)
    private String type;

    @NotBlank
    @ApiModelProperty(value = "Audit source URL, for MYSQL or CLICKHOUSE, is jdbcUrl, and for ELASTICSEARCH is the access URL with hostname:port", required = true)
    private String url;

    @ApiModelProperty(value = "Offline the url if not null")
    private String offlineUrl;

    @ApiModelProperty(value = "Enable auth or not, 0: disable, 1: enable")
    private Integer enableAuth;

    @ApiModelProperty(value = "Audit source username, needed if enableAuth is 1")
    private String username;

    @ApiModelProperty(value = "Audit source token, needed if enableAuth is 1")
    private String token;

    @ApiModelProperty(value = "Version number")
    private Integer version;

}
