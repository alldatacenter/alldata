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
package io.datavines.common.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ConnectionInfo
 */
@Data
public class ConnectionInfo {

    private String type;

    private String url;

    private String host;

    private String port;

    private String driverName;

    private String catalog;

    private String database;

    private String params;

    private String address;

    private String username;

    private String password;

    @Override
    public String toString() {
        return "JdbcInfo{"
                + "host='" + host + '\''
                + ", port='" + port + '\''
                + ", driverName='" + driverName + '\''
                + ", database='" + database + '\''
                + ", params='" + params + '\''
                + ", address='" + address + '\''
                + '}';
    }

    public Map<String, Object> configMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("database",database);
        configMap.put("password",password);
        configMap.put("host",host);
        configMap.put("port",port);
        configMap.put("user",username);
        configMap.put("properties",params);
        configMap.put("catalog", catalog);
        return configMap;
    }

    public void setConfig(Map<String, Object> configMap) {
        this.database = (String)configMap.get("database");
        this.password = (String)configMap.get("password");
        this.host = (String)configMap.get("host");
        this.port = (String)configMap.get("port");
        this.username = (String)configMap.get("user");
        this.params = (String)configMap.get("properties");
        this.catalog = (String)configMap.get("catalog");
    }
}
