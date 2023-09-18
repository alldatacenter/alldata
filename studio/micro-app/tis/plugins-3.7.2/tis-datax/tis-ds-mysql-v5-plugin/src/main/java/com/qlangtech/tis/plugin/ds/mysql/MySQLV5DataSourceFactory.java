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

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-08 21:47
 **/
@Public
public class MySQLV5DataSourceFactory extends MySQLDataSourceFactory {
//    static {
//        try {
    //   DriverManager.registerDriver(new com.mysql.jdbc.Driver());
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private transient com.mysql.jdbc.Driver driver;

    @Override
    public JDBCConnection getConnection(String jdbcUrl) throws SQLException {
        if (driver == null) {
            driver = new com.mysql.jdbc.Driver();
        }
        java.util.Properties info = new java.util.Properties();

        if (this.userName != null) {
            info.put("user", this.userName);
        }
        if (password != null) {
            info.put("password", password);
        }
        return new JDBCConnection(driver.connect(jdbcUrl, info), jdbcUrl);
    }

    @Override
    public void setReaderStatement(Statement stmt) throws SQLException {
        com.mysql.jdbc.Statement statement = (com.mysql.jdbc.Statement) stmt;
        statement.enableStreamingResults();
        //statement.setFetchSize(0);
    }

    @TISExtension
    public static class V5Descriptor extends DefaultDescriptor {
        @Override
        protected String getDataSourceName() {
            return DS_TYPE_MYSQL_V5;
        }
    }
}
