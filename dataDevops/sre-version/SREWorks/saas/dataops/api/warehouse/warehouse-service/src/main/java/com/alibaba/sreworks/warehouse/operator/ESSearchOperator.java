package com.alibaba.sreworks.warehouse.operator;

import com.alibaba.sreworks.warehouse.common.client.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;


/**
 * ES查询操作服务类
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/10 20:52
 */
@Service
@Slf4j
public class ESSearchOperator {

    @Autowired
    ESClient esClient;

    public long countDocByIndices(Set<String> indices) throws Exception {
        CountRequest countRequest = new CountRequest(indices.toArray(new String[0]));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        countRequest.source(searchSourceBuilder);

        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        CountResponse countResponse = hlClient.count(countRequest, RequestOptions.DEFAULT);

        return countResponse.getCount();
    }

    public long countDocByAlias(String alias) throws Exception {
        GetAliasesRequest requestWithAlias = new GetAliasesRequest(alias);

        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        GetAliasesResponse aliasesResponse = hlClient.indices().getAlias(requestWithAlias, RequestOptions.DEFAULT);
        Map<String, Set<AliasMetadata>> aliases = aliasesResponse.getAliases();
        Set<String> indices = aliases.keySet();
        return countDocByIndices(indices);
    }
}
