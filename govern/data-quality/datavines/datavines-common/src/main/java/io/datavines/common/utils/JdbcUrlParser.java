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
package io.datavines.common.utils;

import static io.datavines.common.CommonConstants.COLON;
import static io.datavines.common.CommonConstants.DOUBLE_SLASH;
import static io.datavines.common.CommonConstants.QUESTION;
import static io.datavines.common.CommonConstants.SEMICOLON;
import static io.datavines.common.CommonConstants.SINGLE_SLASH;

import io.datavines.common.entity.ConnectionInfo;

/**
 * JdbcUrlParser
 */
public class JdbcUrlParser {

    private JdbcUrlParser() {
        throw new IllegalStateException("Utility class");
    }

    public static ConnectionInfo getConnectionInfo(String jdbcUrl,String username, String password) {

        ConnectionInfo connectionInfo = new ConnectionInfo();

        int pos;
        int pos1;
        int pos2;
        String tempUri;

        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:") || (pos1 = jdbcUrl.indexOf(COLON, 5)) == -1) {
            return null;
        }

        String type = jdbcUrl.substring(5, pos1);
        String params = "";
        String host = "";
        String database = "";
        String port = "";
        if (((pos2 = jdbcUrl.indexOf(SEMICOLON, pos1)) == -1) && ((pos2 = jdbcUrl.indexOf(QUESTION, pos1)) == -1)) {
            tempUri = jdbcUrl.substring(pos1 + 1);
        } else {
            tempUri = jdbcUrl.substring(pos1 + 1, pos2);
            params = jdbcUrl.substring(pos2 + 1);
        }

        if (tempUri.startsWith(DOUBLE_SLASH)) {
            if ((pos = tempUri.indexOf(SINGLE_SLASH, 2)) != -1) {
                host = tempUri.substring(2, pos);
                database = tempUri.substring(pos + 1);

                if ((pos = host.indexOf(COLON)) != -1) {
                    port = host.substring(pos + 1);
                    host = host.substring(0, pos);
                }
            }
        } else {
            database = tempUri;
        }

        if (StringUtils.isEmpty(database)) {
            return null;
        }

        if (database.contains(QUESTION)) {
            database = database.substring(0, database.indexOf(QUESTION));
        }

        if (database.contains(SEMICOLON)) {
            database = database.substring(0, database.indexOf(SEMICOLON));
        }

        connectionInfo.setType(type);
        connectionInfo.setUrl(jdbcUrl);
        connectionInfo.setHost(host);
        connectionInfo.setPort(port);
        connectionInfo.setDatabase(database);
        connectionInfo.setParams(params);
        connectionInfo.setAddress("jdbc:" + type + "://" + host + COLON + port);
        connectionInfo.setUsername(username);
        connectionInfo.setPassword(password);

        return connectionInfo;
    }
}
