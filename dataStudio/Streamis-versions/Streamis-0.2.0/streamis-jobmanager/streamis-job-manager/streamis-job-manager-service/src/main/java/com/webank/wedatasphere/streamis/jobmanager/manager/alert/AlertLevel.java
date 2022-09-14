/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.alert;


public enum AlertLevel {

    /**
     * Alert level(告警的级别)
     */
    CRITICAL(1, "critical"),
    MAJOR(2, "major"),
    MINOR(3, "minor"),
    WARNING(4, "warning"),
    INFO(5, "info");

    private AlertLevel(int level, String description){
        this.level = level;
        this.description = description;
    }

    private int level;
    private String description;

    public int getLevel(){
        return this.level;
    }

}
