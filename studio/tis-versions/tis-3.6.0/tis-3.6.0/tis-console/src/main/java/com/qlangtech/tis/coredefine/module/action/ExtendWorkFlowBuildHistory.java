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
package com.qlangtech.tis.coredefine.module.action;

import com.alibaba.fastjson.annotation.JSONField;
import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.assemble.TriggerType;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年7月29日
 */
public class ExtendWorkFlowBuildHistory {

  private final WorkFlowBuildHistory delegate;

  public ExtendWorkFlowBuildHistory(WorkFlowBuildHistory delegate) {
    super();
    this.delegate = delegate;
  }

  /**
   * 耗时
   *
   * @return
   */
  public String getConsuming() {
    ExecResult result = ExecResult.parse(this.delegate.getState());
    Date endTime = ((result == ExecResult.FAILD || result == ExecResult.SUCCESS) && this.getEndTime() != null) ? this.getEndTime() : new Date();
    int consuming = (int) ((endTime.getTime() - this.getStartTime().getTime()) / 1000);
    if (consuming < 60) {
      return consuming + "秒";
    } else {
      return (consuming / 60) + "分钟";
    }
  }

  public Integer getId() {
    return delegate.getId();
  }

  public Date getStartTime() {
    return delegate.getStartTime();
  }

  @JSONField(serialize = false)
  public WorkFlowBuildHistory getDelegate() {
    return this.delegate;
  }

  public static void main(String[] args) {
    SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    long t1 = 1599460480838l;
    long t2 = 1599450043000l;
    System.out.println(f.format(new Date(t1)));
    System.out.println(f.format(new Date(t2)));
    long diff = t1 - t2;
    System.out.println(diff / (1000 * 60 * 60) + "小时");
    System.out.println((diff / 1000) % (60) + "秒");
    System.out.println((diff / (1000 * 60)) % (60) + "分");
    Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    System.out.println(gmt.getTimeInMillis());
    gmt = Calendar.getInstance();
    System.out.println(gmt.getTimeInMillis());
    // ExtendWorkFlowBuildHistory test = new ExtendWorkFlowBuildHistory(null);
    // System.out.println(test.getNow());
  }


  public Date getEndTime() {
    return delegate.getEndTime();
  }

  public String getStateClass() {
    // SUCCESS(1, "成功"), FAILD(-1, "失败"), DOING(2, "执行中"), CANCEL(3, "终止");
    ExecResult result = ExecResult.parse(this.delegate.getState());
    switch (result) {
      case DOING:
      case ASYN_DOING:
        return "loading";
      case SUCCESS:
        return "check";
      case FAILD:
        return "close";
      case CANCEL:
        // 取消了
        return "stop";
      default:
        throw new IllegalStateException("result:" + result + " status is illegal");
    }
  }

  public String getStateColor() {
    // [ngStyle]="{'max-width.px': widthExp}"
    ExecResult result = ExecResult.parse(this.delegate.getState());
    switch (result) {
      case DOING:
      case ASYN_DOING:
        return "blue";
      case SUCCESS:
        return "green";
      case FAILD:
        return "red";
      case CANCEL:
        // 取消了
        return "red";
      default:
        throw new IllegalStateException("result:" + result + " status is illegal");
    }
  }

  public int getState() {
    return delegate.getState();
  }

  /**
   * 取得执状态
   *
   * @return
   */
  public String getLiteralState() {
    return ExecResult.parse(this.delegate.getState()).getLiteral();
  }

  public String getTriggerType() {
    return TriggerType.parse(delegate.getTriggerType()).getLiteral();
  }

  public Integer getOpUserId() {
    return delegate.getOpUserId();
  }

  public String getOpUserName() {
    return delegate.getOpUserName();
  }

  public Integer getAppId() {
    return delegate.getAppId();
  }

  public String getAppName() {
    return delegate.getAppName();
  }

  public String getStartPhase() {
    return FullbuildPhase.parse(delegate.getStartPhase()).getLiteral();
  }

  public int getStartPhaseNum() {
    return delegate.getStartPhase();
  }

  public int getEndPhaseNum() {
    return delegate.getEndPhase();
  }

  public String getEndPhase() {
    return FullbuildPhase.parse(delegate.getEndPhase()).getLiteral();
  }

  public Integer getHistoryId() {
    return delegate.getHistoryId();
  }

  public Integer getWorkFlowId() {
    return delegate.getWorkFlowId();
  }

  public long getCreateTime() {
    return delegate.getCreateTime().getTime();
  }

  public Date getOpTime() {
    return delegate.getOpTime();
  }
}
