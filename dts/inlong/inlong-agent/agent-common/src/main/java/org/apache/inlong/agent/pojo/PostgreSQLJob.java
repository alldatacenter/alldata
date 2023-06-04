/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.pojo;

import lombok.Data;

import java.util.List;

/**
 * PostgreSQL Job info
 */
@Data
public class PostgreSQLJob {

    private String user;
    private String password;
    private String hostname;
    private String port;
    private String dbname;
    private String schema;
    private String servername;
    private String pluginname;
    private List<String> tableNameList;
    private String serverTimeZone;
    private String scanStartupMode;
    private String primaryKey;

    @Data
    public static class PostgreSQLJobConfig {

        private String username;
        private String password;
        private String hostname;
        private String port;
        private String database;
        private String schema;
        private String decodingPluginName;
        private List<String> tableNameList;
        private String serverTimeZone;
        private String scanStartupMode;
        private String primaryKey;
    }
}
