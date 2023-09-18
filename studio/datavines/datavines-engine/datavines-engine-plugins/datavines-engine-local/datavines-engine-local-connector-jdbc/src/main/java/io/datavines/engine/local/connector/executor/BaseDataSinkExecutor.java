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
package io.datavines.engine.local.connector.executor;

import io.datavines.common.config.Config;
import io.datavines.common.utils.ParameterUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.utils.LoggerFactory;
import io.datavines.engine.local.api.utils.SqlUtils;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static io.datavines.common.ConfigConstants.INVALIDATE_ITEMS_TABLE;
import static io.datavines.common.ConfigConstants.SQL;
import static io.datavines.engine.api.EngineConstants.PLUGIN_TYPE;
import static io.datavines.engine.local.connector.BaseJdbcSink.CREATE_TABLE_SQL;
import static io.datavines.engine.local.connector.BaseJdbcSink.SINK_TABLE_NAME;

public abstract class BaseDataSinkExecutor implements ISinkExecutor {

    protected Logger log = LoggerFactory.getLogger(BaseDataSinkExecutor.class);

    protected final Config config;

    protected final LocalRuntimeEnvironment env;

    public BaseDataSinkExecutor(Config config, LocalRuntimeEnvironment env) {
        this.config = config;
        this.env = env;
    }

    protected void innerExecute(Map<String, String> inputParameter) throws SQLException{
        executeDataSink(env, inputParameter);
    }

    private void executeDataSink(LocalRuntimeEnvironment env, Map<String,String> inputParameter) throws SQLException {

        String tableName = inputParameter.get(SINK_TABLE_NAME);
        String createTableSql = inputParameter.get(CREATE_TABLE_SQL);

        if(!checkTableExist(env, tableName)) {
            createTable(env, createTableSql);
        }

        String sql = config.getString(SQL);
        sql = ParameterUtils.convertParameterPlaceholders(sql, inputParameter);
        log.info("execute " + config.getString(PLUGIN_TYPE) + " output sql : {}", sql);
        if (StringUtils.isNotEmpty(sql) && !sql.contains("${")) {
            executeInsert(sql, env);
        } else {
            log.error("output sql {} contains placeholder", sql);
        }
    }

    private void executeInsert(String sql, LocalRuntimeEnvironment env) throws SQLException {
        Statement statement =  env.getMetadataConnection().getConnection().createStatement();
        statement.execute(sql);
        statement.close();
    }

    private boolean checkTableExist(LocalRuntimeEnvironment env, String tableName) throws SQLException {
        //定义一个变量标示
        boolean flag = false ;
        //一个查询该表所有的语句。
        String sql = "SELECT COUNT(*) FROM "+ tableName ;
        Statement statement = null;
        try {
            statement =  env.getMetadataConnection().getConnection().createStatement();
            statement.executeQuery(sql);
            flag =  true;
        } catch(Exception e) {
            log.warn("table {} is not exist", tableName);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return flag;
    }

    private void createTable(LocalRuntimeEnvironment env, String createTableSql) throws SQLException{
        Statement statement =  env.getMetadataConnection().getConnection().createStatement();
        statement.execute(createTableSql);
        statement.close();
    }

    protected void after(LocalRuntimeEnvironment env, Config config) {
        try {
            SqlUtils.dropView(config.getString(INVALIDATE_ITEMS_TABLE), env.getSourceConnection().getConnection());
        } catch (SQLException e) {
            log.error("create connection error: ", e);
        }
    }
}
