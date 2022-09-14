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
package com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo;

import java.util.HashMap;
import java.util.Map;

/**
 * Schedule Result
 * @param
 */
public class ScheduleResultVo {

    /**
     * Schedule id
     */
    protected String scheduleId;

    /**
     * Schedule status
     */
    protected String scheduleState;

    /**
     * Error message
     */
    protected String message;
    /**
     * Progress
     */
    private double progress = 0d;

    /**
     * Metric
     */
    private Map<String, Object> metric = new HashMap<>();

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getScheduleState() {
        return scheduleState;
    }

    public void setScheduleState(String scheduleState) {
        this.scheduleState = scheduleState;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public Map<String, Object> getMetric() {
        return metric;
    }

    public void setMetric(Map<String, Object> metric) {
        this.metric = metric;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
