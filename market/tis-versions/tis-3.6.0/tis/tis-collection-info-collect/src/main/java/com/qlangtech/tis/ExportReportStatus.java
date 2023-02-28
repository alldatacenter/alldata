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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by work on 15-1-12.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ExportReportStatus {

    private String serviceName;

    private Date shouldTriggerTime;

    private Map<Integer, List<DumpReportInfo>> hostsStatus = new HashMap<Integer, List<DumpReportInfo>>();

    public ExportReportStatus(String serviceName) {
        this.serviceName = serviceName;
    }

    public ExportReportStatus(String serviceName, Date shouldTriggerTime, Map<Integer, List<DumpReportInfo>> hostsStatus) {
        this.serviceName = serviceName;
        this.shouldTriggerTime = shouldTriggerTime;
        this.hostsStatus = hostsStatus;
    }

    public int getHostsSize() {
        int size = 0;
        for (Map.Entry<Integer, List<DumpReportInfo>> entry : hostsStatus.entrySet()) {
            if (entry.getKey() != null) {
                size += entry.getValue().size();
            }
        }
        return size;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Date getShouldTriggerTime() {
        return shouldTriggerTime;
    }

    public void setShouldTriggerTime(Date shouldTriggerTime) {
        this.shouldTriggerTime = shouldTriggerTime;
    }

    public Map<Integer, List<DumpReportInfo>> getHostsStatus() {
        return hostsStatus;
    }

    public void setHostsStatus(Map<Integer, List<DumpReportInfo>> hostsStatus) {
        this.hostsStatus = hostsStatus;
    }
}
