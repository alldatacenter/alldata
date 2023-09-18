/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.connector.plugin;

import io.datavines.common.datasource.jdbc.BaseJdbcDataSourceInfo;
import io.datavines.common.datasource.jdbc.JdbcConnectionInfo;

public class MysqlDataSourceInfo extends BaseJdbcDataSourceInfo {

    public MysqlDataSourceInfo(JdbcConnectionInfo jdbcConnectionInfo) {
        super(jdbcConnectionInfo);
    }

    @Override
    public String getAddress() {
        return "jdbc:mysql://"+getHost()+":"+getPort();
    }

    @Override
    public String getDriverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String getType() {
        return "mysql";
    }

    @Override
    protected String getSeparator() {
        return "?";
    }

    @Override
    protected String filterProperties(String other){
        return other;
    }
}
