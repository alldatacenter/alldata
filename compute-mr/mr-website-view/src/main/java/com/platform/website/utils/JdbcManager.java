package com.platform.website.utils;

import com.platform.website.common.GlobalConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.conf.Configuration;

public class JdbcManager {

  /**
   * 根据配置获取关系型数据库的jdbc连接
   *
   * @param conf hadoop配置信息
   * @param flag 区分不同数据源的flag
   */
  public static Connection getConnection(Configuration conf, String flag)
      throws SQLException {

    String driverStr = String.format(GlobalConstants.JDBC_DRIVE, flag);
    String urlStr = String.format(GlobalConstants.JDBC_URL, flag);
    String usernameStr = String.format(GlobalConstants.JDBC_USERNAME, flag);
    String passwordStr = String.format(GlobalConstants.JDBC_PASSWORD, flag);

    String driverClass = conf.get(driverStr);
    String url = conf.get(urlStr);
    String username = conf.get(usernameStr);
    String password = conf.get(passwordStr);

    try {
      Class.forName(driverClass);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    return DriverManager.getConnection(url, username, password);

  }

  public static void close(Connection connection, Statement statement, ResultSet resultSet){
    if (resultSet != null){
      try{
        resultSet.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    if (statement != null){
      try{
        statement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    if (connection != null){
      try{
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

}
