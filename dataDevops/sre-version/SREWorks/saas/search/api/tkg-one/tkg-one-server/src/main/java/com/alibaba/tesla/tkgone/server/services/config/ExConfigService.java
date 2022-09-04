package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.common.annotation.timediff.TimeDiff;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSpecSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yangjinghua
 */

@Service
public class ExConfigService {

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    ElasticSearchSpecSearchService elasticSearchSpecSearchService;

    public Object parseContent(Object object, JSONObject exJson) throws Exception {

        object = JSONObject.toJSON(object);
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)object;

            if ("innerFun".equals(jsonObject.getString("__innerType__"))) {
                String name = jsonObject.getString("name");
                JSONObject nameJson = exJson.getJSONObject(name);
                nameJson = nameJson == null ? exJson : nameJson;
                jsonObject = Tools.deepMerge(nameJson, jsonObject);
                return innerFunResult(jsonObject);
            } else {
                JSONObject retJson = new JSONObject();
                for (String key : jsonObject.keySet()) {
                    retJson.put(key, parseContent(jsonObject.get(key), exJson));
                }
                return retJson;
            }

        }
        if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray)object;
            for (int index = 0; index < jsonArray.size(); index++) {
                jsonArray.set(index, parseContent(jsonArray.get(index), exJson));
            }
            return jsonArray;
        }

        return object;
    }

    public Object innerFunResult(JSONObject jsonObject) throws Exception {
        if (!"innerFun".equals(jsonObject.getOrDefault("__innerType__", ""))) {
            return jsonObject;
        }

        String className = jsonObject.getString("className");
        String methodName = jsonObject.getString("methodName");
        Object args = jsonObject.get("args");

        return runControllerMethod(className, methodName, args);

    }

    @TimeDiff(name = "runControllerMethod")
    public Object runControllerMethod(String className, String methodName, Object args) throws Exception {

        if ("ElasticSearchSearchService".equals(className)) {
            if ("aggByKv".equals(methodName)) {
                return elasticSearchSearchService.aggByKv((JSONObject)args);
            }
        }
        if ("ElasticSearchSpecSearchService".equals(className)) {
            if ("universeEventAggTimeSeriesSample".equals(methodName)) {
                return elasticSearchSpecSearchService.universeEventAggTimeSeriesSample((JSONObject)args);
            }
        }

        return null;

    }

}
