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
package io.datavines.connector.plugin;

import io.datavines.common.datasource.jdbc.BaseJdbcDataSourceInfo;
import io.datavines.common.datasource.jdbc.JdbcConnectionInfo;
import io.datavines.common.param.ConnectorResponse;
import io.datavines.common.param.TestConnectionRequestParam;
import io.datavines.common.param.form.Validate;
import io.datavines.common.param.form.type.InputParam;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class PrestoConnector extends JdbcConnector {

    @Override
    protected ResultSet getPrimaryKeys(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getMetadataDatabases(Connection connection) throws SQLException {
        java.sql.Statement stmt = connection.createStatement();
        return stmt.executeQuery("SHOW SCHEMAS");
    }

    @Override
    public BaseJdbcDataSourceInfo getDatasourceInfo(JdbcConnectionInfo jdbcConnectionInfo) {
        return new PrestoDataSourceInfo(jdbcConnectionInfo);
    }

    @Override
    public ConnectorResponse testConnect(TestConnectionRequestParam param) {
        JdbcConnectionInfo jdbcConnectionInfo = JSONUtils.parseObject(param.getDataSourceParam(), JdbcConnectionInfo.class);
        BaseJdbcDataSourceInfo dataSourceInfo = getDatasourceInfo(jdbcConnectionInfo);
        dataSourceInfo.loadClass();

        try (Connection con = DriverManager.getConnection(dataSourceInfo.getJdbcUrl(),
                dataSourceInfo.getUser(), StringUtils.isEmpty(dataSourceInfo.getPassword()) ? null : dataSourceInfo.getPassword())) {
            boolean result = (con!=null);
            if (result) {
                try {
                    getMetadataDatabases(con);
                } catch (Exception e) {
                    logger.error("create connection error", e);
                    return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(false).build();
                }

                con.close();
            }

            return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(true).build();
        } catch (SQLException e) {
            logger.error("create connection error", e);
            return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(false).build();
        }
    }

    @Override
    protected InputParam getCatalogInput(boolean isEn) {
        return getInputParam("catalog",
                isEn ? "catalog" : "目录类型",
                isEn ? "please enter catalog" : "请填入目录类型", 1,
                Validate.newBuilder().setRequired(true).setMessage(isEn ? "please enter catalog" : "请填入目录类型").build(),
                null);
    }

    @Override
    protected InputParam getDatabaseInput(boolean isEn) {
        return getInputParam("database",
                isEn ? "database" : "数据库",
                isEn ? "please enter database" : "请填入数据库", 1,
                null, null);
    }

    @Override
    protected InputParam getPasswordInput(boolean isEn) {
        return getInputParam("password",
                isEn ? "password" : "密码",
                isEn ? "please enter password" : "请填入密码", 1,
                null,
                null);
    }

    @Override
    public List<String> keyProperties() {
        return Arrays.asList("host","port","catalog","database");
    }
}
