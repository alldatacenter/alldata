package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * @author jialiang.tjl
 */
@Data
@Slf4j
public class MysqlHelper {

    private static final String PRESTO = "presto";
    private static final int SINGLE_QUERY_MAX = 1000;
    /**
     * 在使用 ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY 依赖无效的情况下，强制使用maxRows来控制最大行数
     */
    private static final int MAX_ROWS = 1000000;
    private static Map<String, Connection> connectionMap = new Hashtable<>();
    private Connection conn;
    private int fetchSize = Constant.FETCH_DATA_SIZE;
    private String url;

    public MysqlHelper(JSONObject config) throws SQLException {
        this(
            config.getString("host"),
            config.getString("port"),
            config.getString("db"),
            config.getString("username"),
            config.getString("password")
        );
    }

    public MysqlHelper(String protocol, String host, String port, String db, String username, String password)
        throws SQLException {
        if (PRESTO.equals(protocol)) {
            url = String.format("jdbc:presto://%s:%s/%s", host, port, db);
        } else {
            url = String.format("jdbc:mysql://%s:%s/%s?"
                + "useUnicode=yes&"
                + "characterEncoding=UTF-8&"
                + "useSSL=false&"
                + "zeroDateTimeBehavior=convertToNull", host, port, db);
        }
        if (connectionMap.containsKey(url) && connectionMap.get(url).isValid(1)) {
            conn = connectionMap.get(url);
        } else {
            log.info(String.format("new mysqlHelper conn: %s %s", host, port));
            if (StringUtils.isEmpty(password)) {
                conn = DriverManager.getConnection(url, "test", null);
            } else {
                conn = DriverManager.getConnection(url, username, password);
            }
            connectionMap.put(url, conn);
        }
    }

    public MysqlHelper(String host, String port, String db, String username, String password) throws SQLException {

        this("mysql", host, port, db, username, password);

    }

    public int executeCreateStatement(String sql) throws SQLException {
        return conn.createStatement().executeUpdate(sql);
    }

    /**
     * 注意，为了避免一次查询产生了过多的数据，本查询最多返回前1000行
     * @param sql
     * @return
     * @throws SQLException
     */
    public List<JSONObject> executeQuery(String sql) throws SQLException {
        List<JSONObject> result = new LinkedList<>();
        executeQuery(sql, SINGLE_QUERY_MAX, result::addAll, true);
        return result;
    }

    public void executeQuery(String sql, int fetchSize, FetchDataCallback callback) throws SQLException {
        executeQuery(sql, fetchSize, callback, false);
    }

    private void executeQuery(String sql, int fetchSize, FetchDataCallback callback, boolean onlyFirst) throws SQLException {
        log.debug("execute sql={} fetchSize={}", sql, fetchSize);
        try (PreparedStatement pst = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            pst.setMaxRows(MAX_ROWS);
            pst.setFetchSize(fetchSize);
            try (ResultSet rs = pst.executeQuery()){
                log.debug("sample {}", sql);
                fetchData(rs, fetchSize, callback, onlyFirst);
            }
        }
    }

    public void executeUpdate(String sql) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(sql)){
            pst.setFetchSize(fetchSize);
            log.info("写入数据, 执行sql: " + sql);
            pst.executeUpdate();
        }
    }

    private List<JSONObject> fetchData(ResultSet rs, int fetchSize, FetchDataCallback callback, boolean onlyFirst) throws SQLException {
        fetchSize = fetchSize == 0 ? this.fetchSize : fetchSize;
        List<JSONObject> retList = new ArrayList<>();

        while (rs.next()) {
            JSONObject jsonObject = new JSONObject();
            for (int index = 1; index <= rs.getMetaData().getColumnCount(); index++) {
                jsonObject.put(rs.getMetaData().getColumnLabel(index), rs.getString(index));
            }
            retList.add(jsonObject);
            if (retList.size() >= fetchSize) {
                if (onlyFirst) {
                    break;
                }
                callback.fetch(retList);
                retList.clear();
            }
        }

        if (!retList.isEmpty()) {
            callback.fetch(retList);
        }

        return retList;
    }
}
