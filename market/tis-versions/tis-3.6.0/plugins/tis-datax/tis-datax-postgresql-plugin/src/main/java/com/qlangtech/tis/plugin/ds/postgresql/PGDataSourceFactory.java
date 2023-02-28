/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.ds.postgresql;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.DataXPostgresqlReader;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.PGProperty;
import org.postgresql.jdbc.PgConnection;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * https://jdbc.postgresql.org/download.html <br/>
 * sehcme 设置办法：<br/>
 * https://blog.csdn.net/sanyuedexuanlv/article/details/84615388 <br/>
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-23 19:03
 **/
@Public
public class PGDataSourceFactory extends BasicDataSourceFactory implements BasicDataSourceFactory.ISchemaSupported {
    // public static final String DS_TYPE_PG = "PG";

    private static final String FIELD_TAB_SCHEMA = "tabSchema";

    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String tabSchema;

    @Override
    public String getEscapeChar() {
        return "\"";
    }

    @Override
    public String getDBSchema() {
        return this.tabSchema;
    }

    @Override
    public String buidJdbcUrl(DBConfig db, String ip, String dbName) {
        //https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters
        String jdbcUrl = "jdbc:postgresql://" + ip + ":" + this.port + "/" + dbName + "?ssl=false&stringtype=unspecified";
        // boolean hasParam = false;
        if (StringUtils.isNotEmpty(this.encode)) {
            // hasParam = true;
            jdbcUrl = jdbcUrl + "&charSet=" + this.encode;
        }
        if (StringUtils.isNotEmpty(this.extraParams)) {
            jdbcUrl = jdbcUrl + "&" + this.extraParams;
        }
        return jdbcUrl;
    }

    @Override
    public void refectTableInDB(List<String> tabs, Connection conn) throws SQLException {
        Statement statement = null;
        ResultSet result = null;
        try {
            statement = conn.createStatement();
            if (StringUtils.isEmpty(this.tabSchema)) {
                throw new IllegalStateException("prop tabSchema can not be null");
            }
            if (StringUtils.isEmpty(this.dbName)) {
                throw new IllegalStateException("prop dbName can not be null");
            }
            result = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables  WHERE table_schema = '" + this.tabSchema + "' and table_catalog='" + this.dbName + "'");

//        DatabaseMetaData metaData = conn.getMetaData();
//        String[] types = {"TABLE"};
//        ResultSet tablesResult = metaData.getTables(conn.getCatalog(), "public", "%", types);
            while (result.next()) {
                tabs.add(result.getString(1));
            }
        } finally {
            this.closeResultSet(result);
            try {
                statement.close();
            } catch (Throwable e) { }
        }
    }

    @Override
    public Connection getConnection(String jdbcUrl) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // StringUtils.trimToNull(this.userName), StringUtils.trimToNull(password)
        java.util.Properties props = new Properties();
        props.setProperty(PGProperty.CURRENT_SCHEMA.getName(), this.tabSchema);
        props.setProperty(PGProperty.USER.getName(), this.getUserName());
        props.setProperty(PGProperty.PASSWORD.getName(), this.password);

        return DriverManager.getConnection(jdbcUrl, props);
//        if (!StringUtils.equals(this.tabSchema, conn.getSchema())) {
//            throw new TisException("invalid tabSchema:" + this.tabSchema);
//        }
        //  return conn;
    }


    @Override
    protected DataType createColDataType(String colName
            , String typeName, int dbColType, int colSize) throws SQLException {
        DataType type = super.createColDataType(colName, typeName, dbColType, colSize);
        DataType fix = type.accept(new DataType.DefaultTypeVisitor<DataType>() {
            @Override
            public DataType bitType(DataType type) {
                if (StringUtils.lastIndexOfIgnoreCase(type.typeName, "bool") > -1) {
                    return new DataType(Types.BOOLEAN, type.typeName, type.columnSize);
                }
                return null;
            }
        });
        return fix != null ? fix : type;
    }

//    @Override
//    public DataDumpers getDataDumpers(TISTable table) {
//        Iterator<IDataSourceDumper> dumpers = null;
//        return new DataDumpers(1, dumpers);
//    }

    @TISExtension
    public static class DefaultDescriptor extends BasicRdbmsDataSourceFactoryDescriptor {
        @Override
        protected String getDataSourceName() {
            return DataXPostgresqlReader.PG_NAME;
        }

        @Override
        public boolean supportFacade() {
            return false;
        }

        @Override
        public List<String> facadeSourceTypes() {
            return Collections.emptyList();
        }

        @Override
        protected boolean validateDSFactory(IControlMsgHandler msgHandler, Context context, BasicDataSourceFactory dsFactory) {
            try {
                AtomicBoolean valid = new AtomicBoolean(true);
                PGDataSourceFactory ds = (PGDataSourceFactory) dsFactory;
                dsFactory.visitFirstConnection((c) -> {
                    PgConnection conn = (PgConnection) c;
                    if (!StringUtils.equals(ds.tabSchema, conn.getSchema())) {
                        msgHandler.addFieldError(context, FIELD_TAB_SCHEMA, "Invalid table Schema valid");
                        valid.set(false);
                    }
                });

                if (!valid.get()) {
                    return false;
                }
                //  List<String> tables = dsFactory.getTablesInDB();
                // msgHandler.addActionMessage(context, "find " + tables.size() + " table in db");
            } catch (Exception e) {
                //logger.warn(e.getMessage(), e);
                msgHandler.addErrorMessage(context, TisException.getErrMsg(e).getMessage());
                return false;
            }
            return true;
        }
    }

}
