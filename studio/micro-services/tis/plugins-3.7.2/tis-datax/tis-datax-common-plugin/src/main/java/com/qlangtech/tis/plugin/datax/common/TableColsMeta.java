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

package com.qlangtech.tis.plugin.datax.common;

import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.Memoizer;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-07 10:20
 **/
public class TableColsMeta extends Memoizer<String, Map<String, ColumnMetaData>> implements Closeable {
    private final DataSourceFactory datasource;
    private final String dbName;

    private final DataSourceMeta.JDBCConnection connection;


    public TableColsMeta(DataSourceFactory datasource, String dbName) {
        this.datasource = datasource;
        this.dbName = dbName;
        final DBConfig dbConfig = datasource.getDbConfig();

        AtomicReference<DataSourceMeta.JDBCConnection> conn = new AtomicReference<>();
        try {
            dbConfig.vistDbName((config, jdbcUrl, ip, dbname) -> {
                conn.set(datasource.getConnection(jdbcUrl));
                return true;
            });
            this.connection = Objects.requireNonNull(conn.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, ColumnMetaData> compute(String tab) {


        Objects.requireNonNull(datasource, "ds:" + dbName + " relevant DataSource can not be find");

        try {
            return datasource.getTableMetadata(connection, false, EntityName.parse(tab))
                    .stream().collect(
                            Collectors.toMap(
                                    (m) -> m.getKey()
                                    , (m) -> m
                                    , (c1, c2) -> c1));
        } catch (TableNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
