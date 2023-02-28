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
import io.datavines.common.config.enums.SinkType;
import io.datavines.common.enums.DataType;
import io.datavines.common.utils.StringUtils;
import io.datavines.common.utils.placeholder.PlaceholderUtils;
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.connector.api.Dialect;
import io.datavines.connector.api.TypeConverter;
import io.datavines.connector.plugin.entity.StructField;
import io.datavines.connector.plugin.utils.JdbcUtils;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.LocalSink;
import io.datavines.engine.local.api.entity.ConnectionItem;
import io.datavines.engine.local.api.entity.ResultList;
import io.datavines.engine.local.api.utils.SqlUtils;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static io.datavines.engine.api.ConfigConstants.*;
import static io.datavines.engine.api.EngineConstants.PLUGIN_TYPE;

@Slf4j
public abstract class BaseJdbcSink implements LocalSink {

    protected Config config = new Config();

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
        List<String> requiredOptions = Arrays.asList("url", "user", "password");

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
    public void prepare(RuntimeEnvironment env) {

    }

    @Override
    public void output(List<ResultList> resultList, LocalRuntimeEnvironment env) {

        if (env.getMetadataConnection() == null) {
            env.setMetadataConnection(getConnectionItem());
        }

        Map<String,String> inputParameter = new HashMap<>();
        setExceptedValue(config, resultList, inputParameter);
        try {
            switch (SinkType.of(config.getString(PLUGIN_TYPE))){
                case ERROR_DATA:
                    sinkErrorData(env);
                    after(env, config);
                    break;
                case VALIDATE_RESULT:
                    executeDataSink(env, "dv_job_execution_result", getExecutionResultTableSql(), inputParameter);
                    break;
                case ACTUAL_VALUE:
                    executeDataSink(env, "dv_actual_values", getActualValueTableSql(), inputParameter);
                    break;
                case PROFILE_VALUE:
                    executeDataSink(env, "dv_catalog_entity_profile", getProfileValueTableSql(), inputParameter);
                    after(env, config);
                    break;
                default:
                    break;
            }
        } catch (SQLException e){
            log.error("sink error : {}", e.getMessage());
        }

        after(env, config);
    }

    private void executeDataSink(LocalRuntimeEnvironment env, String tableName, String createTableSql, Map<String,String> inputParameter) throws SQLException {
        if(!checkTableExist(env, tableName)) {
            createTable(env, createTableSql);
        }

        String sql = config.getString(SQL);
        sql = PlaceholderUtils.replacePlaceholders(sql, inputParameter,true);
        log.info("execute " + config.getString(PLUGIN_TYPE) + " output sql : {}", sql);
        if (StringUtils.isNotEmpty(sql) && !sql.contains("${")) {
            executeInsert(sql, env);
        } else {
            log.error("output sql {} contains placeholder ${}", sql);
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

    private boolean checkTableExist(Connection connection, String tableName, Dialect dialect) throws SQLException{
        //定义一个变量标示
        boolean flag = false ;
        //一个查询该表所有的语句。
        String sql = dialect.getTableExistsQuery(tableName);
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery(sql);
            flag = true;
        } catch (Exception e) {
            log.warn("table {} is not exist", tableName);
        }
        return flag;
    }

    private void createTable(LocalRuntimeEnvironment env, String createTableSql) throws SQLException{
        Statement statement =  env.getMetadataConnection().getConnection().createStatement();
        statement.execute(createTableSql);
        statement.close();
    }

    protected abstract String getExecutionResultTableSql();

    protected abstract String getActualValueTableSql();

    protected abstract String getProfileValueTableSql();

    private ConnectionItem getConnectionItem() {
        return new ConnectionItem(config);
    }

    private void sinkErrorData(LocalRuntimeEnvironment env) throws SQLException {
        String sourceTable = config.getString(INVALIDATE_ITEMS_TABLE);
        Statement statement = env.getSourceConnection().getConnection().createStatement();
        if (TRUE.equals(config.getString(INVALIDATE_ITEM_CAN_OUTPUT))) {
            int count = 0;
            //执行统计行数语句
            ResultSet countResultSet = statement.executeQuery("SELECT COUNT(1) FROM " + sourceTable);
            if (countResultSet.next()) {
                count = countResultSet.getInt(1);
            }

            if (count > 0) {
                String srcConnectorType = config.getString(SRC_CONNECTOR_TYPE);
                ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(srcConnectorType);
                TypeConverter typeConverter = connectorFactory.getTypeConverter();
                Dialect dialect = connectorFactory.getDialect();
                String targetTableName = config.getString(ERROR_DATA_FILE_NAME);
                List<StructField> columns = getTableSchema(statement, config, typeConverter);
                if (!checkTableExist(getConnectionItem().getConnection(), targetTableName, dialect)) {
                    createTable(typeConverter, dialect, targetTableName, columns);
                }
                //根据行数进行分页查询。分批写到文件里面
                int pageSize = 1000;
                int totalPage = count/pageSize + count%pageSize>0 ? 1:0;

                ResultSet resultSet = statement.executeQuery("SELECT * FROM " + sourceTable);
                Connection connection = getConnectionItem().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(JdbcUtils.getInsertStatement(targetTableName, columns, dialect));
                for (int i=0; i<totalPage; i++) {
                    int start = i * pageSize;
                    int end = (i+1) * pageSize;

                    ResultList resultList = SqlUtils.getPageFromResultSet(resultSet, SqlUtils.getQueryFromsAndJoins("select * from " + sourceTable), start, end);
                    for (Map<String, Object> row: resultList.getResultList()) {
                        for (int j=0 ;j<columns.size();j++) {
                            StructField field = columns.get(j);
                            String value = String.valueOf(row.get(field.getName()));
                            String rowContent = "null".equalsIgnoreCase(value) ? null : value;
                            DataType dataType = field.getDataType();
                            try {
                                switch (dataType) {
                                    case NULL_TYPE:
                                        preparedStatement.setNull(i+1, 0);
                                        break;
                                    case BOOLEAN_TYPE:
                                        preparedStatement.setBoolean(i+1, Boolean.parseBoolean(rowContent));
                                        break;
                                    case BYTE_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setByte(i+1, Byte.parseByte(rowContent));
                                        } else {
                                            preparedStatement.setByte(i+1,Byte.parseByte(""));
                                        }
                                        break;
                                    case SHORT_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setShort(i+1, Short.parseShort(rowContent));
                                        } else {
                                            preparedStatement.setShort(i+1, Short.parseShort("0"));
                                        }
                                        break;
                                    case INT_TYPE :
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setInt(i+1, Integer.parseInt(rowContent));
                                        } else {
                                            preparedStatement.setInt(i+1, 0);
                                        }
                                        break;
                                    case LONG_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setLong(i+1, Long.parseLong(rowContent));
                                        } else {
                                            preparedStatement.setLong(i+1, 0);
                                        }
                                        break;
                                    case FLOAT_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setLong(i+1, Long.parseLong(rowContent));
                                        } else {
                                            preparedStatement.setLong(i+1, 0);
                                        }
                                        break;
                                    case DOUBLE_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setDouble(i+1, Double.parseDouble(rowContent));
                                        } else {
                                            preparedStatement.setDouble(i+1, 0);
                                        }
                                        break;
                                    case TIME_TYPE:
                                    case DATE_TYPE:
                                    case TIMESTAMP_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setString(i+1,rowContent);
                                        } else {
                                            preparedStatement.setString(i+1,null);
                                        }
                                        break;
                                    case STRING_TYPE :
                                        preparedStatement.setString(i+1, rowContent);
                                        break;
                                    case BYTES_TYPE:
                                        preparedStatement.setBytes(i+1, String.valueOf(rowContent).getBytes());
                                        break;
                                    case BIG_DECIMAL_TYPE:
                                        if (StringUtils.isNotEmpty(rowContent)) {
                                            preparedStatement.setBigDecimal(i+1, new BigDecimal(rowContent));
                                        } else {
                                            preparedStatement.setBigDecimal(i+1, null);
                                        }
                                        break;
                                    case OBJECT:
                                        break;
                                    default:
                                        break;
                                }
                            } catch (SQLException exception) {
                                log.error("transform data type error", exception);
                            }

                            try {
                                preparedStatement.addBatch();
                            } catch (SQLException e) {
                                log.error("insert data error", e);
                            }
                        }
                    }
                    preparedStatement.executeBatch();
                    preparedStatement.close();
                }

                connection.close();
                resultSet.close();
            }
        }
    }

    private void createTable(TypeConverter typeConverter, Dialect dialect, String targetTableName, List<StructField> columns) throws SQLException {
        String createTableSql =
                JdbcUtils.getCreateTableStatement(targetTableName, columns, dialect, typeConverter);
        Statement statement = getConnectionItem().getConnection().createStatement();
        statement.execute(createTableSql);
        statement.close();
    }
}
