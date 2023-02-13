package com.alibaba.sreworks.dataset.common;

import com.alibaba.sreworks.dataset.common.exception.ESIndexException;
import com.alibaba.sreworks.dataset.connection.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ES索引操作服务类
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/10 20:52
 */
@Service
@Slf4j
public class ESIndexOperator {

    @Autowired
    ESClient esClient;

    public void createIndexIfNotExist(String index, String alias, String datasourceId) throws Exception {
        boolean indexExist;
        try {
            indexExist = existIndex(index, datasourceId);
        } catch (Exception ex) {
            throw new ESIndexException(String.format("索引%s检查异常, 数据源%s", index, datasourceId));
        }

        if (!indexExist) {
            CreateIndexRequest request = new CreateIndexRequest(index);
            request.settings(Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 1)
                    .put("index.refresh_interval", "5s")  // 分片的刷新频率 5s
                    .put("index.max_result_window", 20000)   // 分页支持的最大查询记录数 from+size < max_result_window
                    .put("index.mapping.total_fields.limit", 10000)  // index支持的最大字段数量, 值越大会导致性能下降和内存消耗
            );
            request.alias(new Alias(alias));

            RestHighLevelClient hlClient = esClient.getHighLevelClient(datasourceId);
            CreateIndexResponse createIndexResponse = hlClient.indices().create(request, RequestOptions.DEFAULT);

            log.info(createIndexResponse.toString());
        } else {
            log.warn(String.format("索引:%s 别名:%s 已经存在, 数据源%s", index, alias, datasourceId));
        }
    }

    public boolean existIndex(String index, String datasourceId) throws Exception {
        GetIndexRequest request = new GetIndexRequest(index);
        RestHighLevelClient hlClient = esClient.getHighLevelClient(datasourceId);
        return hlClient.indices().exists(request, RequestOptions.DEFAULT);
    }
}
