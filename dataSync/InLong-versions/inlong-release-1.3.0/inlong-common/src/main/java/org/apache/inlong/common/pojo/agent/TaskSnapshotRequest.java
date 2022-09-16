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

package org.apache.inlong.common.pojo.agent;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Request of the agent task snapshot
 */
@Data
public class TaskSnapshotRequest {

    /**
     * The ip of agent Ip
     */
    private String agentIp;

    /**
     * The mac UUID of agent
     */
    private  String uuid;

    /**
     * Report Time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reportTime;

    /**
     * The snapshot message list
     */
    private List<TaskSnapshotMessage> snapshotList;


}
