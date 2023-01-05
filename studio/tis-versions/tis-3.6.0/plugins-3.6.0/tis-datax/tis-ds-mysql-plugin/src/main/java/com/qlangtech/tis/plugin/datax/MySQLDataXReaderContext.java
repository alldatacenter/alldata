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

import com.qlangtech.tis.plugin.datax.common.RdbmsReaderContext;
import org.apache.commons.lang.StringUtils;

/**
 * @author: baisui 百岁
 * @create: 2021-04-20 17:42
 **/
public class MySQLDataXReaderContext extends RdbmsReaderContext {
    private final RdbmsDataxContext rdbmsContext;

    public MySQLDataXReaderContext(String name, String sourceTableName, RdbmsDataxContext mysqlContext) {
        super(name, sourceTableName, null, null);
        this.rdbmsContext = mysqlContext;
    }

    public String getDataXName() {
        return rdbmsContext.getDataXName();
    }

    public String getTabName() {
        return rdbmsContext.getTabName();
    }

    public String getPassword() {
        return rdbmsContext.getPassword();
    }

    public String getUsername() {
        return rdbmsContext.getUsername();
    }

    public String getJdbcUrl() {
        if (StringUtils.isEmpty(rdbmsContext.getJdbcUrl())) {
            throw new NullPointerException("rdbmsContext.getJdbcUrl() can not be empty");
        }
        return rdbmsContext.getJdbcUrl();
    }


//    public boolean isContainWhere() {
//        return StringUtils.isNotBlank(this.where);
//    }
//
//    public String getWhere() {
//        return where;
//    }
//
//    public void setWhere(String where) {
//        this.where = where;
//    }


}
