package com.alibaba.tesla.tkgone.server.services.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class ResourceLibraryService {

    @Autowired
    BaseConfigService baseConfigService;

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    public JSONArray getChildren(String product, String path) throws Exception {
        if (StringUtils.isEmpty(path)) { path = ""; }
        JSONObject jsonObject = baseConfigService.getTypeNameContentWithDefault(
            product, "applicationResourceLibrary", new JSONObject());
        if (CollectionUtils.isEmpty(jsonObject)) {
            return new JSONArray();
        }
        String type = jsonObject.getString("type");
        String field = jsonObject.getString("field");
        JSONObject queryJson = jsonObject.getJSONObject("filter");
        if (queryJson == null) { queryJson = new JSONObject(); }
        String[] wordArray = StringUtils.split(path, "/");

        for (int index = 0; index < wordArray.length; index++) {
            jsonObject = jsonObject.getJSONObject("child");
            JSONObject subQueryJson = jsonObject.getJSONObject("filter");
            if (subQueryJson == null) { subQueryJson = new JSONObject(); }
            queryJson.putAll(subQueryJson);
            queryJson.put(field, wordArray[index]);
            field = jsonObject.getString("field");
        }

        JSONObject metaJson = JSONObject.parseObject(String.format("{"
            + "    \"type\": \"%s\","
            + "    \"aggField\": \"%s\","
            + "    \"aggSize\": %s"
            + "}", type, field, 1000));

        JSONObject requestJson = new JSONObject();
        requestJson.put("query", queryJson);
        requestJson.put("meta", metaJson);
        requestJson.put("style", "kv");
        JSONArray searchRetArray = elasticSearchSearchService.aggByKv(requestJson);

        JSONArray retArray = new JSONArray();
        for (JSONArray eachArray : searchRetArray.toJavaList(JSONArray.class)) {
            JSONObject tmpJson = new JSONObject();
            String value = eachArray.getString(0);
            String num = eachArray.getString(1);
            boolean is_leaf = !jsonObject.containsKey("child");
            tmpJson.put("path", path + "/" + value);
            tmpJson.put("id",  path + "/" + value);
            tmpJson.put("label", value);
            tmpJson.put("value", value);
            tmpJson.put("key", field);
            tmpJson.put("parent_id", path);
            tmpJson.put("is_leaf", is_leaf);
            tmpJson.put("level", wordArray.length + 1);
            JSONObject ext_data = JSONObject.parseObject("{"
                + "    \"detail\": [],"
                + "    \"summary\": {}"
                + "}");
            tmpJson.put("ext_data", ext_data);
            if (is_leaf) {
                JSONObject queryRequestJson = new JSONObject();
                queryJson.put(field, value);
                JSONObject queryMetaJson = JSONObject.parseObject(String.format("{"
                    + "    \"type\": \"%s\","
                    + "    \"size\": 1000"
                    + "}", type));
                queryRequestJson.put("query", queryJson);
                queryRequestJson.put("meta", queryMetaJson);
                JSONArray nodes = elasticSearchSearchService.queryByKv(queryRequestJson);
                ext_data.getJSONObject("summary").put("nums", nodes.size());
                ext_data.put("detail", nodes);
            }
            tmpJson.put("num", num);
            retArray.add(tmpJson);
        }
        return retArray;
    }

    public JSONArray getAllSubChildren(String product, String path) throws Exception {
        if (StringUtils.isEmpty(path)) {
            path = "";
        }
        JSONArray jsonArray = getChildren(product, path);
        getAllChildren(product, jsonArray);
        return jsonArray;
    }

    private void getAllChildren(String product, JSONArray levelJsonArray) {
        levelJsonArray.toJavaList(JSONObject.class).parallelStream().forEach(nodeJson -> {
            JSONArray subLevelJsonArray = new JSONArray();
            try {
                subLevelJsonArray = getChildren(product, nodeJson.getString("path"));
                nodeJson.put("children", subLevelJsonArray);
            } catch (Exception e) {
                log.error(e);
            }
            if (!CollectionUtils.isEmpty(subLevelJsonArray) && !subLevelJsonArray.getJSONObject(0).getBooleanValue(
                "is_leaf")) {
                getAllChildren(product, subLevelJsonArray);
            }
        });
    }

    public JSONArray getAllTree(String product) throws Exception {
        return getAllSubChildren(product, "");
    }
}
