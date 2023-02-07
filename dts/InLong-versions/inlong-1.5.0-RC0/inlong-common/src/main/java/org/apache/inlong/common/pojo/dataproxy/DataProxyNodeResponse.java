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

package org.apache.inlong.common.pojo.dataproxy;

import lombok.Data;

import java.util.List;

/**
 * DataProxy node response, used for DataProxy SDK.
 */
@Data
public class DataProxyNodeResponse {

    /**
     * DataProxy cluster id
     */
    @Deprecated
    private Integer clusterId;

    /**
     * Is the DataProxy cluster an intranet? 0: no, 1: yes
     */
    private Integer isIntranet;

    /**
     * Is the DataProxy cluster in a switch status? 0: no, 1: yes
     */
    private Integer isSwitch;

    /**
     * Load of the DataProxy cluster, default is 20
     */
    private Integer load = 20;

    /**
     * List of the cluster node
     */
    private List<DataProxyNodeInfo> nodeList;

}
