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
public class OperationLog implements Serializable {

    private Integer opId;

    private String usrId;

    private String usrName;

    private String opType;

    private Date createTime;

    private String tabName;

    /**
     * prop:搴旂敤鍚嶇О
     */
    private String appName;

    /**
     * prop:杩愯鐜
     */
    private Short runtime;

    /**
     * prop:鍐呭澶囨敞
     */
    private String memo;

    private String opDesc;

    private static final long serialVersionUID = 1L;

    public Integer getOpId() {
        return opId;
    }

    public void setOpId(Integer opId) {
        this.opId = opId;
    }

    public String getUsrId() {
        return usrId;
    }

    public void setUsrId(String usrId) {
        this.usrId = usrId == null ? null : usrId.trim();
    }

    public String getUsrName() {
        return usrName;
    }

    public void setUsrName(String usrName) {
        this.usrName = usrName == null ? null : usrName.trim();
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType == null ? null : opType.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName == null ? null : tabName.trim();
    }

    /**
     * get:搴旂敤鍚嶇О
     */
    public String getAppName() {
        return appName;
    }

    /**
     * set:搴旂敤鍚嶇О
     */
    public void setAppName(String appName) {
        this.appName = appName == null ? null : appName.trim();
    }

    /**
     * get:杩愯鐜
     */
    public Short getRuntime() {
        return runtime;
    }

    /**
     * set:杩愯鐜
     */
    public void setRuntime(Short runtime) {
        this.runtime = runtime;
    }

    /**
     * get:鍐呭澶囨敞
     */
    public String getMemo() {
        return memo;
    }

    /**
     * set:鍐呭澶囨敞
     */
    public void setMemo(String memo) {
        this.memo = memo == null ? null : memo.trim();
    }

    public String getOpDesc() {
        return opDesc;
    }

    public void setOpDesc(String opDesc) {
        this.opDesc = opDesc == null ? null : opDesc.trim();
    }
}
