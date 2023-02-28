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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-09 16:08
 **/
public class ClickHouseWriterContext implements IDataxContext {
    private IDataxProcessor.TabCols cols;
    private String password;
    private String username;
    private String table;
    private String jdbcUrl;
    private Integer batchSize;
    private Integer batchByteSize;
    private String writeMode;

    private String dataXName;

    private String preSql;
    private String postSql;

    public String getDataXName() {
        return dataXName;
    }

    public void setDataXName(String dataXName) {
        this.dataXName = dataXName;
    }

    public boolean isContainPreSql() {
        return StringUtils.isNotEmpty(this.preSql);
    }

    public boolean isContainPostSql() {
        return StringUtils.isNotEmpty(this.postSql);
    }

    public String getPreSql() {
        return preSql;
    }

    public void setPreSql(String preSql) {
        this.preSql = preSql;
    }

    public String getPostSql() {
        return postSql;
    }

    public void setPostSql(String postSql) {
        this.postSql = postSql;
    }

    public IDataxProcessor.TabCols getCols() {
        return cols;
    }

    public void setCols(IDataxProcessor.TabCols cols) {
        this.cols = cols;
    }

    public boolean isContainPassword() {
        return StringUtils.isNotEmpty(this.password);
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public boolean isContainBatchSize() {
        return this.batchSize != null;
    }

    public boolean isContainBatchByteSize() {
        return this.batchByteSize != null;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getBatchByteSize() {
        return batchByteSize;
    }

    public void setBatchByteSize(Integer batchByteSize) {
        this.batchByteSize = batchByteSize;
    }

    public String getWriteMode() {
        return writeMode;
    }

    public void setWriteMode(String writeMode) {
        this.writeMode = writeMode;
    }

    public boolean isContainWriteMode() {
        return StringUtils.isNotEmpty(this.writeMode);
    }
}
