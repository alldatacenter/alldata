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

package org.apache.inlong.common.enums;

/**
 * Enum of node service status.
 */
public enum NodeSrvStatus {

    OK(0, "Service ok"),

    SERVICE_UNREADY(1, "Service not ready"),

    SERVICE_UNINSTALL(2, "Service unregister"),

    UNKNOWN_ERROR(Integer.MAX_VALUE - 1, "Unknown status");

    private final int statusId;
    private final String statusDesc;

    NodeSrvStatus(int statusId, String statusDesc) {
        this.statusId = statusId;
        this.statusDesc = statusDesc;
    }

    public static NodeSrvStatus valueOf(int value) {
        for (NodeSrvStatus nodeStatus : NodeSrvStatus.values()) {
            if (nodeStatus.getStatusId() == value) {
                return nodeStatus;
            }
        }

        return UNKNOWN_ERROR;
    }

    public int getStatusId() {
        return statusId;
    }

    public String getStatusDesc() {
        return statusDesc;
    }
}
