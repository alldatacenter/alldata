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

package org.apache.inlong.manager.service.workflow;

/**
 * WorkflowProcess name
 */
public enum ProcessName {

    /**
     * New inlong group application process
     */
    NEW_GROUP_PROCESS("New-Group"),

    /**
     * Startup inlong group application process
     */
    CREATE_GROUP_RESOURCE("Create-Group"),

    /**
     * Suspend inlong group application process
     */
    SUSPEND_GROUP_PROCESS("Suspend-Group"),

    /**
     * Restart inlong group application process
     */
    RESTART_GROUP_PROCESS("Restart-Group"),

    /**
     * Delete inlong group application process
     */
    DELETE_GROUP_PROCESS("Delete-Group"),

    /**
     * Startup lightweight inlong group application process
     */
    CREATE_LIGHT_GROUP_PROCESS("Create-Light-Group"),

    /**
     * Suspend lightweight inlong group application process
     */
    SUSPEND_LIGHT_GROUP_PROCESS("Suspend-Light-Group"),

    /**
     * Restart lightweight inlong group application process
     */
    RESTART_LIGHT_GROUP_PROCESS("Restart-Light-Group"),

    /**
     * Delete lightweight inlong group application process
     */
    DELETE_LIGHT_GROUP_PROCESS("Delete-Light-Group"),

    /**
     * New consumption application process
     */
    NEW_CONSUMPTION_PROCESS("New-Consumption"),

    /**
     * Startup single stream process
     */
    CREATE_STREAM_RESOURCE("Create-Stream"),

    /**
     * Suspend single stream process
     */
    SUSPEND_STREAM_RESOURCE("Suspend-Stream"),

    /**
     * Restart single stream process
     */
    RESTART_STREAM_RESOURCE("Restart-Stream"),

    /**
     * Delete single stream process
     */
    DELETE_STREAM_RESOURCE("Delete-Stream");

    private final String displayName;

    ProcessName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
