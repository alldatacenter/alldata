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

package org.apache.inlong.manager.pojo.sink.hbase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * HBase sink request.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "HBase sink request")
@JsonTypeDefine(value = SinkType.HBASE)
public class HBaseSinkRequest extends SinkRequest {

    @ApiModelProperty("Target namespace")
    private String namespace;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Row key")
    private String rowKey;

    @ApiModelProperty("ZooKeeper quorum")
    private String zkQuorum;

    @ApiModelProperty("ZooKeeper node parent")
    private String zkNodeParent;

    @ApiModelProperty("Sink buffer flush maxsize")
    private String bufferFlushMaxSize;

    @ApiModelProperty("Sink buffer flush max rows")
    private String bufferFlushMaxRows;

    @ApiModelProperty("Sink buffer flush interval")
    private String bufferFlushInterval;

}
