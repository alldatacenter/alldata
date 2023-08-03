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
package io.datavines.engine.local.connector;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.Config;
import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.LocalSource;
import io.datavines.engine.local.api.entity.ConnectionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class LocalFileSource implements LocalSource {

    private Logger log = LoggerFactory.getLogger(LocalFileSource.class);

    private Config config = new Config();

    @Override
    public void setConfig(Config config) {
        if(config != null) {
            this.config = config;
        }
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public CheckResult checkConfig() {
        List<String> requiredOptions = Arrays.asList("path", "table_name", "schema");

        List<String> nonExistsOptions = new ArrayList<>();
        requiredOptions.forEach(x->{
            if(!config.has(x)){
                nonExistsOptions.add(x);
            }
        });

        if (!nonExistsOptions.isEmpty()) {
            return new CheckResult(
                    false,
                    "please specify " + nonExistsOptions.stream().map(option ->
                            "[" + option + "]").collect(Collectors.joining(",")) + " as non-empty string");
        } else {
            return new CheckResult(true, "");
        }
    }

    @Override
    public ConnectionItem getConnectionItem(LocalRuntimeEnvironment env) {
        Connection conn;
        try {
            conn = getConnection();
            Statement statement = conn.createStatement();
            statement.execute(buildCreateTableSql(config.getString("table_name"), config.getString("schema"), config.getString("path")));
            statement.close();
            return new ConnectionItem(conn, config);
        } catch (Exception e) {
            log.error("can not create connection");
        }

        return null;
    }

    @Override
    public void prepare(RuntimeEnvironment env) throws Exception{
        //An exception is reported if the file length is greater than 10M
        File file = new File(config.getString("path"));
        if (file.exists()) {
            long fileSize = file.length();
            long maxFileLength = CommonPropertyUtils.getLong(CommonPropertyUtils.FILE_MAX_LENGTH, CommonPropertyUtils.FILE_MAX_LENGTH_DEFAULT);
            if (fileSize > maxFileLength) {
                throw new Exception("Error: file length is greater than " + maxFileLength);
            }
        }
    }

    private Connection getConnection() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", "test");
        properties.setProperty("password", "123456");
        properties.setProperty("rowId", "false");
        DriverManager.registerDriver(new org.h2.Driver());
        Class.forName("org.h2.Driver", false, this.getClass().getClassLoader());
        return DriverManager.getConnection("jdbc:h2:mem:data;DB_CLOSE_DELAY=-1", properties);
    }

    private String buildCreateTableSql(String tableName, String schema, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName);
        if (StringUtils.isNotEmpty(schema)) {
            sb.append("(");
            String[] columns = schema.split(",");
            String[] columnWithTypeList = new String[columns.length];
            for (int i=0; i< columns.length; i++) {
                columnWithTypeList[i] = columns[i] + " VARCHAR(65535)";
            }
            sb.append(String.join(",", columnWithTypeList));
            sb.append(")");
        }
        sb.append(" AS SELECT * FROM CSVREAD('").append(fileName).append("')");
        log.info("create table sql : " + sb.toString());
        return sb.toString();
    }

    @Override
    public boolean checkTableExist() {
        return true;
    }
}
