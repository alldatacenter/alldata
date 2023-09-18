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
import io.datavines.common.datasource.jdbc.JdbcDataSourceManager;
import io.datavines.common.exception.DataVinesException;

import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionUtils {

    public static Connection getConnection(Config config) throws DataVinesException {
        Logger logger = LoggerFactory.getLogger(ConnectionUtils.class);
        try {
            DataSource dataSource = JdbcDataSourceManager.getInstance().getDataSource(config.configMap());
            if (dataSource != null) {
                logger.info("get connection success : {}", config.getString("url") + "[username=" + config.getString("user") + "]");
                return dataSource.getConnection();
            } else {
                logger.error("get datasource error");
                throw new DataVinesException("can not get datasource");
            }
        } catch (SQLException exception) {
            logger.error("get connection error :", exception);
            throw new DataVinesException(exception);
        }
    }
}
