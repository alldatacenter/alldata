package com.alibaba.tesla.tkgone.server.services.tsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.config.ApplicationProperties;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.category.GetExData;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchHttpApiBasic;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchUpsertService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetAggGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class TeslasearchService {

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    GetExData getExData;

    @Autowired
    GetQueryGrammar getQueryGrammar;

    @Autowired
    GetAggGrammar getAggGrammar;

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    ElasticSearchHttpApiBasic elasticSearchHttpApiBasic;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    private IndexMapper indexMapper;

    public JSONArray metaTypeNum(String category, String query, String empId) throws Exception {
        JSONObject params = new JSONObject();
        // 新的规范约定category以_拼接，第二位为产品名称
        String product = category.split("_").length == 3 ? category.split("_")[1] : null;
        if (StringUtils.isNotEmpty(product)) {
            params.put("product", String.format("%s", product));
            params.put("buCode", getUserBuCode(empId));
        }
        List<String> types = categoryConfigService.getCategoryIndexes(category).toJavaList(String.class);
        types.retainAll(indexMapper.getAliasIndexes());

        return metaTypeNum(category, types, query, params);
    }

    public String getUserBuCode(String empId) throws Exception {
        JSONObject requestJson = JSONObject.parseObject(String.format("{\n" + "    \"meta\": {\n"
                + "        \"size\": 20,\n" + "        \"from\": 0,\n" + "        \"type\": \"person\"\n" + "    },\n"
                + "    \"query\": {\n" + "        \"AND\": [\n" + "            {\n"
                + "                \"empid\": \"%s\"\n" + "            }\n" + "        ]\n" + "    }\n" + "}", empId));
        JSONArray personArray = elasticSearchSearchService.queryByKv(requestJson);
        if (personArray.isEmpty()) {
            return "JSPT"; // 默认部门
        }
        String buCode = personArray.getJSONObject(0).getString("buCode");

        return StringUtils.isNotEmpty(buCode) ? buCode : "JSPT"; // 默认部门
    }

    private JSONArray metaTypeNum(String category, List<String> types, String query, JSONObject params)
            throws Exception {
        JSONObject sortJson = new JSONObject();
        sortJson.put("field", "count");
        sortJson.put("reverse", true);
        JSONObject queryGrammarJson = getQueryGrammar.getByCategory(category, new JSONArray(new ArrayList<>(types)),
                null, query, params, null, 0, 0, false);
        JSONObject aggGrammarJson = getAggGrammar.getTermsAggGrammar(Constant.INNER_TYPE, 10000L, sortJson);
        queryGrammarJson.putAll(aggGrammarJson);

        return elasticSearchSearchService.getAggsByRet(elasticSearchSearchService.searchByIndices(types, null,
                JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST, true), null);
    }

    public JSONArray getNodeByTypeId(String category, String type, String id) throws Exception {
        JSONObject queryJson = new JSONObject();
        queryJson.put(Constant.INNER_TYPE, type);
        queryJson.put(Constant.INNER_ID, id);
        queryJson.put(Constant.PARTITION_FIELD, categoryConfigService.getCategoryIndexPartition(category, type));
        JSONObject queryGrammarJson = getQueryGrammar.get(queryJson, 0, 100);
        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category, type);
        return elasticSearchSearchService.getNodesByRet(elasticSearchSearchService.searchByBackendStores(
                backendStoreDTOs, null, JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST));
    }

    public JSONArray getPreciseNodesByTypeQuery(String category, String type, String query) throws Exception {
        JSONObject queryGrammarJson = getQueryGrammar.getByCategory(category,
                new JSONArray(Collections.singletonList(type)),
                categoryConfigService.getCategoryTypePreciseMatchFields(category, type), query, null, "keyword", 0, 100,
                false);

        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category, type);
        return elasticSearchSearchService.getNodesByRet(elasticSearchSearchService.searchByBackendStores(
                backendStoreDTOs, null, JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST));
    }

    public JSONArray getNodesByTypeQuery(String category, String type, String query, int from, int size)
            throws Exception {
        JSONObject params = new JSONObject();
        String product = category.split("_").length == 3 ? category.split("_")[1] : null;
        if (StringUtils.isNotEmpty(product)) {
            params.put("product", String.format("%s", product));
        }

        JSONArray types = new JSONArray();
        if (!StringUtils.isEmpty(type)) {
            types = new JSONArray(Collections.singletonList(type));
        }
        JSONObject queryGrammarJson = getQueryGrammar.getByCategory(category, types, null, query, params, null, from,
                size, false);
        log.info(queryGrammarJson.toJSONString());

        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category, type);
        return elasticSearchSearchService.getNodesByRet(elasticSearchSearchService.searchByBackendStores(
                backendStoreDTOs, null, JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST));
    }

    public JSONObject getExData(String category, String type, String id, String exDataName) throws Exception {
        JSONArray nodeArray = getNodeByTypeId(category, type, id);
        if (0 == nodeArray.size()) {
            return new JSONObject();
        }
        JSONObject node = nodeArray.getJSONObject(0);
        return getExData.getData(category, node, exDataName);
    }

    public void setHotSpot(String category, String query, String empid) {
        if (StringUtils.isEmpty(category)) {
            category = Constant.DEFAULT_CATEGORY;
        }
        try {
            String uuid = UUID.randomUUID().toString();
            String index = "sreworks_query_hotspot";
            JSONObject node = new JSONObject();
            node.put(Constant.INNER_TYPE, index);
            node.put(Constant.INNER_ID, uuid);
            node.put(Constant.UPSERT_TIME_FIELD, System.currentTimeMillis() / 1000);
            node.put(Constant.PARTITION_FIELD, Constant.DEFAULT_PARTITION);
            node.put("category", category);
            node.put("query", query);
            node.put("empid", empid);
            elasticSearchUpsertService.upserts(index, Collections.singletonList(node));
        } catch (Exception e) {
            log.error("saveHotSpot 报错: ", e);
        }
    }

    public String getRealField(String index, String field) {
        JSONObject elasticsearchIndexAnalysis = JSONObject
                .parseObject(elasticSearchConfigService.getElasticsearchIndexAnalysis(index));
        List<String> analyzers = new ArrayList<>(elasticsearchIndexAnalysis.getJSONObject("analyzer").keySet());
        analyzers.addAll(elasticSearchConfigService.getElasticsearchIndexExAnalyzers(index));
        for (String analyzer : analyzers) {
            if (field.endsWith(analyzer)) {
                return field.substring(0, field.length() - analyzer.length() - 1);
            }
        }
        return field;
    }

    /**
     * 根据查出来的product查出product下的mapping
     * 
     * @param productObj product实例
     * @return mapping列表
     */
    public JSONArray getPathByProduct(JSONObject productObj) {
        JSONObject queryJson = new JSONObject();
        queryJson.put("nodeTypePath", productObj.getString("nodeTypePath").substring("__xy__".length()));
        JSONObject queryGrammarJson = getQueryGrammar.get(queryJson);
        JSONArray pathArray = new JSONArray();
        try {
            log.debug(String.format("queryGrammarJson: %s", queryGrammarJson));
            pathArray = elasticSearchSearchService.getNodesByRet(
                    elasticSearchSearchService.search(applicationProperties.getProductopsPathMappingIndex(), null,
                            JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST, true));
        } catch (Exception e) {
            log.error("getPathByProduct has an exception!", e);
        }
        return pathArray;
    }

    /**
     * 根据查出来的path查出path对应product
     * 
     * @param pathObj pathObj
     * @return product列表
     */
    public JSONArray getProductByPath(JSONObject pathObj) {
        JSONObject queryJson = new JSONObject();
        // nodeTypePath在product有前缀
        queryJson.put("nodeTypePath", String.format("__xy__%s", pathObj.getString("nodeTypePath")));
        JSONObject queryGrammarJson = getQueryGrammar.get(queryJson);
        JSONArray productArray = new JSONArray();
        try {
            log.debug(String.format("queryGrammarJson: %s", queryGrammarJson));
            productArray = elasticSearchSearchService
                    .getNodesByRet(elasticSearchSearchService.search(applicationProperties.getProductopsStdIndex(),
                            null, JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST, true));
        } catch (Exception e) {
            log.error("getProductByPath has an exception!", e);
        }
        log.info(productArray);
        return productArray;
    }

}
