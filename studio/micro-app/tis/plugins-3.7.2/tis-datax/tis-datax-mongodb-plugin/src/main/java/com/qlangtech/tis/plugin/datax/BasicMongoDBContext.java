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

import com.qlangtech.tis.plugin.ds.mangodb.MangoDBDataSourceFactory;

import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-20 20:43
 **/
public class BasicMongoDBContext {
    protected final MangoDBDataSourceFactory dsFactory;

    public BasicMongoDBContext(MangoDBDataSourceFactory dsFactory) {
        this.dsFactory = dsFactory;
    }

    public String getServerAddress() {
        return MangoDBDataSourceFactory.getAddressList(this.dsFactory.address)
                .stream().map((address) -> "\"" + address + "\"").collect(Collectors.joining(","));
    }

    public boolean isContainCredential() {
        return this.dsFactory.isContainCredential();
    }

    public String getUserName() {
        return this.dsFactory.username;
    }

    public String getPassword() {
        return this.dsFactory.password;
    }

    public String getDbName() {
        return this.dsFactory.dbName;
    }
}
