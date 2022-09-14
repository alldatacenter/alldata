package com.alibaba.sreworks.job.taskhandler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.utils.Requests;

import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlContentAction {

    public static List<Map<String, Object>> run(MysqlContent mysqlContent) throws Exception {

        HttpResponse<String> response = Requests.get(
            "http://prod-dataops-pmdb.sreworks-dataops/datasource/getDatasourceById?id="
                + mysqlContent.getDatasourceId(), null, null);
        Requests.checkResponseStatus(response);
        JSONObject connectConfig = JSONObject.parseObject(response.body())
            .getJSONObject("data").getJSONObject("connectConfig");
        String host = connectConfig.getString("host");
        int port = connectConfig.getIntValue("port");
        String db = connectConfig.getString("db");
        String username = connectConfig.getString("username");
        String password = connectConfig.getString("password");

        Connection con = null;
        ResultSet rs = null;
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = String.format(
            "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false", host, port, db);

        Class.forName(driver);

        try {
            con = DriverManager.getConnection(url, username, password);
            Statement statement = con.createStatement();
            rs = statement.executeQuery(mysqlContent.getSql());
            return get(rs);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (con != null) {
                con.close();
            }
        }

    }

    private static List<Map<String, Object>> get(ResultSet rs) throws SQLException {

        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();

        while (rs.next()) {
            Map<String, Object> rowData = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                rowData.put(md.getColumnName(i), rs.getObject(i));
            }
            list.add(rowData);
        }
        return list;

    }

}
