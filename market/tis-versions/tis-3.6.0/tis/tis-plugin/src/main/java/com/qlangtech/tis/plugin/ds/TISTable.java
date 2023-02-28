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
package com.qlangtech.tis.plugin.ds;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * git仓库保存的table信息
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TISTable {

    private String tableName;

    private int partitionNum;

    private Integer dbId;

    private Integer tabId;

    private String dbName;

    private int partitionInterval;

    private String selectSql;

    private List<ColumnMetaData> reflectCols = Lists.newArrayList();

    public TISTable() {
    }

    public void addColumnMeta(ColumnMetaData colMeta) {
        this.reflectCols.add(colMeta);
    }

    public List<ColumnMetaData> getReflectCols() {
        return reflectCols;
    }

    public void setReflectCols(List<ColumnMetaData> reflectCols) {
        this.reflectCols = reflectCols;
    }

    public TISTable(String tableName, int partitionNum
            , Integer dbId, int partitionInterval, String selectSql) {
        this.tableName = tableName;
        this.partitionNum = partitionNum;
        this.dbId = dbId;
        this.partitionInterval = partitionInterval;
        this.selectSql = selectSql;
    }

    public Integer getTabId() {
        return tabId;
    }

    public void setTabId(Integer tabId) {
        this.tabId = tabId;
    }

    public String getDbName() {
        if (StringUtils.isEmpty(this.dbName)) {
            throw new IllegalStateException("param db name can not be null");
        }
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getPartitionNum() {
        return partitionNum;
    }

    public void setPartitionNum(int partitionNum) {
        this.partitionNum = partitionNum;
    }

    public Integer getDbId() {
        return this.dbId;
    }

    public void setDbId(Integer dbId) {
        this.dbId = dbId;
    }

    public int getPartitionInterval() {
        return partitionInterval;
    }

    public void setPartitionInterval(int partitionInterval) {
        this.partitionInterval = partitionInterval;
    }

    public String getSelectSql() {
        return selectSql;
    }

    public void setSelectSql(String selectSql) {
        this.selectSql = selectSql;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TISTable tisTable = (TISTable) o;
        return Objects.equals(tableName, tisTable.tableName) &&
                Objects.equals(tabId, tisTable.tabId) &&
                Objects.equals(dbName, tisTable.dbName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, tabId, dbName);
    }


    @Override
    public String toString() {
        return "TISTable{" +
                "tableName='" + tableName + '\'' +
                ", tabId=" + tabId +
                ", dbName='" + dbName + '\'' +
                '}';
    }
}
