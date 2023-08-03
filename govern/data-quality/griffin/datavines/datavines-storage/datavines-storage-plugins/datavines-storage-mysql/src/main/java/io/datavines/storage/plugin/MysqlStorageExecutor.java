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
package io.datavines.storage.plugin;

import io.datavines.common.datasource.jdbc.*;
import io.datavines.common.datasource.jdbc.utils.SqlUtils;
import io.datavines.common.entity.ConnectionInfo;
import io.datavines.common.param.ConnectorResponse;
import io.datavines.common.param.ExecuteRequestParam;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.JdbcUrlParser;
import io.datavines.storage.api.StorageExecutor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

public class MysqlStorageExecutor implements StorageExecutor, IJdbcDataSourceInfo {

    private final JdbcExecutorClientManager jdbcExecutorClientManager = JdbcExecutorClientManager.getInstance();

    @Override
    public ConnectorResponse queryForPage(ExecuteRequestParam param) {
        ConnectorResponse.ConnectorResponseBuilder builder = ConnectorResponse.builder();
        String dataSourceParam = param.getDataSourceParam();

        JdbcConnectionInfo jdbcConnectionInfo = JSONUtils.parseObject(dataSourceParam, JdbcConnectionInfo.class);

        JdbcExecutorClient executorClient = jdbcExecutorClientManager
                .getExecutorClient(
                        JdbcDataSourceInfoManager.getDatasourceInfo(dataSourceParam,
                                getDatasourceInfo(jdbcConnectionInfo)));

        JdbcTemplate jdbcTemplate = executorClient.getJdbcTemplate();

        String sql = param.getScript();
        if (StringUtils.isEmpty(sql)) {
            builder.status(ConnectorResponse.Status.ERROR);
            builder.errorMsg("execute script must not null");
        }

        builder.result(SqlUtils.queryForPage(jdbcTemplate, sql, param.getLimit(),
                param.getPageNumber(), param.getPageSize()));

        return builder.build();
    }

    @Override
    public ConnectorResponse queryForOne(ExecuteRequestParam param) {
        ConnectorResponse.ConnectorResponseBuilder builder = ConnectorResponse.builder();
        String dataSourceParam = param.getDataSourceParam();

        Map<String,String> paramMap = JSONUtils.toMap(dataSourceParam, String.class, String.class);
        ConnectionInfo connectionInfo = JdbcUrlParser.getConnectionInfo(paramMap.get("url"), paramMap.get("user"), paramMap.get("password"));
        JdbcConnectionInfo jdbcConnectionInfo = new JdbcConnectionInfo();
        assert connectionInfo != null;
        BeanUtils.copyProperties(connectionInfo, jdbcConnectionInfo);

        JdbcExecutorClient executorClient = jdbcExecutorClientManager
                .getExecutorClient(
                        JdbcDataSourceInfoManager.getDatasourceInfo(dataSourceParam,
                                getDatasourceInfo(jdbcConnectionInfo)));

        JdbcTemplate jdbcTemplate = executorClient.getJdbcTemplate();

        String sql = param.getScript() + " limit 1";
        if (StringUtils.isEmpty(sql)) {
            builder.status(ConnectorResponse.Status.ERROR);
            builder.errorMsg("execute script must not null");
        }

        builder.result(SqlUtils.queryForPage(jdbcTemplate, sql, param.getLimit(),
                param.getPageNumber(), param.getPageSize()));

        return builder.build();
    }

    @Override
    public BaseJdbcDataSourceInfo getDatasourceInfo(JdbcConnectionInfo jdbcConnectionInfo) {
        return new MysqlDataSourceInfo(jdbcConnectionInfo);
    }
}
