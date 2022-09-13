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

package org.apache.inlong.common.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The entity of task command, used for Agent to interact with Manager and BDB.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandEntity {

    private String id;
    private int commandResult;
    private boolean isAcked;
    private Integer taskId;
    /**
     * The task version.
     */
    private Integer version;
    /**
     * The task delivery time, format is 'yyyy-MM-dd HH:mm:ss'.
     */
    private String deliveryTime;

    public static String generateCommandId(String taskId, int opType) {
        return taskId + opType;
    }

}
