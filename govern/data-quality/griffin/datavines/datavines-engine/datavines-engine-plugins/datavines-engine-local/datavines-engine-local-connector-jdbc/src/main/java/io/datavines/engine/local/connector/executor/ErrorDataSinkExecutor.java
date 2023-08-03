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
import io.datavines.common.enums.DataType;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.StringUtils;
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.connector.api.Dialect;
import io.datavines.connector.api.TypeConverter;
import io.datavines.connector.plugin.entity.JdbcOptions;
import io.datavines.connector.plugin.entity.StructField;
import io.datavines.connector.plugin.utils.JdbcUtils;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.entity.ConnectionItem;
import io.datavines.engine.local.api.entity.ResultList;
import io.datavines.engine.local.api.utils.SqlUtils;
import io.datavines.engine.local.api.utils.LoggerFactory;
import io.datavines.spi.PluginLoader;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static io.datavines.common.ConfigConstants.*;
import static io.datavines.common.ConfigConstants.ERROR_DATA_FILE_NAME;

public class ErrorDataSinkExecutor extends BaseDataSinkExecutor {

    protected Logger log = LoggerFactory.getLogger(ErrorDataSinkExecutor.class);

    private ConnectionItem connectionItem;

    public ErrorDataSinkExecutor(Config config, LocalRuntimeEnvironment env) {
        super(config, env);
    }

    private ConnectionItem getConnectionItem() {
        if (connectionItem == null) {
            connectionItem = new ConnectionItem(config);
        }

        return connectionItem;
    }

    @Override
    public void execute(Map<String, String> inputParameter) throws DataVinesException {
        try {
            sinkErrorData();
        } catch (Exception e) {
            log.error("sink error data error : ", e);
            throw new DataVinesException(e);
        } finally {
            after(env, config);
        }
    }

    private void sinkErrorData() {

        if (TRUE.equals(config.getString(INVALIDATE_ITEM_CAN_OUTPUT))) {

            String sourceTable = config.getString(INVALIDATE_ITEMS_TABLE);
            Statement sourceConnectionStatement = null;
            ResultSet countResultSet = null;
            ResultSet errorDataResultSet = null;
            Connection errorDataStorageConnection = null;
            PreparedStatement errorDataPreparedStatement = null;
            try {
                sourceConnectionStatement = env.getSourceConnection().getConnection().createStatement();
                int count = 0;
                //执行统计行数语句
                countResultSet = sourceConnectionStatement.executeQuery("SELECT COUNT(1) FROM " + sourceTable);
                if (countResultSet.next()) {
                    count = countResultSet.getInt(1);
                }

                if (count > 0) {
                    String srcConnectorType = config.getString(SRC_CONNECTOR_TYPE);
                    ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(srcConnectorType);
                    TypeConverter typeConverter = connectorFactory.getTypeConverter();
                    Dialect dialect = connectorFactory.getDialect();
                    String targetTableName = config.getString(ERROR_DATA_FILE_NAME);
                    List<StructField> columns = getTableSchema(sourceConnectionStatement, config, typeConverter);
                    if (!checkTableExist(getConnectionItem().getConnection(), targetTableName, dialect)) {
                        createTable(typeConverter, dialect, targetTableName, columns);
                    }
                    //根据行数进行分页查询。分批写到文件里面
                    int pageSize = 1000;
                    int totalPage = count/pageSize + count%pageSize>0 ? 1:0;

                    errorDataResultSet = sourceConnectionStatement.executeQuery("SELECT * FROM " + sourceTable);
                    errorDataStorageConnection = getConnectionItem().getConnection();
                    errorDataPreparedStatement = errorDataStorageConnection.prepareStatement(JdbcUtils.getInsertStatement(targetTableName, columns, dialect));
                    for (int i=0; i<totalPage; i++) {
                        int start = i * pageSize;
                        int end = (i+1) * pageSize;

                        ResultList resultList = SqlUtils.getPageFromResultSet(errorDataResultSet, SqlUtils.getQueryFromsAndJoins("select * from " + sourceTable), start, end);
                        for (Map<String, Object> row: resultList.getResultList()) {
                            for (int j=0 ;j<columns.size();j++) {
                                StructField field = columns.get(j);
                                String value = String.valueOf(row.get(field.getName()));
                                String rowContent = "null".equalsIgnoreCase(value) ? null : value;
                                if (rowContent != null) {
                                    rowContent = rowContent.replaceAll("\"","");
                                }
                                DataType dataType = field.getDataType();
                                try {
                                    switch (dataType) {
                                        case NULL_TYPE:
                                            errorDataPreparedStatement.setNull(j+1, 0);
                                            break;
                                        case BOOLEAN_TYPE:
                                            errorDataPreparedStatement.setBoolean(j+1, Boolean.parseBoolean(rowContent));
                                            break;
                                        case BYTE_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setByte(j+1, Byte.parseByte(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setByte(j+1,Byte.parseByte(""));
                                            }
                                            break;
                                        case SHORT_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setShort(j+1, Short.parseShort(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setShort(j+1, Short.parseShort("0"));
                                            }
                                            break;
                                        case INT_TYPE :
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setInt(j+1, Integer.parseInt(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setInt(j+1, 0);
                                            }
                                            break;
                                        case LONG_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setLong(j+1, Long.parseLong(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setLong(j+1, 0);
                                            }
                                            break;
                                        case FLOAT_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setFloat(j+1, Float.parseFloat(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setFloat(j+1, 0);
                                            }
                                            break;
                                        case DOUBLE_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setDouble(j+1, Double.parseDouble(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setDouble(j+1, 0);
                                            }
                                            break;
                                        case TIME_TYPE:
                                        case DATE_TYPE:
                                        case TIMESTAMP_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setString(j+1,rowContent);
                                            } else {
                                                errorDataPreparedStatement.setString(j+1,null);
                                            }
                                            break;
                                        case STRING_TYPE :
                                            errorDataPreparedStatement.setString(j+1, rowContent);
                                            break;
                                        case BYTES_TYPE:
                                            errorDataPreparedStatement.setBytes(j+1, String.valueOf(rowContent).getBytes());
                                            break;
                                        case BIG_DECIMAL_TYPE:
                                            if (StringUtils.isNotEmpty(rowContent)) {
                                                errorDataPreparedStatement.setBigDecimal(j+1, new BigDecimal(rowContent));
                                            } else {
                                                errorDataPreparedStatement.setBigDecimal(j+1, null);
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
                            }
                            try {
                                errorDataPreparedStatement.addBatch();
                            } catch (SQLException e) {
                                log.error("insert data error", e);
                            }
                        }
                        errorDataPreparedStatement.executeBatch();
                    }
                    log.info("sink error data finished");
                }
            } catch (Exception e) {
                log.error("sink error data error : ", e);
                throw new DataVinesException("sink error data error", e);
            } finally {
                SqlUtils.closeResultSet(countResultSet);
                SqlUtils.closeStatement(sourceConnectionStatement);
                SqlUtils.closeResultSet(errorDataResultSet);
                SqlUtils.closeStatement(errorDataPreparedStatement);
                SqlUtils.closeConnection(errorDataStorageConnection);
            }
        }
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

    private void createTable(TypeConverter typeConverter, Dialect dialect,
                             String targetTableName, List<StructField> columns) throws SQLException {
        String createTableSql =
                JdbcUtils.getCreateTableStatement(targetTableName, columns, dialect, typeConverter);
        if (StringUtils.isEmpty(createTableSql)) {
            log.error("generate create table sql error");
            return;
        }
        Statement statement = getConnectionItem().getConnection().createStatement();
        statement.execute(createTableSql);
        statement.close();

        log.info("create error data table : " + createTableSql);
    }

    private List<StructField> getTableSchema(Statement statement, Config config, TypeConverter typeConverter) {
        if (statement != null) {
            ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class)
                    .getOrCreatePlugin(config.getString(SRC_CONNECTOR_TYPE));

            String tableName = config.getString(INVALIDATE_ITEMS_TABLE);
            JdbcOptions jdbcOptions = new JdbcOptions();
            jdbcOptions.setTableName(tableName);
            jdbcOptions.setQueryTimeout(10000);
            try {
                Dialect dialect = connectorFactory.getDialect();
                String getSchemaQuery = dialect.getSchemaQuery(tableName);
                return JdbcUtils.getSchema(statement.executeQuery(getSchemaQuery), dialect, typeConverter);
            } catch (Exception e) {
                log.error("check table {} exists error ：", config.getString(INVALIDATE_ITEMS_TABLE), e);
                return null;
            }
        } else {
            return null;
        }
    }
}
