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
package com.qlangtech.tis;

import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年2月15日
 */
public class DumpReportInfo {

    private String hostname;

    private Date lastSucTriggerTime;

    public DumpReportInfo(String hostname, Date lastSucTriggerTime) {
        this.hostname = hostname;
        this.lastSucTriggerTime = lastSucTriggerTime;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Date getLastSucTriggerTime() {
        return lastSucTriggerTime;
    }

    public void setLastSucTriggerTime(Date lastSucTriggerTime) {
        this.lastSucTriggerTime = lastSucTriggerTime;
    }
}
