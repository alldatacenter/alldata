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
package io.datavines.registry.plugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class MysqlMutex {

    private Connection connection;
    private final Properties properties;

    public MysqlMutex(Connection connection, Properties properties) throws SQLException {
        this.connection = connection;
        this.properties = properties;
    }

    public boolean acquire(String key, long time) throws SQLException {
        String sql = String.format("select get_lock(%s,%d)", key ,time);
        return executeSql(sql);
    }

    public boolean release(String key) throws SQLException {
        String sql = String.format("select release_lock(%s)", key);
        return executeSql(sql);
    }

    private boolean executeSql(String sql) throws SQLException {

        if (connection == null || connection.isClosed() || !connection.isValid(10)) {
            connection = ConnectionUtils.getConnection(properties);
        }

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);

        if (resultSet == null) {
            return false;
        }

        if (resultSet.first()) {
            int result = resultSet.getInt(1);
            resultSet.close();
            statement.close();
            return result >= 1;
        } else {
            resultSet.close();
            statement.close();
            return false;
        }
    }

    public boolean isUsedLock(String key) throws SQLException {
        String sql = String.format("select is_used_lock(%s)", key);
        return executeSql(sql);
    }

    public boolean isFreeLock(String key) throws SQLException {
        String sql = String.format("select is_free_lock(%s)", key);
        return executeSql(sql);
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
