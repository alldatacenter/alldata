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

import com.alibaba.druid.pool.DruidDataSource;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.Md5Utils;
import io.datavines.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class JdbcDataSourceManager {

    private final ConcurrentHashMap<String,DataSource> dataSourceMap = new ConcurrentHashMap<>();

    private static final class Singleton {
        private static final JdbcDataSourceManager INSTANCE = new JdbcDataSourceManager();
    }

    public static JdbcDataSourceManager getInstance() {
        return Singleton.INSTANCE;
    }

    public DataSource getDataSource(BaseJdbcDataSourceInfo baseJdbcDataSourceInfo) {
        if (baseJdbcDataSourceInfo == null) {
            return null;
        }

        DataSource dataSource = dataSourceMap.get(baseJdbcDataSourceInfo.getUniqueKey());

        if (dataSource == null) {
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(baseJdbcDataSourceInfo.getJdbcUrl());
            druidDataSource.setUsername(baseJdbcDataSourceInfo.getUser());
            druidDataSource.setPassword(StringUtils.isEmpty(baseJdbcDataSourceInfo.getPassword()) ? null : baseJdbcDataSourceInfo.getPassword());
            druidDataSource.setDriverClassName(baseJdbcDataSourceInfo.getDriverClass());
            druidDataSource.setBreakAfterAcquireFailure(true);
            druidDataSource.setValidationQuery(baseJdbcDataSourceInfo.getValidationQuery());
            dataSourceMap.put(baseJdbcDataSourceInfo.getUniqueKey(), druidDataSource);
            return druidDataSource;
        }

        return dataSource;
    }

    public DataSource getDataSource(Map<String,Object> configMap) {
        String uniqueKey = getUniqueKey(configMap);
        DataSource dataSource = dataSourceMap.get(getUniqueKey(configMap));

        if (dataSource == null) {
            String driver = String.valueOf(configMap.get("driver"));
            String url = String.valueOf(configMap.get("url"));
            String username = String.valueOf(configMap.get("user"));
            String password = String.valueOf(configMap.get("password"));
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(url);
            druidDataSource.setUsername(username);
            druidDataSource.setPassword(StringUtils.isEmpty(password) ? null : password);
            druidDataSource.setDriverClassName(driver);
            druidDataSource.setBreakAfterAcquireFailure(true);
            dataSourceMap.put(uniqueKey, druidDataSource);
            return druidDataSource;
        }

        return dataSource;
    }

    private String getUniqueKey(Map<String,Object> configMap) {
        return Md5Utils.getMd5(JSONUtils.toJsonString(configMap),false);
    }
}
