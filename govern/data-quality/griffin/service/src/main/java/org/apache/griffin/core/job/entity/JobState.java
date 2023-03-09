/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Encapsulating job scheduler state to reduce job startup and stop logical
 * processing
 */
public class JobState {

    /**
     * job scheduler state
     */
    private String state;

    /**
     * whether job can be started
     */
    private boolean toStart = false;

    /**
     * whether job can be stopped
     */
    private boolean toStop = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long nextFireTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long previousFireTime;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isToStart() {
        return toStart;
    }

    public void setToStart(boolean toStart) {
        this.toStart = toStart;
    }

    public boolean isToStop() {
        return toStop;
    }

    public void setToStop(boolean toStop) {
        this.toStop = toStop;
    }

    public Long getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Long nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public Long getPreviousFireTime() {
        return previousFireTime;
    }

    public void setPreviousFireTime(Long previousFireTime) {
        this.previousFireTime = previousFireTime;
    }
}
