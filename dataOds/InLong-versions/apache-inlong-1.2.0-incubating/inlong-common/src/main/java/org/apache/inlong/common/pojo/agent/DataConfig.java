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

import lombok.Data;

/**
 * The task config for agent.
 */
@Data
public class DataConfig {

    private String ip;
    private String uuid;
    private String inlongGroupId;
    private String inlongStreamId;
    private String op;
    private Integer taskId;
    private Integer taskType;
    private String taskName;
    private String snapshot;
    private Integer syncSend;
    private String extParams;
    /**
     * The task version.
     */
    private Integer version;
    /**
     * The task delivery time, format is 'yyyy-MM-dd HH:mm:ss'.
     */
    private String deliveryTime;

    public boolean isValid() {
        return true;
    }
}