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

import com.qlangtech.tis.datax.IDataxProcessor;
import org.apache.commons.lang.StringUtils;

/**
 * @author: baisui 百岁
 * @create: 2021-04-08 11:06
 **/
public class RdbmsDataxContext {
    private final String dataXName;
    public IDataxProcessor.TabCols cols;
    String tabName;
    String password;
    String username;
    String jdbcUrl;

    public RdbmsDataxContext(String dataXName) {
        if (StringUtils.isEmpty(dataXName)) {
            throw new IllegalArgumentException("param dataXName:" + dataXName + " can not be null");
        }
        this.dataXName = dataXName;
    }

    public String getDataXName() {
        return this.dataXName;
    }

    public String getColsQuotes() {
        return cols.getColsQuotes();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setJdbcUrl(String jdbcUrl) {
        if (StringUtils.isEmpty(jdbcUrl)) {
            throw new IllegalArgumentException("param jdbcUrl can not be empty");
        }
        this.jdbcUrl = jdbcUrl;
    }

    public String getTabName() {
        return this.tabName;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getJdbcUrl() {

        return jdbcUrl;
    }
}
