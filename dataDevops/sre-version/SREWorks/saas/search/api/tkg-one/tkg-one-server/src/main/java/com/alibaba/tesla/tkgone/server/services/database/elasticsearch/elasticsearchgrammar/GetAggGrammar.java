package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.StringFun;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjinghua
 */
@Service
public class GetAggGrammar {

    public List<String> getNestAggFields(JSONObject metaJson) throws Exception {
        List<String> retList = new ArrayList<>();
        for (Object object : metaJson.getJSONArray("aggFields")) {
            object = JSONObject.toJSON(object);
            if (object instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject)object;
                retList.add(jsonObject.getString("aggField"));
            } else if (object instanceof String) {
                retList.add((String)object);
            } else {
                throw new Exception("nestAggGrammar 的meta结构错误: " + JSONObject.toJSONString(metaJson, true));
            }
        }
        return retList;
    }

    public JSONObject getNestAggGrammars(JSONObject metaJson, JSONObject sortJson) throws Exception {
        metaJson = JSONObject.parseObject(StringFun.run(JSONObject.toJSONString(metaJson)));
        String type = metaJson.getString("type");
        JSONObject allAggs = null;
        JSONObject aggs = null;
        JSONArray aggFieldArray = metaJson.getJSONArray("aggFields");
        for (Object aggFieldObject : aggFieldArray) {
            aggFieldObject = JSONObject.toJSON(aggFieldObject);
            JSONObject aggFieldJson = new JSONObject();
            if (aggFieldObject instanceof String) {
                aggFieldJson.put("aggSize", 100);
                aggFieldJson.put("aggField", aggFieldObject);
                aggFieldJson.put("type", type);
            } else if (aggFieldObject instanceof JSONObject) {
                aggFieldJson = (JSONObject)aggFieldObject;
            } else {
                throw new Exception("nestAggGrammar 的meta结构错误: " + JSONObject.toJSONString(metaJson, true));
            }
            String aggType = aggFieldJson.getString("aggType");
            aggType = StringUtils.isEmpty(aggType) ? "" : aggType;
            String aggField = aggFieldJson.getString("aggField");
            JSONObject eachAggs;
            switch (aggType) {
                case "range":
                    long from = aggFieldJson.getLongValue("from");
                    long to = aggFieldJson.getLongValue("to");
                    long interval = aggFieldJson.getLongValue("interval");
                    from = from == 0 ? System.currentTimeMillis()/1000-2*60*60 : from;
                    to = to == 0 ? System.currentTimeMillis()/1000 : to;
                    interval = interval == 0 ? 1 : interval;
                    eachAggs = getRangeAggGrammar(aggField, from, to, interval);
                    break;
                default:
                    int aggSize = aggFieldJson.getIntValue("aggSize");
                    eachAggs = getTermsAggGrammar(aggField, aggSize, sortJson);
                    break;
            }
            if (aggs == null) {
                allAggs = eachAggs;
                aggs = eachAggs;
            } else {
                aggs.getJSONObject("aggs").getJSONObject("agg").putAll(eachAggs);
                aggs = eachAggs;
            }
        }
        return allAggs;
    }

    public JSONObject getTermsAggGrammar(String field, Number aggSize, JSONObject sortJson) {

        sortJson = sortJson == null ? new JSONObject() : sortJson;
        JSONObject jsonObject = JSONObject.parseObject(String.format("{ "
            + "    \"aggs\" : {"
            + "        \"agg\" : {"
            + "            \"terms\" : {"
            + "                \"field\" : \"%s\","
            + "                \"size\": %s,"
            + "                \"order\" : {"
            + "                    \"_count\" : \"desc\""
            + "                }"
            + "            }"
            + "        }"
            + "    }"
            + "}", field, aggSize));

        String key = "_count";
        String value = "desc";
        if ("key".equals(sortJson.getString("field"))) {
            key = "_key";
        }

        if (!sortJson.getBooleanValue("reverse")) {
            value = "asc";
        }
        jsonObject.getJSONObject("aggs").getJSONObject("agg")
            .getJSONObject("terms").getJSONObject("order").put(key, value);

        return jsonObject;
    }

    public JSONObject getRangeAggGrammar(String field, Number from, Number to, Number interval) {

        JSONObject jsonObject = JSONObject.parseObject(String.format("{ "
            + "    \"aggs\" : {"
            + "        \"agg\" : {"
            + "            \"range\" : {"
            + "                \"field\" : \"%s\","
            + "                \"ranges\": []"
            + "            }"
            + "        }"
            + "    }"
            + "}", field));

        JSONArray jsonArray = jsonObject.getJSONObject("aggs").getJSONObject("agg")
            .getJSONObject("range").getJSONArray("ranges");
        for (long f = (Long)from; f <= (Long)to; f += (Long)interval) {
            jsonArray.add(JSONObject.parseObject(String.format("{"
                + "    \"from\": %s,"
                + "    \"to\": %s"
                + "}", f, f + (Long)interval)));
        }
        return jsonObject;
    }

}
