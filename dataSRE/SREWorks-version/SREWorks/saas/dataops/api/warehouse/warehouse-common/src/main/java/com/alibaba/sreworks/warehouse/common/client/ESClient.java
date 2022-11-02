package com.alibaba.sreworks.warehouse.common.client;

import com.alibaba.sreworks.warehouse.common.constant.Constant;
import com.alibaba.sreworks.warehouse.common.properties.ApplicationProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * ES客户端
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 11:18
 */
@Repository
@Scope("singleton")
@Slf4j
public class ESClient implements InitializingBean {

    @Autowired
    ApplicationProperties properties;

    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    private transient Cache<String, RestHighLevelClient> hlDataSourceCache;
    private transient Cache<String, RestClient> dataSourceCache;

    @Override
    public void afterPropertiesSet() {
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(properties.getEsUsername(), properties.getEsPassword()));

//        dataSourceCache = CacheBuilder.newBuilder().expireAfterAccess(Constant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).maximumSize(Constant.CACHE_MAX_SIZE).build();
        hlDataSourceCache = CacheBuilder.newBuilder().expireAfterAccess(Constant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).maximumSize(Constant.CACHE_MAX_SIZE).removalListener(buildCacheRemoveListener()).build();
    }

    private RemovalListener<String, RestHighLevelClient> buildCacheRemoveListener() {
        return notification -> {
            log.warn("[" + notification.getKey() + ":" + notification.getValue() + "]缓存删除");
            try {
                notification.getValue().close();
            } catch (Exception ex) {
                log.error(String.format("ES链接池关闭失败, %s", ex.getMessage()));
            }
        };
    }

    public synchronized RestClient getLowLevelClient() {
        RestClient client = dataSourceCache.getIfPresent(Constant.DW_DB_NAME);
        if (client == null) {
            client = reconstructRestLowLevelClient();
        }
        return client;
    }

    public synchronized RestHighLevelClient getHighLevelClient() {
        RestHighLevelClient hlClient = hlDataSourceCache.getIfPresent(Constant.DW_DB_NAME);
        if (hlClient == null) {
            hlClient = reconstructRestHighLevelClient();
        }
        return hlClient;
    }

    private synchronized RestClient reconstructRestLowLevelClient() {
        log.info("====reconstructESRestLowLevelClient====");
        RestClient client = RestClient.builder(
                new HttpHost(properties.getEsHost(), properties.getEsPort(), properties.getEsProtocol())
        ).setHttpClientConfigCallback(
                httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
        ).build();

        dataSourceCache.put(Constant.DW_DB_NAME, client);

        return client;
    }

    private synchronized RestHighLevelClient reconstructRestHighLevelClient() {
        log.info("====reconstructESRestHighLevelClient====");

        RestHighLevelClient hlClient = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(properties.getEsHost(), properties.getEsPort(), properties.getEsProtocol())
            ).setHttpClientConfigCallback(
                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            )
        );

        hlDataSourceCache.put(Constant.DW_DB_NAME, hlClient);

        return hlClient;
    }
}
