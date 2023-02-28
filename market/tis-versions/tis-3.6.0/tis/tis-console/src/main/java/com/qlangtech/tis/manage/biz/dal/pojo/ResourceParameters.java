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
package com.qlangtech.tis.manage.biz.dal.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ResourceParameters implements Serializable {

    private Long rpId;

    private String keyName;

    private String dailyValue;

    private String readyValue;

    private String onlineValue;

    public String value;

    public void setValue(String value) {
        this.value = value;
    }

    private Date gmtCreate;

    private Date gmtUpdate;

    private static final long serialVersionUID = 1L;

    public Long getRpId() {
        return rpId;
    }

    public void setRpId(Long rpId) {
        this.rpId = rpId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName == null ? null : keyName.trim();
    }

    public String getDailyValue() {
        return dailyValue;
    }

    public void setDailyValue(String dailyValue) {
        this.dailyValue = dailyValue == null ? null : dailyValue.trim();
    }

    public String getReadyValue() {
        return readyValue;
    }

    public void setReadyValue(String readyValue) {
        this.readyValue = readyValue == null ? null : readyValue.trim();
    }

    public String getOnlineValue() {
        return onlineValue;
    }

    public void setOnlineValue(String onlineValue) {
        this.onlineValue = onlineValue == null ? null : onlineValue.trim();
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtUpdate() {
        return gmtUpdate;
    }

    public void setGmtUpdate(Date gmtUpdate) {
        this.gmtUpdate = gmtUpdate;
    }
}
