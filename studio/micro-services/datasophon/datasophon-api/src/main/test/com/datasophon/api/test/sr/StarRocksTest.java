package com.datasophon.api.test.sr;

import org.junit.Test;

import java.sql.*;

public class StarRocksTest {
    @Test
    public void testShowProc() throws SQLException, ClassNotFoundException {
        String username="root";
        String password = "";
        String url = "jdbc:mysql://ddp2:9030";
        //加载驱动
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement();
        String sql = "show proc '/frontends'";
        //执行sql，返回结果集
        ResultSet resultSet = statement.executeQuery(sql);
        int columnCount = resultSet.getMetaData().getColumnCount();

        //结果封装
        while (resultSet.next()){
            String name = resultSet.getString("Name");
            System.out.println(name);
        }
    }
    @Test
    public void testAddFollower() throws SQLException, ClassNotFoundException {
        String username = "root";
        String password = "";
        String url = "jdbc:mysql://ddp2:9030";
        //加载驱动
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement();
        String sql = " ALTER SYSTEM ADD FOLLOWER \"ddp3:9010\";";
        //执行sql，返回结果集
        statement.executeUpdate(sql);
    }
}
