
package com.platform.modules.mnt.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.yeecode.dynamicdatasource.DynamicDataSource;
import com.github.yeecode.dynamicdatasource.model.DataSourceInfo;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import com.platform.utils.CloseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;

/**
 * @author AllDataDC
 */
@Slf4j
@Component
public class SqlUtils {

    @Autowired
    private static DynamicDataSource dynamicDataSource;//注入动态数据源

    /**
     * 获取数据源
     *
     * @param jdbcUrl /
     * @param userName /
     * @param password /
     * @return DataSource
     */
    private static DataSource getDataSource(String jdbcUrl, String userName, String password) {
        DruidDataSource druidDataSource = new DruidDataSource();
        String className;
        try {
            className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
        } catch (SQLException e) {
            throw new RuntimeException("Get class name error: =" + jdbcUrl);
        }
        if (StringUtils.isEmpty(className)) {
            DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(jdbcUrl);
            if (null == dataTypeEnum) {
                throw new RuntimeException("Not supported data type: jdbcUrl=" + jdbcUrl);
            }
            druidDataSource.setDriverClassName(dataTypeEnum.getDriver());
        } else {
            druidDataSource.setDriverClassName(className);
        }


        druidDataSource.setUrl(jdbcUrl);
        druidDataSource.setUsername(userName);
        druidDataSource.setPassword(password);
        // 配置获取连接等待超时的时间
        druidDataSource.setMaxWait(3000);
        // 配置初始化大小、最小、最大
        druidDataSource.setInitialSize(1);
        druidDataSource.setMinIdle(1);
        druidDataSource.setMaxActive(1);

        // 如果链接出现异常则直接判定为失败而不是一直重试
        druidDataSource.setBreakAfterAcquireFailure(true);
        try {
            druidDataSource.init();
        } catch (SQLException e) {
            log.error("Exception during pool initialization", e);
            throw new RuntimeException(e.getMessage());
        }

        return druidDataSource;
    }

    /**
     * 获取数据源
     *
     * @param jdbcUrl  /
     * @param userName /
     * @param password /
     * @return DataSource
     */

    private static Connection getConnection(String jdbcUrl, String userName, String password) {
        String dataSourceKey = String.valueOf(System.currentTimeMillis());
        DataSourceInfo dataSourceInfo = new DataSourceInfo(dataSourceKey,
                "com.p6spy.engine.spy.P6SpyDriver", jdbcUrl, userName, password);
        dynamicDataSource.addDataSource(dataSourceInfo, true);
        dynamicDataSource.switchDataSource(dataSourceKey);
        Connection connection = null;
        try {
            connection = dynamicDataSource.getConnection();
        } catch (Exception ignored) {
        }
        try {
            int timeOut = 5;
            if (null == connection || connection.isClosed() || !connection.isValid(timeOut)) {
                log.info("connection is closed or invalid, retry get connection!");
                connection = dynamicDataSource.getConnection();
            }
        } catch (Exception e) {
            log.error("create connection error, jdbcUrl: {}", jdbcUrl);
            throw new RuntimeException("create connection error, jdbcUrl: " + jdbcUrl);
        } finally {
            CloseUtil.close(connection);
        }
        return connection;
    }

    /**
     * 获取数据源
     *
     * @param jdbcUrl  /
     * @param userName /
     * @param password /
     * @return DataSource
     */

    private static Connection getDruidConnection(String jdbcUrl, String userName, String password) {
        DataSource dataSource = getDataSource(jdbcUrl, userName, password);
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
        } catch (Exception ignored) {}
        try {
            int timeOut = 5;
            if (null == connection || connection.isClosed() || !connection.isValid(timeOut)) {
                log.info("connection is closed or invalid, retry get connection!");
                connection = dataSource.getConnection();
            }
        } catch (Exception e) {
            log.error("create connection error, jdbcUrl: {}", jdbcUrl);
            throw new RuntimeException("create connection error, jdbcUrl: " + jdbcUrl);
        } finally {
            CloseUtil.close(connection);
        }
        return connection;
    }

    private static void releaseConnection(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                log.error("connection close error：" + e.getMessage());
            }
        }
    }

    public static boolean testConnection(String jdbcUrl, String userName, String password) {
        Connection connection = null;
        try {
            connection = getDruidConnection(jdbcUrl, userName, password);
            if (null != connection) {
                return true;
            }
        } catch (Exception e) {
            log.info("Get connection failed:" + e.getMessage());
        } finally {
            releaseConnection(connection);
        }
        return false;
    }

    public static String executeFile(String jdbcUrl, String userName, String password, File sqlFile) {
        Connection connection = getDruidConnection(jdbcUrl, userName, password);
        try {
            batchExecute(connection, readSqlList(sqlFile));
        } catch (Exception e) {
            log.error("sql脚本执行发生异常:{}", e.getMessage());
            return e.getMessage();
        } finally {
            releaseConnection(connection);
        }
        return "success";
    }


    /**
     * 批量执行sql
     *
     * @param connection /
     * @param sqlList    /
     */
    public static void batchExecute(Connection connection, List<String> sqlList) {
        Statement st = null;
        try {
            st = connection.createStatement();
            for (String sql : sqlList) {
                if (sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                st.addBatch(sql);
            }
            st.executeBatch();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            CloseUtil.close(st);
        }
    }

    /**
     * 将文件中的sql语句以；为单位读取到列表中
     *
     * @param sqlFile /
     * @return /
     * @throws Exception e
     */
    private static List<String> readSqlList(File sqlFile) throws Exception {
        List<String> sqlList = Lists.newArrayList();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(sqlFile), StandardCharsets.UTF_8))) {
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                log.info("line:{}", tmp);
                if (tmp.endsWith(";")) {
                    sb.append(tmp);
                    sqlList.add(sb.toString());
                    sb.delete(0, sb.length());
                } else {
                    sb.append(tmp);
                }
            }
            if (!"".endsWith(sb.toString().trim())) {
                sqlList.add(sb.toString());
            }
        }

        return sqlList;
    }

}
