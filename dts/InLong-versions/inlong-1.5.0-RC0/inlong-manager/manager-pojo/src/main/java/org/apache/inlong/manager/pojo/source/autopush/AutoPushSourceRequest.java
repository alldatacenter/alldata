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

package org.apache.inlong.manager.pojo.source.autopush;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;

import java.nio.charset.StandardCharsets;

/**
 * AutoPush(DataProxy SDK) source request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Auto push source request")
@JsonTypeDefine(value = SourceType.AUTO_PUSH)
public class AutoPushSourceRequest extends SourceRequest {

    @ApiModelProperty(value = "DataProxy group name, used when the user enables local configuration")
    private String dataProxyGroup;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding = StandardCharsets.UTF_8.toString();

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator = String.valueOf((int) '|');

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    public AutoPushSourceRequest() {
        this.setSourceType(SourceType.AUTO_PUSH);
    }

}
