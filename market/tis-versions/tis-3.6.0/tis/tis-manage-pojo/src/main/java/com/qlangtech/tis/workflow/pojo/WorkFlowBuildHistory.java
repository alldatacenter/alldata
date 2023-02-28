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
public class WorkFlowBuildHistory implements Serializable {

  private Integer id;

  private Date startTime;

  private Date endTime;

  private Byte state;

  private Integer triggerType;

  private Integer opUserId;

  private String opUserName;

  private Integer appId;

  private String appName;

  private Integer lastVer;

  /**
   * 异步任务状态描述
   * {
   * taskName1:{complete:bool,success:bool},
   * }
   */
  private String asynSubTaskStatus;

  /**
   * prop:1:dump 2:join 3 build 4 backflow
   */
  private Integer startPhase;

  private Integer endPhase;

  private Integer historyId;

  private Integer workFlowId;

  private Date createTime;

  private Date opTime;

  private static final long serialVersionUID = 1L;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }


  public Integer getLastVer() {
    return lastVer;
  }

  public void setLastVer(Integer lastVer) {
    this.lastVer = lastVer;
  }

  public String getAsynSubTaskStatus() {
    return asynSubTaskStatus;
  }

  public void setAsynSubTaskStatus(String asynSubTaskStatus) {
    this.asynSubTaskStatus = asynSubTaskStatus;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public Byte getState() {
    return state;
  }

  public void setState(Byte state) {
    this.state = state;
  }

  public Integer getTriggerType() {
    return triggerType;
  }

  public void setTriggerType(Integer triggerType) {
    this.triggerType = triggerType;
  }

  public Integer getOpUserId() {
    return opUserId;
  }

  public void setOpUserId(Integer opUserId) {
    this.opUserId = opUserId;
  }

  public String getOpUserName() {
    return opUserName;
  }

  public void setOpUserName(String opUserName) {
    this.opUserName = opUserName == null ? null : opUserName.trim();
  }

  public Integer getAppId() {
    return appId;
  }

  public void setAppId(Integer appId) {
    this.appId = appId;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName == null ? null : appName.trim();
  }

  /**
   * get:1:dump 2:join 3 build 4 backflow
   */
  public Integer getStartPhase() {
    return startPhase;
  }

  /**
   * set:1:dump 2:join 3 build 4 backflow
   */
  public void setStartPhase(Integer startPhase) {
    this.startPhase = startPhase;
  }

  public Integer getHistoryId() {
    return historyId;
  }

  public void setHistoryId(Integer historyId) {
    this.historyId = historyId;
  }

  public Integer getWorkFlowId() {
    return workFlowId;
  }

  public void setWorkFlowId(Integer workFlowId) {
    this.workFlowId = workFlowId;
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

  public Integer getEndPhase() {
    return endPhase;
  }

  public void setEndPhase(Integer endPhase) {
    this.endPhase = endPhase;
  }
}
