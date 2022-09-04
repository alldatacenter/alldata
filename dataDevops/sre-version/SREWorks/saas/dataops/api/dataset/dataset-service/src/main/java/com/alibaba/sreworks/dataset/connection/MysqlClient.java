package com.alibaba.sreworks.dataset.connection;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.datasource.DataSourceService;
import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Mysql客户端
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 11:18
 */
@Repository
@Scope("singleton")
@Slf4j
public class MysqlClient implements InitializingBean {
    @Autowired
    DataSourceService dsService;

    private transient Cache<String, DataSource> dataSourceCaches;

    @Override
    public void afterPropertiesSet() {
        dataSourceCaches = CacheBuilder.newBuilder().expireAfterAccess(Constant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).maximumSize(Constant.CACHE_MAX_SIZE).build();
    }

    private synchronized DataSource reconstructDataSource(String dataSourceId) {
        log.info("====reconstructMysqlDataSource====");
        JSONObject dsObject = dsService.getDataSourceById(dataSourceId);
        if (dsObject.isEmpty()) {
            return null;
        }

        JSONObject connectConfig = dsObject.getJSONObject("connectConfig");
        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false",
                connectConfig.getString("host"), connectConfig.getIntValue("port"), connectConfig.getString("db"));

        DataSource dataSource = DataSourceBuilder.create().driverClassName("com.mysql.jdbc.Driver")
                .url(url)
                .username(connectConfig.getString("username"))
                .password(connectConfig.getString("password"))
                .build();

        dataSourceCaches.put(dataSourceId, dataSource);
        return dataSource;
    }

    public synchronized DataSource getDataSource(String dataSourceId) {
        DataSource dataSource = dataSourceCaches.getIfPresent(dataSourceId);
        if (dataSource == null) {
            dataSource = reconstructDataSource(dataSourceId);
        }
       return dataSource;
    }
}
