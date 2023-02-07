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

package org.apache.inlong.manager.pojo.node.iceberg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;

/**
 * Iceberg data node request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = DataNodeType.ICEBERG)
@ApiModel("Iceberg data node request")
public class IcebergDataNodeRequest extends DataNodeRequest {

    @ApiModelProperty("Catalog type, like: HIVE, HADOOP, default is HIVE")
    private String catalogType = "HIVE";

    @ApiModelProperty("Iceberg data warehouse dir")
    private String warehouse;

    public IcebergDataNodeRequest() {
        this.setType(DataNodeType.ICEBERG);
    }

}
