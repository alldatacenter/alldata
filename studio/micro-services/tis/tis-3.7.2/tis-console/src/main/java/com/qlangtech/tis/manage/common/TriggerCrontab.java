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
package com.qlangtech.tis.manage.common;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-25
 */
public class TriggerCrontab {

    private String appName;

    private Integer appId;

    private Integer fjobId;

    private Integer fjobType;

    private String fcrontab;

    // 该定时任务是否已经停止？
    private boolean fstop;

    private Integer ijobId;

    private Integer ijobType;

    private String icrontab;

    // 该定时任务是否已经停止？
    private boolean istop;

    public boolean isFstop() {
        return fstop;
    }

    public void setFstop(boolean fstop) {
        this.fstop = fstop;
    }

    public boolean isIstop() {
        return istop;
    }

    public void setIstop(boolean istop) {
        this.istop = istop;
    }

    public Integer getFjobId() {
        return fjobId;
    }

    public void setFjobId(Integer fjobId) {
        this.fjobId = fjobId;
    }

    public Integer getFjobType() {
        return fjobType;
    }

    public void setFjobType(Integer fjobType) {
        this.fjobType = fjobType;
    }

    public String getFcrontab() {
        return fcrontab;
    }

    public void setFcrontab(String fcrontab) {
        this.fcrontab = fcrontab;
    }

    public Integer getIjobId() {
        return ijobId;
    }

    public void setIjobId(Integer ijobId) {
        this.ijobId = ijobId;
    }

    public Integer getIjobType() {
        return ijobType;
    }

    public void setIjobType(Integer ijobType) {
        this.ijobType = ijobType;
    }

    public String getIcrontab() {
        return icrontab;
    }

    public void setIcrontab(String icrontab) {
        this.icrontab = icrontab;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }
}
