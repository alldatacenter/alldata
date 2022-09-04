package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import lombok.extern.log4j.Log4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * @author jialiang.tjl
 */
@Service
@Log4j
public class ElasticSearchIndicesService extends ElasticSearchBasic {

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    ElasticSearchDeleteByQueryService elasticSearchDeleteByQueryService;

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    RedisHelper redisHelper;

    @Autowired
    IndexMapper indexMapper;

    private JSONObject getStringDynamicTemplates(String index) {

        List<String> analyzers = new ArrayList<>();
        analyzers.addAll(elasticSearchConfigService.getElasticsearchIndexExAnalyzers(index));
        analyzers.addAll(JSONObject.parseObject(elasticSearchConfigService.getElasticsearchIndexAnalysis(index))
                .getJSONObject("analyzer").keySet());

        JSONObject jsonObject = JSONObject
                .parseObject("{" + "    \"string\": {" + "        \"match_mapping_type\": \"string\","
                        + "        \"mapping\": {" + "            \"type\": \"keyword\","
                        + "            \"fields\": {},"
                        + "            \"ignore_above\": 8190" + "        }" + "    }" + "}");

        for (String analyzer : analyzers) {
            JSONObject fieldJson = new JSONObject();
            JSONObject.parseObject(
                    String.format("{" + "    \"analyzer\": \"%s\"," + "    \"type\": \"text\"" + "}", analyzer));
            switch (analyzer) {
                case "keyword":
                    fieldJson.put("type", "keyword");
                    fieldJson.put("ignore_above", 8190);
                    break;
                case "lowercase":
                    fieldJson.put("analyzer", analyzer);
                    fieldJson.put("type", "text");
                    fieldJson.put("search_analyzer", "lowercase");
                    break;
                default:
                    fieldJson.put("analyzer", analyzer);
                    fieldJson.put("type", "text");
                    fieldJson.put("search_analyzer", "whitespace");
                    break;
            }

            jsonObject.getJSONObject("string").getJSONObject("mapping").getJSONObject("fields").put(analyzer,
                    fieldJson);
        }
        return jsonObject;

    }

    public synchronized void create(String index) throws Exception {

        if (indexMapper.isAliasContains(index)) {
            return;
        }
        String indexRealName = index + Constant.AFTER_SEPARATOR + 0;
        createRealIndex(index, indexRealName);
        alias(indexRealName, index);
        indexMapper.add(indexRealName, index);

        // 如果有额外的别名配置
        String extraAlias = categoryConfigService.getCategoryTypeExtraTypes(Constant.DEFAULT_CATEGORY, index);
        if (!extraAlias.equals(index)) {
            alias(indexRealName, extraAlias);
            indexMapper.add(indexRealName, extraAlias);
        }
    }

    private synchronized void createRealIndex(String index, String indexRealName) throws Exception {

        CreateIndexRequest request = new CreateIndexRequest(indexRealName);
//        String createIndexString = String.format("{\"settings\":{\"index.mapping.coerce\": %s, \"max_ngram_diff\": 100, \"number_of_shards\": %s,\"number_of_replicas\": %s," + "        \"index.mapping.total_fields.limit\": %s,"
//                + "        \"analysis\": %s," + "        \"index.mapping.ignore_malformed\": true,"
//                + "        \"index.routing.allocation.total_shards_per_node\": %s,"
//                + "        \"index.max_result_window\": %s" + "    }," + "    \"mappings\":{" + "        \"nr\":{"
//                + "            \"_all\": { " + "                \"enabled\": false " + "            },"
//                + "            \"dynamic_templates\": %s," + "            \"properties\": %s,"
//                + "            \"date_detection\": false" + "        }" + "}" + "}",
                String createIndexString = String.format("{\"settings\":{\"index.mapping.coerce\": %s, \"max_ngram_diff\": 100, \"number_of_shards\": %s,\"number_of_replicas\": %s," + "        \"index.mapping.total_fields.limit\": %s,"
                + "        \"analysis\": %s," + "        \"index.mapping.ignore_malformed\": true,"
                + "        \"index.routing.allocation.total_shards_per_node\": %s,"
                + "        \"index.max_result_window\": %s" + "    }," + "    \"mappings\":{"
                + "            \"dynamic_templates\": %s," + "            \"properties\": %s,"
                + "            \"date_detection\": false" + "        }" + "}",
                elasticSearchConfigService.getElasticsearchIndexCoerce(index),
                elasticSearchConfigService.getIndexNumberOfShards(index),
                elasticSearchConfigService.getIndexNumberOfReplicas(index),
                elasticSearchConfigService.getElasticsearchIndexFieldsLimit(index),
                elasticSearchConfigService.getElasticsearchIndexAnalysis(index),
                elasticSearchConfigService.getElasticsearchIndexShardsPerNode(index),
                elasticSearchConfigService.getElasticsearchIndexMaxResultWindow(index),
                elasticSearchConfigService.getElasticsearchDynamicTemplates(index),
                elasticSearchConfigService.getElasticsearchProperties(index));

        JSONObject createIndexBody = JSONObject.parseObject(createIndexString);
        createIndexBody.getJSONObject("mappings").getJSONArray("dynamic_templates")
                .add(getStringDynamicTemplates(index));

        log.info(String.format("createIndexBody: index=%s real=%s body=%s", index, indexRealName,
                JSONObject.toJSONString(createIndexBody, true)));

        request.source(createIndexBody.toJSONString(), XContentType.JSON);

        try {
            getRestHighLevelClient(index).indices().create(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            String errorMsg = Tools.getErrorInfoFromException(e);
            if (!errorMsg.contains("resource_already_exists_exception") && !errorMsg.contains("同名索引已存在")) {
                throw new Exception(e);
            }
        }

    }

    private void alias(String index, String alias) throws Exception {
        String uri = String.format("/%s/_alias/%s", index, alias);
        getOrPost(uri, index, null, null, RequestMethod.PUT, true);
    }

    public AcknowledgedResponse delete(String index, String type) throws Exception {

        DeleteIndexRequest request = new DeleteIndexRequest(index);
        // 删除缓存信息
        redisHelper.delAll(type);
        return getRestHighLevelClient(index).indices().delete(request, RequestOptions.DEFAULT);

    }

    public boolean exists(String index) throws Exception {

        GetIndexRequest request = new GetIndexRequest();
        request.indices(index);
        return getRestHighLevelClient(index).indices().exists(request, RequestOptions.DEFAULT);

    }

    public JSONObject getStats(String index) throws Exception {
        String uri = String.format("%s/_stats", index);
        return getOrPost(uri, index, null, null, RequestMethod.GET, true);

    }

    public long getSize(String index) throws Exception {

        JSONObject jsonObject = getStats(index);
        return jsonObject.getJSONObject("_all").getJSONObject("primaries").getJSONObject("store")
                .getLong("size_in_bytes");
    }

    public Set<String> getIndexes() {
        return indexMapper.getAliasIndexes();
    }

    public JSONObject getMapping(String index) throws Exception {
        String uri = String.format("/%s/_mapping?pretty", index);
        return getOrPost(uri, index, null, null, RequestMethod.GET, true);
    }

    public JSONObject getAllAlias() {
        JSONObject allAlias = new JSONObject();
        List<BackendStoreDTO> backendStoreDTOs = getAllBackendStores();
        for (BackendStoreDTO backendStoreDTO : backendStoreDTOs) {
            JSONObject alias = getAlias(backendStoreDTO);
            if (null != alias) {
                allAlias.put(backendStoreDTO.getHost(), alias);
            }
        }
        return allAlias;
    }

    public JSONObject getAlias(BackendStoreDTO backendStoreDTO) {
        try {
            JSONObject aliasObj = getOrPost("/_alias/sreworks*", backendStoreDTO, null, null, RequestMethod.GET, false);
            JSONObject retObj = new JSONObject();
            if (!backendStoreDTO.isDefaultStore()) {
                for (String key : aliasObj.keySet()) {
                    for (Pattern pattern : backendStoreDTO.getIndexPatternMap().keySet()) {
                        if (pattern.matcher(key).find()) {
                            retObj.put(key, aliasObj.getJSONObject(key));
                            break;
                        }
                    }
                }
            } else {
                retObj = aliasObj;
            }
            return retObj;
        } catch (Exception e) {
            log.error(String.format("get alias from backend(%s) failed", backendStoreDTO), e);
            return null;
        }
    }

    public Integer removeTypeProperty(String type, String property) throws Exception {
        List<String> propertyWords = new ArrayList<>(Arrays.asList(property.split(".")));
        String key = propertyWords.get(propertyWords.size() - 1);
        String path = String.join(".", propertyWords.subList(0, propertyWords.size() - 1));
        return removeTypePathField(type, path, key);
    }

    private Integer removeTypePathField(String type, String path, String key) throws Exception {
        String uri = String.format("/%s/_update_by_query", type);
        String ctxStartString = "ctx._source";
        if (!StringUtils.isEmpty(path)) {
            ctxStartString = ctxStartString + "." + path;
        }
        String keyPath = StringUtils.isEmpty(path) ? key : path + "." + key;
        String postBody = String.format("{" + "    \"script\": {" + "        \"source\": \"%s.remove('%s')\","
                + "        \"lang\": \"painless\"" + "    }," + "    \"query\": {" + "        \"exists\": {"
                + "            \"field\": \"%s\"" + "        }" + "    }" + "}", ctxStartString, key, keyPath);
        Map<String, String> queryParams = new HashMap<>(0);
        queryParams.put("conflicts", "proceed");
        queryParams.put("wait_for_completion", "false");
        JSONObject retJson = getOrPost(uri, type, queryParams, postBody, RequestMethod.POST, true);
        return retJson.getIntValue("total");
    }

    public long getCount(String index) throws Exception {

        JSONObject jsonObject = getStats(index);
        return jsonObject.getJSONObject("_all").getJSONObject("primaries").getJSONObject("docs").getLong("count");

    }

    public JSONObject getHealth(String index) throws Exception {
        return getOrPost("/_cluster/health?level=indices", index, null, null, RequestMethod.GET, true)
                .getJSONObject("indices").getJSONObject(getRealIndexName(index));

    }

    private JSONObject getTasks(String index) throws Exception {
        Map<String, String> queryParams = new HashMap<>(0);
        queryParams.put("pretty", "true");
        queryParams.put("detailed", "true");
        queryParams.put("actions", "*reindex");
        JSONObject retJson = getOrPost("/_tasks", index, queryParams, null, RequestMethod.GET, true);
        return retJson.getJSONObject("nodes");
    }

    public String getRealIndexName(String index) throws Exception {
        String uri = "/_alias/" + index.trim();
        JSONObject jsonObject = getOrPost(uri, index, null, null, RequestMethod.GET, true);
        if (jsonObject.containsKey("error")) {
            throw new Exception(jsonObject.getString("error"));
        }
        return jsonObject.entrySet().iterator().next().getKey();
    }

    private void changeAliasIndex(String index, String oldRealIndex, String newRealIndex) throws Exception {
        String inBody = String.format(
                "{" + "    \"actions\": [" + "        { \"remove\": { \"index\": \"%s\", \"alias\": \"%s\" }},"
                        + "        { \"add\":    { \"index\": \"%s\", \"alias\": \"%s\" }}" + "    ]" + "}",
                oldRealIndex, index, newRealIndex, index);
        getOrPost("_aliases", index, null, inBody, RequestMethod.POST, true);
    }

    public String startReindex(String index) throws Exception {
        String lastUuid = redisHelper.hget(Constant.REDIS_REINDEX_NEWEST_UUID, index);
        String lastProgress = redisHelper.hget(Constant.REDIS_REINDEX_STATUS, lastUuid + "_progress");
        if (StringUtils.isEmpty(lastProgress) || "100".equals(lastProgress)) {
            String uuid = UUID.randomUUID().toString();
            redisHelper.hset(Constant.REDIS_REINDEX_NEWEST_UUID, index, uuid, Integer.MAX_VALUE);
            ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
                    new ElasticSearchIndicesThreadFactory());
            executorService.execute(new Reindex(index, uuid));
            return uuid;
        } else {
            throw new Exception(String.format("上次重建[%s]尚未完成,请等待", lastUuid));
        }
    }

    class ElasticSearchIndicesThreadFactory implements ThreadFactory {
        private final String workerName;
        private final AtomicInteger threadId = new AtomicInteger(1);

        ElasticSearchIndicesThreadFactory() {
            this.workerName = "ElasticSearchIndicesThreadFactory's -worker-";
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            String threadName = this.workerName + this.threadId.getAndIncrement();
            Thread thread = new Thread(r, threadName);
            log.info(String.format("new thread name is: %s", thread.getName()));
            return thread;
        }
    }

    class Reindex implements Runnable {

        String index;
        String realIndexName;
        String newRealIndexName;
        String uuid;

        Reindex(String index, String uuid) {
            this.index = index;
            this.uuid = uuid;
            appendStringToCache("[RUNNING] 开始", 0);
        }

        private void appendStringToCache(String string, long progress) {

            String indexCacheString = redisHelper.hget(Constant.REDIS_REINDEX_STATUS, uuid);
            indexCacheString = indexCacheString + "\n" + Tools.currentDataString() + " " + string;
            redisHelper.hset(Constant.REDIS_REINDEX_STATUS, uuid, indexCacheString, 86400);
            if (progress >= 0) {
                redisHelper.hset(Constant.REDIS_REINDEX_STATUS, uuid + "_progress", String.valueOf(progress), 86400);
            }
        }

        private void rollback() {

            appendStringToCache("回滚中", -1);
            appendStringToCache("数据更新回滚", -1);
            elasticSearchConfigService.setElasticsearchRealUpsertIndex(index, realIndexName, "systemReindex");
            appendStringToCache("删除新建index", -1);
            try {
                if (exists(newRealIndexName)) {
                    delete(newRealIndexName, index);
                }
                appendStringToCache("[SUCCESS] 删除新index", -1);
            } catch (Exception e) {
                appendStringToCache("[ERROR] 删除失败: " + Tools.getErrorInfoFromException(e), -1);
            }
            appendStringToCache("查询回滚", -1);
            try {
                changeAliasIndex(index, realIndexName, newRealIndexName);
                appendStringToCache("[SUCCESS] 优雅查询回滚", 100);
            } catch (Exception e) {
                appendStringToCache("[ERROR] 优雅查询回滚失败: " + Tools.getErrorInfoFromException(e), 100);
                try {
                    alias(realIndexName, index);
                    appendStringToCache("[SUCCESS] 强制查询回滚", 100);
                } catch (Exception e1) {
                    appendStringToCache("[ERROR] 强制查询回滚失败: " + Tools.getErrorInfoFromException(e1), 100);
                }
            }
        }

        private boolean reindexDone(String index) throws Exception {
            JSONObject retJson = getTasks(index);
            for (String key : retJson.keySet()) {
                JSONObject taskJson = retJson.getJSONObject(key);
                String taskString = JSONObject.toJSONString(taskJson);
                if (taskString.contains(realIndexName) && taskString.contains(newRealIndexName)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        @SuppressWarnings("AlibabaMethodTooLong")
        public void run() {

            try {
                appendStringToCache("[RUNNING] 获取真实index", 1);
                realIndexName = getRealIndexName(index);
            } catch (Exception e) {
                appendStringToCache("[ERROR] 获取失败: " + Tools.getErrorInfoFromException(e), -1);
                rollback();
                return;
            }
            newRealIndexName = index + Constant.AFTER_SEPARATOR + Tools.currentDateString("yyyyMMddHHmmss");
            try {
                appendStringToCache("[RUNNING] 创建新index", 2);

                createRealIndex(index, newRealIndexName);
            } catch (Exception e) {
                appendStringToCache("[ERROR] 创建失败: " + Tools.getErrorInfoFromException(e), -1);
                rollback();
                return;
            }
            appendStringToCache("[RUNNING] 实时数据更新迁移至新index", 3);
            elasticSearchConfigService.setElasticsearchRealUpsertIndex(index, newRealIndexName, "systemReindex");
            String reindexBody = String.format(
                    "{" + "  \"source\": {" + "    \"index\": \"%s\"," + "    \"size\": 10000" + "  },"
                            + "  \"dest\": {" + "    \"index\": \"%s\"," + "    \"op_type\": \"create\"" + "  }" + "}",
                    realIndexName, newRealIndexName);

            Map<String, String> queryParams = new HashMap<>(0);
            queryParams.put("slices", "auto");
            appendStringToCache("[RUNNING] 开始数据迁移", 4);
            try {
                getOrPost("/_reindex", index, queryParams, reindexBody, RequestMethod.POST, true);
            } catch (Exception ignored) {
            }
            while (true) {
                boolean done;
                try {
                    done = reindexDone(index);
                } catch (Exception e) {
                    appendStringToCache("[ERROR] 获取数据迁移状态失败: " + Tools.getErrorInfoFromException(e), -1);
                    rollback();
                    break;
                }

                Object oldIndexCount;
                Object newIndexCount;
                long oldIndexCountLong = 0;
                long newIndexCountLong = 1;
                try {
                    oldIndexCountLong = getCount(realIndexName);
                    oldIndexCount = oldIndexCountLong;
                } catch (Exception e) {
                    oldIndexCount = Tools.getErrorInfoFromException(e);
                }
                try {
                    newIndexCountLong = getCount(newRealIndexName);
                    newIndexCount = newIndexCountLong;
                } catch (Exception e) {
                    newIndexCount = Tools.getErrorInfoFromException(e);
                }

                long progress = 5;
                try {
                    progress = Math.min(oldIndexCountLong / newIndexCountLong * 90 / 100 + 5, 95);
                } catch (Exception ignored) {
                }
                appendStringToCache(String.format("[RUNNING] 查询数据迁移状态中, 数据迁移进度: %s/%s", newIndexCount, oldIndexCount),
                        progress);

                if (done) {
                    break;
                }
                Tools.sleep(10);
            }

            try {
                appendStringToCache("[RUNNING] 查询指向新index", 95);
                changeAliasIndex(index, realIndexName, newRealIndexName);
            } catch (Exception e) {
                appendStringToCache("[ERROR] 指向失败", -1);
                rollback();
                return;
            }
            appendStringToCache("[RUNNING] 删除旧index", 98);
            try {
                delete(realIndexName, index);
            } catch (Exception e) {
                appendStringToCache("[ERROR] 删除旧index: " + Tools.getErrorInfoFromException(e), -1);
            }
            appendStringToCache("[DONE] 数据重建完成", 100);
        }
    }

}
