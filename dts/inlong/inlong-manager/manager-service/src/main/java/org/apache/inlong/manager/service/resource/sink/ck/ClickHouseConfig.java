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

import org.apache.inlong.manager.dao.entity.AuditSourceEntity;
import org.apache.inlong.manager.dao.mapper.AuditSourceEntityMapper;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.Objects;
import java.util.Properties;

/**
 * ClickHouse config, including url, user, etc.
 */
@Slf4j
@Service
public class ClickHouseConfig {

    @Autowired
    private AuditSourceEntityMapper auditSourceMapper;
    private static volatile DataSource source;
    private static volatile String currentJdbcUrl = null;
    private static volatile String currentUsername = null;
    private static volatile String currentPassword = null;

    /**
     * Update the runtime config of ClickHouse connection.
     */
    public synchronized void updateRuntimeConfig() {
        try {
            AuditSourceEntity auditSource = auditSourceMapper.selectOnlineSource();
            String jdbcUrl = auditSource.getUrl();
            String username = auditSource.getUsername();
            String password = StringUtils.isBlank(auditSource.getToken()) ? "" : auditSource.getToken();

            boolean changed = !Objects.equals(currentJdbcUrl, jdbcUrl)
                    || !Objects.equals(currentUsername, username)
                    || !Objects.equals(currentPassword, password);
            if (changed) {
                currentJdbcUrl = jdbcUrl;
                currentUsername = username;
                currentPassword = password;

                Properties pros = new Properties();
                pros.put("url", jdbcUrl);
                if (StringUtils.isNotBlank(username)) {
                    pros.put("username", username);
                }
                if (StringUtils.isNotBlank(password)) {
                    pros.put("password", password);
                }

                source = DruidDataSourceFactory.createDataSource(pros);
                log.info("success to create connection to {}", jdbcUrl);
            }
        } catch (Exception e) {
            log.error("failed to read click house audit source: ", e);
        }
    }

    /**
     * Get ClickHouse connection from data source
     */
    public Connection getCkConnection() throws Exception {
        log.debug("start to get connection for CLICKHOUSE");
        int retry = 0;
        while (source == null && retry < 3) {
            updateRuntimeConfig();
            retry += 1;
        }

        if (source == null) {
            log.warn("jdbc source is null for CLICKHOUSE");
            return null;
        }

        Connection connection = source.getConnection();
        log.info("success to get connection for CLICKHOUSE");
        return connection;
    }
}
