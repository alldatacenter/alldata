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

package org.apache.inlong.manager.pojo.sink.doris;

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
 * Doris sink info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Doris sink info")
@JsonTypeDefine(value = SinkType.DORIS)
public class DorisSink extends StreamSink {

    @ApiModelProperty("Doris FE http address")
    private String feNodes;

    @ApiModelProperty("Username for doris accessing")
    private String username;

    @ApiModelProperty("Password for doris accessing")
    private String password;

    @ApiModelProperty("Doris table name, such as: db.tbl")
    private String tableIdentifier;

    @ApiModelProperty("Label prefix for stream loading. Used for guaranteeing Flink EOS semantics, as global unique is "
            + "needed in 2pc.")
    private String labelPrefix;

    @ApiModelProperty("The primary key of sink table")
    private String primaryKey;

    @ApiModelProperty("The multiple enable of sink")
    private Boolean sinkMultipleEnable = false;

    @ApiModelProperty("The multiple format of sink")
    private String sinkMultipleFormat;

    @ApiModelProperty("The multiple database-pattern of sink")
    private String databasePattern;

    @ApiModelProperty("The multiple table-pattern of sink")
    private String tablePattern;

    public DorisSink() {
        this.setSinkType(SinkType.DORIS);
    }

    @Override
    public SinkRequest genSinkRequest() {
        return CommonBeanUtils.copyProperties(this, DorisSinkRequest::new);
    }

}
