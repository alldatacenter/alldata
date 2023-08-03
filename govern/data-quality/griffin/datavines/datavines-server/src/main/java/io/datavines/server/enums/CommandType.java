/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;
import java.util.Map;

public enum CommandType {

    /**
     * 0-start
     * 1-stop
     * 2-error
     * 3-recover-tolerances-fault-task
     * 4-recover-waiting-thread
     * 5-scheduler
     */
    START(0, "start a task"),
    STOP(1, "stop a task"),
    ERROR(2, "task has error"),
    RECOVER_TOLERANCE_FAULT(3, "recover tolerance fault task"),
    RECOVER_WAITING_THREAD(4, "recover waiting thread"),
    SCHEDULER(5,"scheduler");

    CommandType(int code, String description){
        this.code = code;
        this.description = description;
    }

    @EnumValue
    private final int code;

    private final String description;

    private static final Map<Integer, CommandType> COMMAND_TYPE_MAP = new HashMap<>();

    static {
        for (CommandType commandType : CommandType.values()) {
            COMMAND_TYPE_MAP.put(commandType.code,commandType);
        }
    }

    public static CommandType of(Integer status) {
        if (COMMAND_TYPE_MAP.containsKey(status)) {
            return COMMAND_TYPE_MAP.get(status);
        }
        throw new IllegalArgumentException("invalid command : " + status);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
