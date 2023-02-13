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

package org.apache.inlong.manager.pojo.node.hive;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;

/**
 * Hive data node request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = DataNodeType.HIVE)
@ApiModel("Hive data node request")
public class HiveDataNodeRequest extends DataNodeRequest {

    @ApiModelProperty("Version for Hive, such as: 3.2.1")
    private String hiveVersion;

    @ApiModelProperty("Config directory of Hive on HDFS, needed by sort in light mode, must include hive-site.xml")
    private String hiveConfDir;

    @ApiModelProperty("HDFS default FS, such as: hdfs://127.0.0.1:9000")
    private String hdfsPath;

    @ApiModelProperty("Hive warehouse path, such as: /user/hive/warehouse/")
    private String warehouse;

    @ApiModelProperty("User and group information for writing data to HDFS")
    private String hdfsUgi;

    public HiveDataNodeRequest() {
        this.setType(DataNodeType.HIVE);
    }

}
