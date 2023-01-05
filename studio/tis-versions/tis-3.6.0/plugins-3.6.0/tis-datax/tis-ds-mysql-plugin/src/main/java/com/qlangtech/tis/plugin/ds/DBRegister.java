/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.ds;

/**
 * @author: baisui 百岁
 * @create: 2020-12-08 13:52
 **/
public abstract class DBRegister {

    // 由于facade的dbname会和detail的不一样，所以需要额外在加一个供注册到spring datasource中作为id用
    private final String dbName;

    private final DBConfig dbConfig;

    public DBRegister(String dbName, DBConfig dbConfig) {
        this.dbName = dbName;
        this.dbConfig = dbConfig;
    }

    protected abstract void createDefinition(String dbDefinitionId, String driverClassName, String jdbcUrl);

    /**
     * 读取多个数据源中的一个一般是用于读取数据源Meta信息用
     */
    public void visitFirst() {
        this.setApplicationContext(false, true);
    }

    /**
     * 读取所有可访问的数据源
     */
    public void visitAll() {
        this.setApplicationContext(true, false);
    }

    private void setApplicationContext(boolean resolveHostIp, boolean facade) {
        this.dbConfig.vistDbURL(resolveHostIp, (dbName, hostName, jdbcUrl) -> {
            final String dbDefinitionId = (facade ? DBRegister.this.dbName : dbName);
            createDefinition(dbDefinitionId, "com.mysql.jdbc.Driver", jdbcUrl);
        }, facade);
    }
}
