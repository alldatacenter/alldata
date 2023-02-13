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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.enums.TimeStaticsDim;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * The request info of audit.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel("Audit query request")
public class AuditRequest {

    @NotBlank(message = "inlongGroupId not be blank")
    @ApiModelProperty(value = "inlong group id", required = true)
    private String inlongGroupId;

    @NotBlank(message = "inlongStreamId not be blank")
    @ApiModelProperty(value = "inlong stream id", required = true)
    private String inlongStreamId;

    @ApiModelProperty(value = "audit id list", required = true)
    private List<String> auditIds;

    @ApiModelProperty(value = "query date, format by 'yyyy-MM-dd'", required = true, example = "2022-01-01")
    @NotBlank(message = "dt not be blank")
    private String dt;

    /**
     * Time statics dim such as MINUTE, HOUR, DAY
     */
    @ApiModelProperty(value = "time statics dim, default MINUTE", required = true, example = "MINUTE")
    private TimeStaticsDim timeStaticsDim = TimeStaticsDim.MINUTE;

}