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
package io.datavines.common.datasource.jdbc;

import io.datavines.common.utils.Md5Utils;
import io.datavines.common.utils.StringUtils;
import lombok.Data;

@Data
public class JdbcConnectionInfo {

    /**
     * user
     */
    protected String user;

    /**
     * user password
     */
    protected String password;

    /**
     * data source address
     */
    private String host;

    /**
     * datasource port
     */
    private String port;

    /**
     * catalog name
     */
    private String catalog;

    /**
     * database(schema) name
     */
    private String database;

    /**
     * properties
     */
    private String properties;

    @Override
    public String toString() {
        return host.trim() +
                "&" + port +
                "&" + getOrEmpty(catalog) +
                "&" + getOrEmpty(database) +
                "&" + getOrEmpty(user) +
                "&" + getOrEmpty(password) +
                "&" + properties;
    }

    private String getOrEmpty (String keyword) {
        return StringUtils.isNotEmpty(keyword)? keyword.trim():"";
    }

    public String getUniqueKey() {
        return Md5Utils.getMd5(toString(), false);
    }
}
