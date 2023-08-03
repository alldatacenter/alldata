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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.datavines.common.CommonConstants;
import io.datavines.common.datasource.jdbc.BaseJdbcDataSourceInfo;
import io.datavines.common.param.ConnectorResponse;
import io.datavines.common.param.TestConnectionRequestParam;
import io.datavines.common.param.form.PluginParams;
import io.datavines.common.param.form.PropsType;
import io.datavines.common.param.form.Validate;
import io.datavines.common.param.form.props.InputParamsProps;
import io.datavines.common.param.form.type.InputParam;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.storage.api.StorageConnector;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.datavines.common.datasource.jdbc.JdbcDataSourceInfoManager.getDatasourceInfo;

@Slf4j
public class MysqlStorageConnector implements StorageConnector {

    @Override
    public String getConfigJson(boolean isEn) {
        InputParam host = getInputParam("host",
                isEn ? "host" : "地址",
                isEn ? "please enter host ip" : "请填入连接地址", 1,
                Validate.newBuilder()
                        .setRequired(true)
                        .setMessage(isEn ? "please enter host ip" : "请填入连接地址")
                        .build(),
                null);
        InputParam port = getInputParam("port",
                isEn ? "port" : "端口",
                isEn ? "please enter port" : "请填入端口号", 1,
                Validate.newBuilder()
                        .setRequired(true)
                        .setMessage(isEn ? "please enter port" : "请填入端口号")
                        .build(),
                null);
        InputParam database = getInputParam("database",
                isEn ? "database" : "数据库",
                isEn ? "please enter database" : "请填入数据库", 1,
                Validate.newBuilder()
                        .setRequired(true)
                        .setMessage(isEn ? "please enter database" : "请填入数据库")
                        .build(),
                null);
        InputParam user = getInputParam("user",
                isEn ? "user" : "用户名",
                isEn ? "please enter user" : "请填入用户名", 1,
                Validate.newBuilder()
                        .setRequired(true)
                        .setMessage(isEn ? "please enter user" : "请填入用户名")
                        .build(),
                null);
        InputParam password = getInputParam("password",
                isEn ? "password" : "密码",
                isEn ? "please enter password" : "请填入密码", 1,
                Validate.newBuilder()
                        .setRequired(true)
                        .setMessage(isEn ? "please enter password" : "请填入密码")
                        .build(),
                null);
        InputParam properties = getInputParam("properties",
                isEn ? "properties" : "参数",
                isEn ? "please enter properties,like key=value&key1=value1" : "请填入参数，格式为key=value&key1=value1", 2,
                null,
                "useUnicode=true&characterEncoding=UTF-8&useSSL=false");

        List<PluginParams> params = new ArrayList<>();
        params.add(host);
        params.add(port);
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
            log.error("json parse error : {}", e.getMessage(), e);
        }

        return result;
    }

    private InputParam getInputParam(String field, String title, String placeholder, int rows, Validate validate , Object defaultValue) {
        return InputParam
                .newBuilder(field, title)
                .addValidate(validate)
                .setProps(new InputParamsProps().setDisabled(false))
                .setSize(CommonConstants.SMALL)
                .setType(PropsType.TEXT)
                .setRows(rows)
                .setPlaceholder(placeholder)
                .setValue(defaultValue)
                .setEmit(null)
                .build();
    }

    @Override
    public Map<String, Object> getParamMap(Map<String, Object> parameter) {
        Map<String,Object> config = new HashMap<>();
        config.put("table",parameter.get("table"));
        config.put("user",parameter.get("user"));
        config.put("password", parameter.get("password"));
        config.put("url", parameter.get("url") == null ? getUrl(parameter) : parameter.get("url"));
        config.put("driver","com.mysql.cj.jdbc.Driver");
        return config;
    }

    private String getUrl(Map<String, Object> parameter) {
        String url = String.format("jdbc:mysql://%s:%s/%s",
                parameter.get("host"),
                parameter.get("port"),
                parameter.get("database"));
        String properties = (String)parameter.get("properties");
        if (StringUtils.isNotEmpty(properties)) {
            url += "?" + properties;
        }

        return url;
    }

    @Override
    public ConnectorResponse testConnect(TestConnectionRequestParam param) {
        BaseJdbcDataSourceInfo dataSourceInfo = getDatasourceInfo(param.getDataSourceParam());
        dataSourceInfo.loadClass();

        try (Connection con = DriverManager.getConnection(dataSourceInfo.getJdbcUrl(), dataSourceInfo.getUser(), dataSourceInfo.getPassword())) {
            boolean result = con != null;
            if (result) {
                con.close();
            }

            return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(result).build();
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }

        return ConnectorResponse.builder().status(ConnectorResponse.Status.SUCCESS).result(false).build();
    }
}
