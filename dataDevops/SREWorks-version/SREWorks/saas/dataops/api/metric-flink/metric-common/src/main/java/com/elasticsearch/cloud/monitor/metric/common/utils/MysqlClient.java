package com.elasticsearch.cloud.monitor.metric.common.utils;

import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库客户端
 *
 * @author: fangzong.lyj
 * @date: 2021/09/05 13:42
 */
public class MysqlClient {

    private static DataSource dataSource = null;

    public static DataSource getDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        return buildDataSource();

    }

    private static synchronized DataSource buildDataSource() {
        if (dataSource == null) {
            Map<String, String> buildInProps = new HashMap<>();
            buildInProps.put("driverClassName", PropertiesUtil.getProperty("metric.datasource.driver-class-name"));
            buildInProps.put("url", PropertiesUtil.getProperty("metric.datasource.url"));
            buildInProps.put("username", PropertiesUtil.getProperty("metric.datasource.username"));
            buildInProps.put("password", PropertiesUtil.getProperty("metric.datasource.password"));

            dataSource = DataSourceBuilder.create().driverClassName(buildInProps.get("driverClassName"))
                    .url(buildInProps.get("url"))
                    .username(buildInProps.get("username"))
                    .password(buildInProps.get("password"))
                    .build();

        }

        return dataSource;
    }
}
