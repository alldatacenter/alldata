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

package org.apache.inlong.manager.pojo.sink.dlciceberg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * DLCIceberg sink request.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "DLCIceberg sink request")
@JsonTypeDefine(value = SinkType.DLCICEBERG)
public class DLCIcebergSinkRequest extends SinkRequest {

    @ApiModelProperty("Catalog URI of the DLCIceberg server")
    private String catalogUri;

    @ApiModelProperty("Data warehouse of the DLCIceberg server")
    private String warehouse;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty(value = "Primary key")
    private String primaryKey;

}
