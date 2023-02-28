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
package io.datavines.engine.local.api.utils;

import io.datavines.common.config.Config;
import io.datavines.common.utils.StringUtils;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionUtils {

    public static Connection getConnection(Config config) {
        Logger logger = LoggerFactory.getLogger(ConnectionUtils.class);
        String driver = config.getString("driver");
        String url = config.getString("url");
        String username = config.getString("user");
        String password = config.getString("password");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException exception) {
            logger.error("load driver error: " + exception.getLocalizedMessage());
        }

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(url, username, StringUtils.isEmpty(password) ? null : password);
        } catch (SQLException exception) {
            logger.error("get connection error: " + exception.getLocalizedMessage());
        }

        logger.info("create connection success : {}", url + "[username=" + username + "]");

        return connection;
    }
}
