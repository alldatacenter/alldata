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

package org.apache.inlong.manager.common.enums;

import org.apache.inlong.manager.common.exceptions.WorkflowException;

/**
 * Status enum of cluster node
 */
public enum NodeStatus {

    NORMAL(1),

    HEARTBEAT_TIMEOUT(2);

    int status;

    NodeStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static NodeStatus fromStatus(int status) {
        switch (status) {
            case 1:
                return NORMAL;
            case 2:
                return HEARTBEAT_TIMEOUT;
            default:
                throw new WorkflowException("unknown status: " + status);
        }
    }
}
