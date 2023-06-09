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
package com.qlangtech.tis.offline.pojo;

import com.qlangtech.tis.git.GitUtils;
import com.qlangtech.tis.manage.common.Secret;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TISDb {

    String dbId;

    String dbName;

    String dbType;

    String userName;

    String password;

    String port;

    String encoding;

    String extraParams;

    String shardingType;

    String shardingEnum;

    String host;

    // 是否是cobar类型的
    private boolean facade;

    public String createDBConfigDesc() {
        StringBuffer desc = new StringBuffer();
        // mysql order {
        // host:127.0.0.1[00-31],127.0.0.2[32-63],127.0.0.3,127.0.0.4[9],baisui.com[0-9]
        // username:root
        // password:root@123%&*())))**
        // port:3306
        // }
        // Secret
        desc.append("mysql ").append(this.dbName).append(" { \n");
        desc.append(" host:").append(this.getHost()).append(" \n");
        desc.append(" username:").append(this.getUserName()).append(" \n");
        desc.append(" password:").append(Secret.encrypt(this.password, GitUtils.cryptKey)).append(" \n");
        desc.append(" port:").append(this.getPort()).append("\n");
        desc.append("}");
        return desc.toString();
    }

    // 是否是cobar类型的
    public boolean isFacade() {
        return this.facade;
    }

    public void setFacade(boolean facade) {
        this.facade = facade;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(String extraParams) {
        this.extraParams = extraParams;
    }

    public String getShardingType() {
        return shardingType;
    }

    public void setShardingType(String shardingType) {
        this.shardingType = shardingType;
    }

    public String getShardingEnum() {
        return shardingEnum;
    }

    public void setShardingEnum(String shardingEnum) {
        this.shardingEnum = shardingEnum;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    public String getDbId() {
        return this.dbId;
    }
}
