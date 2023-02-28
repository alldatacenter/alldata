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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.datavines.common.datasource.jdbc.BaseJdbcDataSourceInfo;
import io.datavines.common.datasource.jdbc.JdbcConnectionInfo;
import io.datavines.common.datasource.jdbc.JdbcDataSourceInfoManager;
import io.datavines.common.param.ConnectorResponse;
import io.datavines.common.param.TestConnectionRequestParam;
import io.datavines.common.param.form.PluginParams;
import io.datavines.common.param.form.Validate;
import io.datavines.common.param.form.type.InputParam;
import io.datavines.common.utils.JSONUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrestoConnector extends JdbcConnector {

    @Override
    protected ResultSet getPrimaryKeys(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getMetadataColumns(DatabaseMetaData metaData, String catalog, String schema, String tableName, String columnName) throws SQLException {
        return metaData.getColumns(catalog, schema, tableName, "%");
    }

    @Override
    public ResultSet getMetadataTables(DatabaseMetaData metaData, String catalog, String schema) throws SQLException {
        return metaData.getTables(catalog, schema, null, TABLE_TYPES);
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

        try (Connection con = DriverManager.getConnection(dataSourceInfo.getJdbcUrl(), dataSourceInfo.getUser(), null)) {
            boolean result = con!=null;
            if (result) {
                con.close();
            }

            return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(result).build();
        } catch (SQLException e) {
            logger.error(e.toString(), e);
        }

        return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(false).build();
    }

    @Override
    public String getConfigJson(boolean isEn) {
        InputParam host = getInputParam("host",
                isEn ? "host":"地址",
                isEn ? "please enter host ip" : "请填入连接地址", 1, Validate.newBuilder()
                        .setRequired(true).setMessage(isEn ? "please enter host ip" : "请填入连接地址")
                        .build());
        InputParam port = getInputParam("port",
                isEn ? "port" : "端口",
                isEn ? "please enter port" : "请填入端口号", 1, Validate.newBuilder()
                        .setRequired(true).setMessage(isEn ? "please enter port" : "请填入端口号")
                        .build());
        InputParam catalog = getInputParam("catalog",
                isEn ? "catalog" : "目录类型",
                isEn ? "please enter catalog" : "请填入目录类型", 1, Validate.newBuilder()
                        .setRequired(true).setMessage(isEn ? "please enter catalog" : "请填入目录类型")
                        .build());

        InputParam database = getInputParam("database",
                isEn ? "database" : "数据库",
                isEn ? "please enter database" : "请填入数据库", 1, Validate.newBuilder()
                        .setRequired(false).setMessage(isEn ? "please enter database" : "请填入数据库")
                        .build());
        InputParam user = getInputParam("user",
                isEn ? "user" : "用户名",
                isEn ? "please enter user" : "请填入用户名", 1, Validate.newBuilder()
                        .setRequired(true).setMessage(isEn ? "please enter user" : "请填入用户名")
                        .build());
        InputParam password = getInputParam("password",
                isEn ? "password" : "密码",
                isEn ? "please enter password" : "请填入密码", 1, Validate.newBuilder()
                        .setRequired(false).setMessage(isEn ? "please enter password" : "请填入密码")
                        .build());
        InputParam properties = getInputParamNoValidate("properties",
                isEn ? "properties" : "参数",
                isEn ? "please enter properties,like key=value&key1=value1" : "请填入参数，格式为key=value&key1=value1", 2);

        List<PluginParams> params = new ArrayList<>();
        params.add(host);
        params.add(port);
        params.add(catalog);
        params.add(database);
        params.add(user);
        params.add(password);
        params.add(properties);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String result = null;

        try {
            result = mapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            logger.error("json parse error : {}", e.getMessage(), e);
        }

        return result;
    }
}
