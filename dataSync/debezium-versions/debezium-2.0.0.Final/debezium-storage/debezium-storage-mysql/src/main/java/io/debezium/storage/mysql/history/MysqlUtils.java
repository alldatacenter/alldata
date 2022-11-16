package io.debezium.storage.mysql.history;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * JDBCUtils 获取Druid Connection
 * @author AllDataDC
 * @date 2022/11/15
 */
public class MysqlUtils {
    private static DataSource dataSource;
    private static Connection connection;

    public static Connection getConnection(String jdbcUrl, String username, String password) throws SQLException {
        if (connection == null || connection.isClosed()) {
            Map<String, String> jdbcMap = new HashMap<String, String>();
            jdbcMap.put("driverClassName", "com.mysql.jdbc.Driver");
            jdbcMap.put("url", jdbcUrl);
            jdbcMap.put("username", username);
            jdbcMap.put("password", password);
            jdbcMap.put("initialSize", "5");
            jdbcMap.put("maxActive", "10");
            jdbcMap.put("maxWait", "3000");
            Properties properties = new Properties();
            properties.putAll(jdbcMap);
            try {
                dataSource = DruidDataSourceFactory.createDataSource(properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
            connection = dataSource.getConnection();
        }
        return connection;
    }

    // 释放资源
    public static void close(ResultSet resultSet, Statement statement, Connection connection) throws SQLException {
        if (resultSet != null) {
            resultSet.close();
        }
        if (connection != null) {
            connection.close();
        }
        if (statement != null) {
            statement.close();

        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }


}