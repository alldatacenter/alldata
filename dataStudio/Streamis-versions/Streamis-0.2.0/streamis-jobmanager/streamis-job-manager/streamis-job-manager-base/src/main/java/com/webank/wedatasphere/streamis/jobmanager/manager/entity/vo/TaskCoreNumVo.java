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

/**
 * job核心指标
 */
public class TaskCoreNumVo {
    private Long projectId;
    private String projectName;
    //失败任务数目
    private Integer failureNum = 0;
    //运行数目
    private Integer runningNum = 0;
    //慢任务数目
    private Integer slowTaskNum = 0;
    //告警任务
    private Integer alertNum = 0;
    //等待重启数目
    private Integer waitRestartNum = 0;
    //已完成数目
    private Integer successNum = 0;
    //已停止数目
    private Integer stoppedNum = 0;

    public Integer getStoppedNum() {
        return stoppedNum;
    }

    public void setStoppedNum(Integer stoppedNum) {
        this.stoppedNum = stoppedNum;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getFailureNum() {
        return failureNum;
    }

    public void setFailureNum(Integer failureNum) {
        this.failureNum = failureNum;
    }

    public Integer getRunningNum() {
        return runningNum;
    }

    public void setRunningNum(Integer runningNum) {
        this.runningNum = runningNum;
    }

    public Integer getSlowTaskNum() {
        return slowTaskNum;
    }

    public void setSlowTaskNum(Integer slowTaskNum) {
        this.slowTaskNum = slowTaskNum;
    }

    public Integer getAlertNum() {
        return alertNum;
    }

    public void setAlertNum(Integer alertNum) {
        this.alertNum = alertNum;
    }

    public Integer getWaitRestartNum() {
        return waitRestartNum;
    }

    public void setWaitRestartNum(Integer waitRestartNum) {
        this.waitRestartNum = waitRestartNum;
    }

    public Integer getSuccessNum() {
        return successNum;
    }

    public void setSuccessNum(Integer successNum) {
        this.successNum = successNum;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
