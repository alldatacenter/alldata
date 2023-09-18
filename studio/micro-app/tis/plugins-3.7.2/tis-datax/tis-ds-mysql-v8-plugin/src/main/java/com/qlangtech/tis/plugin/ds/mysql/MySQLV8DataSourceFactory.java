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

package com.qlangtech.tis.plugin.ds.mysql;

import com.mysql.cj.conf.PropertyDefinitions;
import com.mysql.cj.conf.PropertyKey;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-08 21:47
 **/
@Public
public class MySQLV8DataSourceFactory extends MySQLDataSourceFactory implements DataSourceFactory.ISchemaSupported {

    private transient com.mysql.cj.jdbc.Driver mysql8Driver;

    @Override
    public JDBCConnection getConnection(String jdbcUrl) throws SQLException {

        if (mysql8Driver == null) {
            mysql8Driver = new com.mysql.cj.jdbc.Driver();
        }
        Properties props = new Properties();
        props.put(PropertyKey.USER.getKeyName(), StringUtils.trimToNull(this.userName));
        props.put(PropertyKey.PASSWORD.getKeyName(), StringUtils.trimToEmpty(password));
        //
        /**
         * https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-connp-props-connection.html
         * MySQL uses the term "schema" as a synonym of the term "database," while Connector/J historically takes the JDBC term "catalog" as synonymous to "database". This property sets for Connector/J which of the JDBC terms "catalog" and "schema" is used in an application to refer to a database. The property takes one of the two values "CATALOG" or "SCHEMA" and uses it to determine (1) which Connection methods can be used to set/get the current database (e.g. 'setCatalog()' or 'setSchema()'?), (2) which arguments can be used within the various 'DatabaseMetaData' methods to filter results (e.g. the catalog or 'schemaPattern' argument of 'getColumns()'?), and (3) which fields in the result sets returned by 'DatabaseMetaData' methods contain the database identification information (i.e., the 'TABLE_CAT' or 'TABLE_SCHEM' field in the result set returned by 'getTables()'?).
         * add for : fix multi table with same name located in mulit DataBase ,will get mulit names invoking DatabaseMetaData.getTables()
         */
        props.put(PropertyKey.databaseTerm.getKeyName(), String.valueOf(PropertyDefinitions.DatabaseTerm.SCHEMA));
        // 为了避开与Mysql5的连接冲突，需要直接从driver中创建connection对象
        return new JDBCConnection(mysql8Driver.connect(jdbcUrl, props), jdbcUrl);
    }

    @Override
    public void setReaderStatement(Statement stmt) throws SQLException {
        com.mysql.cj.jdbc.JdbcStatement statement = (com.mysql.cj.jdbc.JdbcStatement) stmt;
        // statement.enableStreamingResults();
        statement.setFetchSize(0);
        statement.enableStreamingResults();
    }

    /**
     * mysql 中schema 和 database 是相同概念
     *
     * @return
     */
    @Override
    public String getDBSchema() {
        // return null;
        return this.dbName;
    }

    @TISExtension
    public static class V8Descriptor extends DefaultDescriptor {
        @Override
        protected String getDataSourceName() {
            return DS_TYPE_MYSQL_V8;
        }
    }
}
