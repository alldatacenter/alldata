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

package org.apache.inlong.manager.service.resource.sink.ck;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/**
 * Clickhouse config information, including url, user, etc.
 */
@Component
public class ClickHouseConfig {

    private static volatile DataSource source;

    private static String jdbcUrl;

    private static String username;

    private static String password;

    @Value("${audit.ck.jdbcUrl}")
    public void setUrl(String jdbcUrl) {
        ClickHouseConfig.jdbcUrl = jdbcUrl;
    }

    @Value("${audit.ck.username}")
    public void setUsername(String username) {
        ClickHouseConfig.username = username;
    }

    @Value("${audit.ck.password}")
    public void setPassword(String password) {
        ClickHouseConfig.password = password;
    }

    /**
     * Get ClickHouse connection from data source
     */
    public static Connection getCkConnection() throws Exception {
        if (source == null) {
            synchronized (ClickHouseConfig.class) {
                if (source == null) {
                    Properties pros = new Properties();
                    pros.put("url", jdbcUrl);
                    pros.put("username", username);
                    pros.put("password", password);
                    source = DruidDataSourceFactory.createDataSource(pros);
                }
            }
        }
        return source.getConnection();
    }
}
