package com.alibaba.sreworks.dataset.connection;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.datasource.DataSourceService;
import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    DataSourceService dsService;

    private transient Cache<String, RestHighLevelClient> hlDataSourceCache;

    private transient Cache<String, RestClient> dataSourceCache;

    private static class RestHighLevelClientRemovalListener implements RemovalListener<String, RestHighLevelClient> {
        @Override
        public void onRemoval(RemovalNotification<String, RestHighLevelClient> removalNotification) {
            log.info(String.format("Remove Rest High Level Client: key=%s, value=%s, reason=%s", removalNotification.getKey(), removalNotification.getValue(), removalNotification.getCause()));
            try {
                removalNotification.getValue().close();
            } catch (Exception ex) {
                log.warn(String.format("Remove Rest High Level Client Exception:%s", ex.getMessage()));
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        dataSourceCache = CacheBuilder.newBuilder().expireAfterAccess(Constant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).maximumSize(Constant.CACHE_MAX_SIZE).build();
//        hlDataSourceCache = CacheBuilder.newBuilder().removalListener(new RestHighLevelClientRemovalListener()).expireAfterAccess(Constant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).maximumSize(Constant.CACHE_MAX_SIZE).build();
        hlDataSourceCache = CacheBuilder.newBuilder().removalListener(new RestHighLevelClientRemovalListener()).maximumSize(Constant.CACHE_MAX_SIZE).build();

    }

    public synchronized RestClient getLowLevelClient(String dataSourceId) {
        RestClient client = dataSourceCache.getIfPresent(dataSourceId);
        if (client == null) {
            client = reconstructRestLowLevelClient(dataSourceId);
        }
        return client;
    }

    public synchronized RestHighLevelClient getHighLevelClient(String dataSourceId) {
        RestHighLevelClient hlClient = hlDataSourceCache.getIfPresent(dataSourceId);
        if (hlClient == null) {
            hlClient = reconstructRestHighLevelClient(dataSourceId);
        }
        return hlClient;
    }

//    public synchronized RestClient resetLowLevelClient(Integer dataSourceId) throws IOException {
//        RestClient client = dataSourcePool.getOrDefault(dataSourceId, null);
//        if (client != null) {
//            client.close();
//        }
//
//        return this.buildRestLowLevelClient(dataSourceId);
//    }
//
//    public synchronized RestHighLevelClient resetHighLevelClient(Integer dataSourceId) throws IOException {
//        RestHighLevelClient hlClient = hlDataSourcePool.getOrDefault(dataSourceId, null);
//        if (hlClient != null) {
//            hlClient.close();
//        }
//
//        return this.buildRestHighLevelClient(dataSourceId);
//    }

    private synchronized RestClient reconstructRestLowLevelClient(String dataSourceId) {
        log.info("====reconstructESRestLowLevelClient====");
        JSONObject dsObject = dsService.getDataSourceById(dataSourceId);
        if (dsObject.isEmpty()) {
            return null;
        }

        JSONObject connectConfig = dsObject.getJSONObject("connectConfig");

        final CredentialsProvider credentialsProvider = buildCredentialsProvider(connectConfig.getString("username"), connectConfig.getString("password"));

        RestClient client = RestClient.builder(
                new HttpHost(connectConfig.getString("host"), connectConfig.getIntValue("port"), connectConfig.getString("schema"))
        ).setHttpClientConfigCallback(
                httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
        ).build();

        dataSourceCache.put(dataSourceId, client);

        return client;
    }

    private synchronized RestHighLevelClient reconstructRestHighLevelClient(String dataSourceId) {
        log.info("====reconstructESRestHighLevelClient====");
        JSONObject dsObject = dsService.getDataSourceById(dataSourceId);
        if (dsObject.isEmpty()) {
            return null;
        }

        JSONObject connectConfig = dsObject.getJSONObject("connectConfig");

        final CredentialsProvider credentialsProvider = buildCredentialsProvider(connectConfig.getString("username"), connectConfig.getString("password"));

        RestHighLevelClient hlClient = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(connectConfig.getString("host"), connectConfig.getIntValue("port"), connectConfig.getString("schema"))
            ).setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setKeepAliveStrategy((response, context) -> TimeUnit.MINUTES.toMillis(10)))
        );

        hlDataSourceCache.put(dataSourceId, hlClient);

        return hlClient;
    }

    private CredentialsProvider buildCredentialsProvider(String username, String password) {
        CredentialsProvider credentialsProvider = null;
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        }
        return credentialsProvider;

    }
}
