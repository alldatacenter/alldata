package com.alibaba.sreworks.warehouse.operator;

import com.alibaba.sreworks.warehouse.common.client.ESClient;
import com.alibaba.sreworks.warehouse.common.exception.ESIndexDeleteException;
import com.alibaba.sreworks.warehouse.common.exception.ESIndexException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

    public void createIndexIfNotExist(String index, String alias, String lifecycleName) throws Exception {
        boolean indexExist;
        try {
            indexExist = existIndex(index);
        } catch (Exception ex) {
            throw new ESIndexException(String.format("索引%s检查异常", index));
        }

        if (!indexExist) {
            createIndex(index, alias, lifecycleName);
        } else {
            log.warn(String.format("索引:%s 别名:%s 已经存在", index, alias));
        }
    }

    private void createIndex(String index, String alias, String lifecycleName) throws Exception {
        CreateIndexRequest request = new CreateIndexRequest(index);

        Settings.Builder builder = Settings.builder().put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
                .put("index.refresh_interval", "5s")  // 分片的刷新频率 5s
                .put("index.max_result_window", 20000)   // 分页支持的最大查询记录数 from+size < max_result_window
                .put("index.mapping.total_fields.limit", 10000)  // index支持的最大字段数量, 值越大会导致性能下降和内存消耗
                .put("index.mapping.ignore_malformed", true)  // index映射忽略格式错误内容
                .put("max_ngram_diff", 100);  // ngram分词器的最大词组长度
        if (StringUtils.isNotEmpty(lifecycleName)) {
            builder.put("index.lifecycle.name", lifecycleName).put("index.lifecycle.rollover_alias", alias);
        }

        request.settings(builder);
        request.alias(new Alias(alias));

        Map<String, Object> mapping = new HashMap<>();
        mapping.put("type", "keyword");
        Map<String, Object> strings = new HashMap<>();
        strings.put("match_mapping_type", "string");
        strings.put("mapping", mapping);
        Map<String, Object> dynamicTemplate = new HashMap<>();
        dynamicTemplate.put("strings", strings);
        List<Map<String, Object>> dynamicTemplates = new ArrayList<>();
        dynamicTemplates.add(dynamicTemplate);
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("dynamic_templates", dynamicTemplates);
        request.mapping(mappings);

        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        CreateIndexResponse createIndexResponse = hlClient.indices().create(request, RequestOptions.DEFAULT);

        log.info(createIndexResponse.toString());
    }

    public void updateIndexLifecyclePolicy(String index, String alias, String lifecycleName) throws Exception {
        // TODO 历史分区是否需要修改策略

        if (!existIndex(index)) {
            createIndex(index, alias, lifecycleName);
        } else {
            UpdateSettingsRequest request = new UpdateSettingsRequest(index);
            Settings.Builder settingsBuilder = Settings.builder().put("index.lifecycle.name", lifecycleName);
            request.settings(settingsBuilder);

            RestHighLevelClient hlClient = esClient.getHighLevelClient();
            AcknowledgedResponse updateSettingsResponse = hlClient.indices().putSettings(request, RequestOptions.DEFAULT);

            log.info(updateSettingsResponse.toString());
        }
    }

    public boolean existIndex(String index) throws Exception {
        GetIndexRequest request = new GetIndexRequest(index);
        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        return hlClient.indices().exists(request, RequestOptions.DEFAULT);
    }

    public void deleteIndex(String index) throws Exception {
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        request.indicesOptions(IndicesOptions.lenientExpand());   // 允许索引不存在
        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        try {
            AcknowledgedResponse deleteIndexResponse = hlClient.indices().delete(request, RequestOptions.DEFAULT);
            log.info(String.format("索引:%s, 删除成功: %s", index, deleteIndexResponse.isAcknowledged()));
        } catch (Exception ex) {
            log.error(String.format("索引:%s, 删除失败: %s", index, ex.getMessage()));
            throw new ESIndexDeleteException(String.format("索引:%s, 删除失败: %s", index, ex.getMessage()));
        }
    }

    public void deleteIndexByAlias(String index, String alias) throws Exception {
        Map<String, Set<AliasMetadata>> aliases = getAllAlias();
        String[] indices = aliases.keySet().parallelStream().filter(existIndex -> {
            Set<AliasMetadata> metadatas = aliases.get(existIndex);
            for (AliasMetadata metadata : metadatas) {
                if (metadata.alias().equals(alias)) {
                    return true;
                }
            }
            return false;
        }).toArray(String[]::new);
        if (indices.length == 0) {
            log.warn(String.format("索引:%s, 别名:%s, 未找到匹配的索引", index, alias));
            return;
        }

        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indices);
        deleteIndexRequest.indicesOptions(IndicesOptions.lenientExpand());   // 允许索引不存在
        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        try {
            AcknowledgedResponse deleteIndexResponse = hlClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            log.info(String.format("索引:%s, 别名:%s, 删除成功: %s", index, alias, deleteIndexResponse.isAcknowledged()));
        } catch (Exception ex) {
            log.error(String.format("索引:%s, 别名:%s, 删除失败:%s", index, alias, ex.getMessage()));
            throw new ESIndexDeleteException(String.format("索引:%s, 别名:%s, 删除失败:%s", index, alias, ex.getMessage()));
        }
    }

    public Set<String> getIndicesByAlias(String alias) throws Exception {
        GetAliasesRequest request = new GetAliasesRequest(alias);
        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        try {
            GetAliasesResponse response = hlClient.indices().getAlias(request, RequestOptions.DEFAULT);
            RestStatus status = response.status();
            if (status == RestStatus.OK) {
                return response.getAliases().keySet();
            } else {
                log.error(String.format("索引别名[%s]查询失败:%s", alias, response.getError()));
                throw new ESIndexDeleteException(String.format("索引别名[%s]查询失败:%s", alias, response.getError()));
            }
        } catch (Exception ex) {
            log.error(String.format("索引别名[%s]查询失败:%s", alias, ex.getMessage()));
            throw new ESIndexException(String.format("索引别名[%s]查询失败:%s", alias, ex.getMessage()));
        }

    }

    private Map<String, Set<AliasMetadata>> getAllAlias() throws Exception {
        GetAliasesRequest request = new GetAliasesRequest();
        RestHighLevelClient hlClient = esClient.getHighLevelClient();

        try {
            GetAliasesResponse response = hlClient.indices().getAlias(request, RequestOptions.DEFAULT);
            RestStatus status = response.status();
            if (status == RestStatus.OK) {
                return response.getAliases();
            } else {
                log.error(String.format("索引别名查询失败:%s", response.getError()));
                throw new ESIndexDeleteException(String.format("索引别名查询失败:%s", response.getError()));
            }
        } catch (Exception ex) {
            log.error(String.format("索引别名查询失败:%s", ex.getMessage()));
            throw new ESIndexException(String.format("索引别名查询失败:%s", ex.getMessage()));
        }
    }
}
