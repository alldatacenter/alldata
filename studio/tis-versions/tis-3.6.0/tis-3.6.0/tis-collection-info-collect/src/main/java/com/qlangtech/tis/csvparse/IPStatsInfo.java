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
package com.qlangtech.tis.csvparse;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-3-8
 */
public class IPStatsInfo {

    final String ipAddress;

    private String hostName;

    private final String coreName;

    public IPStatsInfo(String ipAddress, String coreName) {
        super();
        this.ipAddress = StringUtils.trimToEmpty(ipAddress);
        this.coreName = coreName;
    }

    private final Set<CoreGroup> groupSet = new HashSet<CoreGroup>();

    public Set<CoreGroup> getGroupSet() {
        return groupSet;
    }

    /**
     * @return
     */
    public long getIndexSize() {
        long result = 0;
        for (CoreGroup group : groupSet) {
            result += group.getIndexSize();
        }
        return result;
    }

    public void addGroup(CoreGroup group) {
        groupSet.add(group);
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     */
    private float maxLoad5Min;

    public float getMaxLoad5Min() {
        return maxLoad5Min;
    }

    public void setMaxLoad5Min(float maxLoad5Min) {
        this.maxLoad5Min = maxLoad5Min;
    }

    private Date date;

    private long used;

    private long available;

    private String uedPercent;

    public String getIpAddress() {
        return ipAddress;
    }

    public String getCoreName() {
        return coreName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public long getAvailable() {
        return available;
    }

    public void setAvailable(long available) {
        this.available = available;
    }

    public String getUedPercent() {
        return uedPercent;
    }

    public void setUedPercent(String uedPercent) {
        this.uedPercent = uedPercent;
    }
}
