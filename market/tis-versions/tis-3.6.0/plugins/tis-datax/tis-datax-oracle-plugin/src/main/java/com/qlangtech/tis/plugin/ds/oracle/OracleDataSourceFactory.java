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

package com.qlangtech.tis.plugin.ds.oracle;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

import java.sql.*;
import java.util.function.Function;

import static oracle.jdbc.OracleTypes.*;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-24 13:42
 **/
@Public
public class OracleDataSourceFactory extends BasicDataSourceFactory {

    public static final String ORACLE = "Oracle";

//    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {Validator.require})
//    public Boolean asServiceName;
//

    @FormField(validate = Validator.require)
    public ConnEntity connEntity;

    @FormField(ordinal = 8, type = FormFieldType.ENUM, validate = {Validator.require})
    public Boolean allAuthorized;

    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public String getEscapeChar() {
        return "\"";
    }

    @Override
    public String getDbName() {
        return "default";
    }

    @Override
    public String buidJdbcUrl(DBConfig db, String ip, String dbName) {
//        String jdbcUrl = "jdbc:oracle:thin:@" + ip + ":" + this.port + (this.asServiceName ? "/" : ":") + dbName;
//        return jdbcUrl;
        return this.connEntity.buidJdbcUrl(ip, this.port);
    }

    @Override
    protected String getRefectTablesSql() {
        if (allAuthorized != null && allAuthorized) {
            return "SELECT owner ||'.'|| table_name FROM all_tables WHERE REGEXP_INSTR(table_name,'[\\.$]+') < 1";
        } else {
            //  return "SELECT tablespace_name ||'.'||  (TABLE_NAME) FROM user_tables WHERE REGEXP_INSTR(TABLE_NAME,'[\\.$]+') < 1 AND tablespace_name is not null";
            // 带上 tablespace的话后续取colsMeta会取不出
            return "SELECT  (TABLE_NAME) FROM user_tables WHERE REGEXP_INSTR(TABLE_NAME,'[\\.$]+') < 1 AND tablespace_name is not null";

        }
    }

    @Override
    public String toString() {
        return "{" +
                // "asServiceName=" + asServiceName +
                ", allAuthorized=" + allAuthorized +
                ", name='" + name + '\'' +
                ", dbName='" + dbName + '\'' +
                ", userName='" + userName + '\'' +
                ", password='********" + '\'' +
                ", nodeDesc='" + nodeDesc + '\'' +
                ", port=" + port +
                ", encode='" + encode + '\'' +
                ", extraParams='" + extraParams + '\'' +
                '}';
    }

    @Override
    protected ResultSet getColumnsMeta(EntityName table, DatabaseMetaData metaData1) throws SQLException {
        return getColRelevantMeta(table, (tab) -> {
            try {
                return metaData1.getColumns(null
                        , tab.owner.isPresent() ? tab.owner.get() : null, tab.tabName, null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected ResultSet getPrimaryKeys(EntityName table, DatabaseMetaData metaData1) throws SQLException {

        return getColRelevantMeta(table, (tab) -> {
            try {

                return metaData1.getPrimaryKeys(null
                        , tab.owner.isPresent() ? tab.owner.get() : null, tab.tabName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ResultSet getColRelevantMeta(EntityName table
            , Function<OracleTab, ResultSet> containSchema) throws SQLException {
        try {
            return containSchema.apply(OracleTab.create(table.getFullName()));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    //
    @Override
    protected DataType createColDataType(String colName, String typeName, int dbColType, int colSize) throws SQLException {
        // 类似oracle驱动内部有一套独立的类型 oracle.jdbc.OracleTypes,有需要可以在具体的实现类里面去实现
        return new DataType(convert2JdbcType(dbColType), typeName, colSize);
    }

    private int convert2JdbcType(int dbColType) {
        switch (dbColType) {
//            case OracleTypes.ARRAY:
//            case OracleTypes.BIGINT:
            case BIT:// = -7;
                return Types.BIT;
            case TINYINT: // = -6;
                return Types.TINYINT;
            case SMALLINT: // = 5;
                return Types.SMALLINT;
            case INTEGER: // = 4;
                return Types.INTEGER;
            case BIGINT: // = -5;
                return Types.BIGINT;
            case FLOAT: // = 6;
                return Types.FLOAT;
            case REAL: // = 7;
            case BINARY_FLOAT:
                return Types.REAL;
            case DOUBLE: // = 8;
            case BINARY_DOUBLE:
                return Types.DOUBLE;
            case NUMERIC: // = 2;
            case DECIMAL: // = 3;
                //https://wenku.baidu.com/view/7f973fd54593daef5ef7ba0d4a7302768e996f35.html
                return Types.DECIMAL;
            case CHAR: // = 1;
                return Types.CHAR;
            case VARCHAR: // = 12;
                return Types.VARCHAR;
            case LONGVARCHAR: // = -1;
                return Types.LONGVARCHAR;
            case DATE: // = 91;
                return Types.DATE;
            case TIME: //= 92;
                return Types.TIME;
            case TIMESTAMP: // = 93;
                return Types.TIMESTAMP;
            case PLSQL_BOOLEAN: // = 252;
                return Types.BOOLEAN;
            default:
                return dbColType;
        }
    }

    @Override
    protected DataType getDataType(String keyName, ResultSet cols) throws SQLException {

//        int columnCount = cols.getMetaData().getColumnCount();
//        String colName = null;
//        for (int i = 1; i <= columnCount; i++) {
//            colName = cols.getMetaData().getColumnName(i);
//            System.out.print(colName + ":" + cols.getString(colName) + ",");
//        }
//        System.out.println();

        DataType type = super.getDataType(keyName, cols);
        // Oracle会将int，smallint映射到Oracle数据库都是number类型，number类型既能表示浮点和整型，所以这里要用进度来鉴别是整型还是浮点
        if (type.type == Types.DECIMAL || type.type == Types.NUMERIC) {
            int decimalDigits = type.getDecimalDigits();// cols.getInt("decimal_digits");
            if (decimalDigits < 1) {
                return new DataType(type.columnSize > 8 ? Types.BIGINT : Types.INTEGER, type.typeName, type.columnSize);
            }
        }

        if ("DATE".equalsIgnoreCase(type.typeName)) {
            return new DataType(Types.DATE, type.typeName, type.columnSize);
        }


        return type;
    }

    @Override
    public Connection getConnection(String jdbcUrl) throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
//        return DriverManager.getConnection(jdbcUrl
//                , StringUtils.trimToNull(this.asServiceName ? "system" : this.userName), StringUtils.trimToNull(password));

        return DriverManager.getConnection(jdbcUrl
                , StringUtils.trimToNull(this.userName), StringUtils.trimToNull(password));
    }


    @TISExtension
    public static class DefaultDescriptor extends BasicRdbmsDataSourceFactoryDescriptor {

        @Override
        protected String getDataSourceName() {
            return ORACLE;
        }

        @Override
        public boolean supportFacade() {
            return false;
        }

        @Override
        public boolean validateExtraParams(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return true;
        }
    }
}
