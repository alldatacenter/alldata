package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yangjinghua
 */
@Service
public class ElasticSearchConfigService extends BaseConfigService {

    @Autowired
    private IndexMapper indexMapper;

    public int getIndexNumberOfShards(String index) {
        String name = "numberOfShards";
        return getTypeNameContentWithDefault(index, name, Constant.DEFAULT_INDEX_SHARDS);
    }

    public int getIndexNumberOfReplicas(String index) {
        String name = "numberOfReplicas";
        return getTypeNameContentWithDefault(index, name, Constant.DEFAULT_INDEX_REPLICAS);
    }

    public int getElasticsearchReadTimeout() {
        String name = "elasticsearchReadTimeout";
        return getNameContentWithDefault(name, Constant.ELASTICSEARCH_READ_TIMEOUT);
    }

    public int getElasticsearchWriteTimeout() {
        String name = "elasticsearchWriteTimeout";
        return getNameContentWithDefault(name, Constant.ELASTICSEARCH_WRITE_TIMEOUT);
    }

    public int getElasticsearchConnectTimeout() {
        String name = "elasticsearchConnectTimeout";
        return getNameContentWithDefault(name, Constant.ELASTICSEARCH_CONNECT_TIMEOUT);
    }

    public int getElasticsearchIndexFieldsLimit(String index) {
        String name = "elasticsearchIndexFieldsLimit";
        return getTypeNameContentWithDefault(index, name,
            getNameContentWithDefault(name, Constant.DEFAULT_INDEX_FIELDS_LIMIT));
    }

    public int getElasticsearchIndexMaxResultWindow(String index) {
        String name = "elasticsearchIndexMaxResultWindow";
        return getTypeNameContentWithDefault(index, name,
            getNameContentWithDefault(name, Constant.DEFAULT_INDEX_MAX_RESULT_WINDOW));
    }

    public String getElasticsearchIndexAnalysis(String index) {
        String name = "elasticsearchIndexAnalysis";
        return getTypeNameContentWithDefault(index, name, Constant.ELASTICSEARCH_INDEX_ANALYSIS);
    }

    public String getElasticsearchDynamicTemplates(String index) {
        String name = "elasticsearchDynamicTemplates";
        return getTypeNameContentWithDefault(index, name,
            getNameContentWithDefault(name, Constant.ELASTICSEARCH_INDEX_DYNAMIC_TEMPLATES));
    }

    public String getElasticsearchProperties(String index) {
        String name = "elasticsearchProperties";
        String content = getContentWithOutCache(ConfigDto.builder().nrType(index).name(name).build());
        content = StringUtils.isEmpty(content) ?
            getContentWithOutCache(ConfigDto.builder().name(name).build()) : content;
        return content;
    }

    public String getElasticsearchIndexCoerce(String index) {
        String name = "elasticsearchxIndexCoerce";
        String content = getContentWithOutCache(ConfigDto.builder().nrType(index).name(name).build());
        content = StringUtils.isEmpty(content) ?
            getContentWithOutCache(ConfigDto.builder().name(name).build()) : content;
        if (StringUtils.isEmpty(content)) {
            content = "false";
        }
        return content;
    }

    public int setElasticsearchProperties(String index, JSONObject elasticsearchProperties) {
        String name = "elasticsearchProperties";
        return setTypeNameContent(index, name, elasticsearchProperties, "sys");
    }

    public int addElasticsearchProperties(String index, String key, String keyType) {
        // String name = "elasticsearchProperties";
        JSONObject properties = JSONObject.parseObject(getElasticsearchProperties(index));
        properties.put(key, new JSONObject());
        properties.getJSONObject(key).put("type", keyType);
        return setElasticsearchProperties(index, properties);
    }

    public int delElasticsearchProperties(String index, String key) {
        // String name = "elasticsearchProperties";
        JSONObject properties = JSONObject.parseObject(getElasticsearchProperties(index));
        properties.remove(key);
        return setElasticsearchProperties(index, properties);
    }

    public List<String> getElasticsearchIndexExAnalyzers(String index) {
        String name = "elasticsearchIndexExAnalyzers";
        return getTypeNameContentWithDefault(index, name,
            getNameContentWithDefault(name, Constant.ELASTICSEARCH_INDEX_EX_ANALYZERS));
    }

    public String getAdjustJsonObjectToEs(String index) {
        String name = "adjustJsonObjectToEs";
        return getTypeNameContentWithDefault(index, name, Constant.ADJUST_JSONOBJECT_TO_ES);
    }

    public String getElasticsearchRealUpsertIndex(String index) {
        String name = "elasticsearchRealUpsertIndex";
        String realUpsertIndex = getTypeNameContentWithDefault(index, name, index);
        if (indexMapper.isRealContains(realUpsertIndex) ||
            indexMapper.isAliasContains(realUpsertIndex)) {
            return realUpsertIndex;
        } else {
            return index;
        }
    }

    public void setElasticsearchRealUpsertIndex(String index, String realUpsertIndex, String modifier) {
        String name = "elasticsearchRealUpsertIndex";
        setTypeNameContent(index, name, realUpsertIndex, modifier);
    }

    public String getElasticsearchIndexShardsPerNode(String index) {
        String name = "elasticsearchIndexShardsPerNode";
        return getTypeNameContentWithDefault(index, name, getNameContentWithDefault(name, "2"));
    }

    public String getHighPriorityIndices() {
        String name = "highPriorityIndices";
        return getNameContentWithDefault(name, "[]");
    }

    /**
     * 获获取后端存储的地址，从缓存读取
     * @return
     */
    public JSONArray getBackendStores() {
        String name = "backendStores";
        String backendStores = this.getNameContentWithDefault(name, "[]");
        return JSONObject.parseArray(backendStores);
    }
}
