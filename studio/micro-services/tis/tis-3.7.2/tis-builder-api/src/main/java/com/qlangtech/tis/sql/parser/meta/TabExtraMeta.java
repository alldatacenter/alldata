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
package com.qlangtech.tis.sql.parser.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TabExtraMeta {

    // ermeta.getBoolean("primaryIndexTab");
    // ermeta.getBoolean("monitorTrigger");
    // 是否是索引主表
    private boolean primaryIndexTab;

    private List<PrimaryLinkKey> primaryIndexColumnNames;

    // 主表的分区键，一般用于在构建宽表过程给数据分区用
    private String sharedKey;

    // 是否监听增量消息
    private boolean monitorTrigger;

    // 表中对应的增量时间戳对应的字段
    private String timeVerColName;

    public String getTimeVerColName() {
        return this.timeVerColName;
    }

    public void setTimeVerColName(String timeVerColName) {
        this.timeVerColName = timeVerColName;
    }

    private List<ColumnTransfer> colTransfers = new ArrayList<>();

    public List<ColumnTransfer> getColTransfers() {
        return colTransfers;
    }

    /**
     * colKey
     */
    private Map<String, ColumnTransfer> colTransfersMap;

    public void addColumnTransfer(ColumnTransfer colTransfer) {
        this.colTransfers.add(colTransfer);
    }

    public List<PrimaryLinkKey> getPrimaryIndexColumnNames() {
        return this.primaryIndexColumnNames;
    }

    public void setPrimaryIndexColumnNames(List<PrimaryLinkKey> primaryIndexColumnName) {
        if (primaryIndexColumnName == null || primaryIndexColumnName.isEmpty()) {
            throw new IllegalArgumentException("param primaryIndexColumnName can not be empty");
        }
        this.primaryIndexColumnNames = primaryIndexColumnName;
    }

    public void setColTransfers(List<ColumnTransfer> colTransfers) {
        this.colTransfers = colTransfers;
    }

    public boolean isPrimaryIndexTab() {
        return this.primaryIndexTab;
    }

    public void setPrimaryIndexTab(boolean primaryIndexTab) {
        this.primaryIndexTab = primaryIndexTab;
    }

    public boolean isMonitorTrigger() {
        return monitorTrigger;
    }

    public void setMonitorTrigger(boolean monitorTrigger) {
        this.monitorTrigger = monitorTrigger;
    }

    public String getSharedKey() {
        return this.sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }
}
