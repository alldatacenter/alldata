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
package com.qlangtech.tis.plugin.ds;


import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 数据源meta信息获取
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-07 15:51
 */
public interface DataSourceMeta extends Describable.IRefreshable, IDBReservedKeys {


    /**
     * Get all the tables in dataBase
     *
     * @return
     */
    default TableInDB getTablesInDB() {
        throw new UnsupportedOperationException();
    }


    /**
     * Get table column metaData list
     *
     * @param inSink 是否在处理sink数据源 https://github.com/qlangtech/tis/issues/192,在处理oracle的Date类型时：inSink：true 则要定义成sql.date false: 保持datetime类型
     * @param table
     * @return
     * @throws TableNotFoundException
     */
    default List<ColumnMetaData> getTableMetadata(boolean inSink, EntityName table) throws TableNotFoundException {
        throw new UnsupportedOperationException("invoke from:" + this.getClass().getName());
    }


    /**
     * Get table column metaData list
     *
     * @param inSink 是否在执行sink流程 https://github.com/qlangtech/tis/issues/192
     * @param table
     * @return
     */
    default List<ColumnMetaData> getTableMetadata(JDBCConnection conn, boolean inSink, EntityName table) throws TableNotFoundException {
        throw new UnsupportedOperationException();
    }

    public class JDBCConnection implements AutoCloseable {
        private final Connection conn;
        private final String url;

        public JDBCConnection(Connection conn, String url) {
            this.conn = conn;
            this.url = url;
        }

        public Statement createStatement() throws SQLException {
            return this.conn.createStatement();
        }

        public Connection getConnection() {
            return this.conn;
        }

        public String getUrl() {
            return this.url;
        }

        @Override
        public void close() throws SQLException {
            this.conn.close();
        }

        /**
         * 执行一个查询语句
         *
         * @param sql
         * @param resultProcess
         * @throws Exception
         */
        public void query(String sql, ResultProcess resultProcess) throws Exception {
            synchronized (JDBCConnection.class) {
                try (Statement stmt = conn.createStatement()) {
                    try {
                        try (ResultSet result = stmt.executeQuery(sql)) {
                            while (result.next()) {
                                if (!resultProcess.callback(result)) {
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(sql, e);
                    }
                }
            }
        }

        public boolean execute(String sql) throws Exception {
            synchronized (JDBCConnection.class) {
                try (Statement stmt = conn.createStatement()) {
                    return stmt.execute(sql);
                }
            }
        }

    }

    public interface ResultProcess {

        /**
         * @param result
         * @return false: 中断执行
         * @throws Exception
         */
        public boolean callback(ResultSet result) throws Exception;
    }
}
