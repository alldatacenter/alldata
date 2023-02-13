/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.workflow.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class WorkFlowBuildPhase implements Serializable {

    private Integer id;

    private Integer workFlowBuildHistoryId;

    /**
     * prop:1??dump 2??join 3??index_build
     */
    private Integer phase;

    private Integer result;

    private Date createTime;

    private Date opTime;

    /**
     * prop:??¼һЩ????????Ϣ??????dump?˶??????
     */
    private String phaseInfo;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWorkFlowBuildHistoryId() {
        return workFlowBuildHistoryId;
    }

    public void setWorkFlowBuildHistoryId(Integer workFlowBuildHistoryId) {
        this.workFlowBuildHistoryId = workFlowBuildHistoryId;
    }

    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) {
        this.phase = phase;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getOpTime() {
        return opTime;
    }

    public void setOpTime(Date opTime) {
        this.opTime = opTime;
    }

    /**
     * get:??¼һЩ????????Ϣ??????dump?˶??????
     */
    public String getPhaseInfo() {
        return phaseInfo;
    }

    /**
     * set:??¼һЩ????????Ϣ??????dump?˶??????
     */
    public void setPhaseInfo(String phaseInfo) {
        this.phaseInfo = phaseInfo == null ? null : phaseInfo.trim();
    }
}
