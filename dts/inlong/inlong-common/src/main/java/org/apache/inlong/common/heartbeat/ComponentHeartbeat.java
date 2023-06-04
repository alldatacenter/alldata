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

package org.apache.inlong.common.heartbeat;

import org.apache.inlong.common.enums.NodeSrvStatus;
import lombok.Data;

/**
 * Component heartbeat info
 */
@Data
public class ComponentHeartbeat {

    // node service status
    private NodeSrvStatus nodeSrvStatus;

    private String clusterTag;

    private String extTag;

    private String clusterName;

    private String componentType;

    private String ip;

    private String port;

    private String protocolType;

    private String inCharges;

    // node load
    private Integer load;

    public ComponentHeartbeat() {
    }

    public ComponentHeartbeat(String clusterTag, String extTag,
            String clusterName, String componentType, String ip,
            String port, String inCharges, String protocolType) {
        this.nodeSrvStatus = NodeSrvStatus.OK;
        this.clusterTag = clusterTag;
        this.extTag = extTag;
        this.clusterName = clusterName;
        this.componentType = componentType;
        this.ip = ip;
        this.port = port;
        this.protocolType = protocolType;
        this.inCharges = inCharges;
        this.load = 0xffff;
    }

    public ComponentHeartbeat(NodeSrvStatus nodeSrvStatus,
            String clusterTag, String extTag, String clusterName,
            String componentType, String ip, String port,
            String inCharges, String protocolType, int loadValue) {
        this.nodeSrvStatus = nodeSrvStatus;
        this.clusterTag = clusterTag;
        this.extTag = extTag;
        this.clusterName = clusterName;
        this.componentType = componentType;
        this.ip = ip;
        this.port = port;
        this.protocolType = protocolType;
        this.inCharges = inCharges;
        this.load = loadValue;
    }
}
