package com.elasticsearch.cloud.monitor.metric.mapping;

import com.elasticsearch.cloud.monitor.metric.common.pojo.CommonPojo.EsClusterConf;
import com.elasticsearch.cloud.monitor.metric.mapping.pojo.MetricMappingInfo;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author xiaoping
 * @date 2021/6/24
 */

public class MappingUtil {
    private static final Log log = LogFactory.getLog(MappingUtil.class);
    private static final int TIME_OUT = 3 * 60 * 1000;
    public static int MAX_CONN_PER_ROUTE = 500;
    public static int MAX_CONN_TOTAL = 500;
    public static Gson gson = new Gson();

    public static RestHighLevelClient createHighLevelClient(EsClusterConf es) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(es.getUser(), es.getPassword()));
        RestClientBuilder clientBuilder = RestClient.builder(
            new HttpHost(es.getEsHost(), es.getPort(), es.getHttpType()));

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
            .setConnectTimeout(TIME_OUT)
            .setSocketTimeout(TIME_OUT)
            .setConnectionRequestTimeout(TIME_OUT);

        clientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {

            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                httpClientBuilder.disableAuthCaching();
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
                httpClientBuilder.setMaxConnTotal(MAX_CONN_TOTAL);
                httpClientBuilder.setMaxConnPerRoute(MAX_CONN_PER_ROUTE);
                return httpClientBuilder;
            }
        });
        clientBuilder.setMaxRetryTimeoutMillis(TIME_OUT);
        return new RestHighLevelClient(clientBuilder);
    }

    public static String md5(String input) {
        return Hashing.md5().newHasher().putString(input, Charsets.UTF_8).hash().toString();
    }

    public static Long getTotal(RestHighLevelClient highLevelClient, String indexName,
        QueryBuilder query)
        throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse.getHits().getTotalHits();
    }

    public static void updateTenantAccessTime(RestHighLevelClient highLevelClient, Map<String, Long> tenantAccessTime,
        String indexName) throws IOException {
        if (tenantAccessTime.size() == 0) {
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        int step = 50000;
        int count = 0;
        for (Entry<String, Long> entry : tenantAccessTime.entrySet()) {
            IndexRequest indexRequest = new IndexRequest(indexName);
            indexRequest.id(md5("@access_time" + entry.getKey()));
            Map<String, Object> body = Maps.newHashMap();
            body.put("tenant", entry.getKey());
            body.put("timestamp", entry.getValue());
            indexRequest.source(body);
            indexRequest.type("_doc");
            bulkRequest.add(indexRequest);
            count++;
            if (count >= step) {
                highLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                bulkRequest = new BulkRequest();
                count = 0;
            }
        }
        if (count > 0) {
            highLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        }
        ForceMergeRequest request = new ForceMergeRequest(indexName);
        request.onlyExpungeDeletes(true);
        highLevelClient.indices().forcemergeAsync(request, RequestOptions.DEFAULT,
            new ActionListener<ForceMergeResponse>() {
                @Override
                public void onResponse(ForceMergeResponse forceMergeResponse) {

                }

                @Override
                public void onFailure(Exception e) {
                    log.error("force merge error " + e.getMessage(), e);
                }
            });
    }

    public static List<MetricMappingInfo> scanMappingInfo(RestHighLevelClient highLevelClient, String indexName,
        QueryBuilder query)
        throws IOException {

        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        searchSourceBuilder.size(10000);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        SearchHit[] searchHits = searchResponse.getHits().getHits();
        List<MetricMappingInfo> result = Lists.newArrayList();
        for (SearchHit searchHit : searchHits) {
            String sourceAsString = searchHit.getSourceAsString();
            result.add(gson.fromJson(sourceAsString, MetricMappingInfo.class));
        }

        String scrollId = searchResponse.getScrollId();
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = highLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();
            if (searchHits != null && searchHits.length > 0) {
                for (SearchHit searchHit : searchHits) {
                    String sourceAsString = searchHit.getSourceAsString();
                    result.add(gson.fromJson(sourceAsString, MetricMappingInfo.class));
                }
            }
        }
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = highLevelClient.clearScroll(clearScrollRequest,
            RequestOptions.DEFAULT);

        boolean succeeded = clearScrollResponse.isSucceeded();
        if (!succeeded) {
            log.error("clear Scroll error");
        }
        return result;
    }

    public static boolean addMappingInfo(RestHighLevelClient highLevelClient, String indexName, String pk,
        String tenant, String metric, int number, boolean isMetricPrimaryKey)
        throws IOException {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.opType(OpType.CREATE);
        indexRequest.id(pk);
        Map<String, Object> body = Maps.newHashMap();
        body.put("tenant", tenant);
        if (StringUtils.isNotEmpty(metric)) {
            body.put("metric", metric);
        }
        body.put("metric_pk", isMetricPrimaryKey);
        body.put("number", number);

        indexRequest.source(body);
        indexRequest.index(indexName);
        indexRequest.type("_doc");
        try {
            highLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            return true;
        } catch (Throwable e) {
            if (Throwables.getStackTraceAsString(e).contains("version conflict, document already exists")) {
                return false;
            } else {
                throw e;
            }
        }
    }

    public static MetricMappingInfo getMetricMappingInfo(RestHighLevelClient highLevelClient, String indexName,
        String pk) throws IOException {
        GetRequest getRequest = new GetRequest(indexName, "_doc", pk);
        getRequest.preference("_primary");
        GetResponse indexResponse = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        if (indexResponse.getSource() == null) {
            return null;
        }
        MetricMappingInfo metricMappingInfo = gson.fromJson(indexResponse.getSourceAsString(), MetricMappingInfo.class);
        return metricMappingInfo;
    }

    public static void deleteInfo(RestHighLevelClient highLevelClient, String indexName,
        String pk) throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest(indexName, "_doc", pk);
        highLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }
}
