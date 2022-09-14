package com.alibaba.tesla.tkgone.server.services.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchUpsertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Service
public class TimeSeriesDataService {

    @Autowired
    private ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    private ElasticSearchSearchService elasticSearchSearchService;

    private JSONObject toData(JSONObject jsonObject) {
        return toData(jsonObject.getString("cate"), jsonObject.getString("topic"), jsonObject.getLongValue("time"),
            jsonObject.getJSONObject("kv"));
    }

    private JSONObject toData(String cate, String topic, long time, JSONObject kv) {
        if (time == 0) {
            time = System.currentTimeMillis() / 1000;
        }
        // String index = ".metric";
        // String id = UUID.randomUUID().toString();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constant.INNER_TYPE, ".metric");
        jsonObject.put(Constant.INNER_ID, UUID.randomUUID().toString());
        jsonObject.put(Constant.PARTITION_FIELD, Constant.DEFAULT_PARTITION);
        jsonObject.put(Constant.UPSERT_TIME_FIELD, System.currentTimeMillis() / 1000);
        jsonObject.put("cate", cate);
        jsonObject.put("topic", topic);
        jsonObject.put("time", time);
        jsonObject.put("kv", kv);
        return jsonObject;
    }

    public void saveData(JSONObject jsonObject) {
        List<JSONObject> dataList = new ArrayList<>();
        dataList.add(toData(jsonObject));
        elasticSearchUpsertService.upserts(getClass().getSimpleName(), dataList);
    }

    public void saveData(String cate, String topic, long time, JSONObject kv) {
        List<JSONObject> dataList = new ArrayList<>();
        dataList.add(toData(cate, topic, time, kv));
        elasticSearchUpsertService.upserts(getClass().getSimpleName(), dataList);
    }

    public void saveDatas(JSONArray inArray) {
        List<JSONObject> dataList = new CopyOnWriteArrayList<>();
        inArray.toJavaList(Object.class).parallelStream()
            .forEach(object -> dataList.add(toData((JSONObject)(JSONObject.toJSON(object)))));
        elasticSearchUpsertService.upserts(getClass().getSimpleName(), dataList);
    }

    public List<JSONObject> getMetric(JSONObject jsonObject) throws Exception {
        String cate = jsonObject.getString("cate");
        String topic = jsonObject.getString("topic");
        List<String> ks = jsonObject.getJSONArray("ks").toJavaList(String.class);
        long stime = jsonObject.getLongValue("stime");
        long etime = jsonObject.getLongValue("etime");
        String flag = jsonObject.getString("flag");
        return getMetric(cate, topic, ks, stime, etime, flag);
    }

    private List<JSONObject> getMetric(String cate, String topic, List<String> ks, long stime, long etime, String flag)
        throws Exception {
        int maxDotNum = 300;
        List<Integer> timeIntervalList = Arrays.asList(60, 300, 3600);
        int timeInterval = timeIntervalList.get(0);
        for (Integer integer : timeIntervalList) {
            if ((etime - stime) / integer > maxDotNum) {
                timeInterval = integer;
            } else {
                break;
            }
        }
        List<Map<String, Long>> ranges = new ArrayList<>();
        for (Long start = (stime / timeInterval - 1) * timeInterval; start < (etime / timeInterval + 1) * timeInterval;
             start += timeInterval) {
            Map<String, Long> range = new HashMap<>();
            range.put("from", start);
            range.put("to", start + timeInterval);
            ranges.add(range);
        }

        String exists = "{\"exists\": {\"field\": \"kv.%s\"}}";
        String priceStats = "{\"%s\": {\"stats\": {\"field\": \"kv.%s\"}}}";
        JSONObject queryJson = JSONObject.parseObject(String.format("{"
            + "    \"size\": 0,"
            + "    \"query\": {"
            + "        \"bool\": {"
            + "            \"must\": ["
            + "                {"
            + "                    \"term\": {\"cate\": \"%s\"}"
            + "                },"
            + "                {"
            + "                    \"term\": {\"topic\": \"%s\"}"
            + "                }"
            + "            ]"
            + "        }"
            + "    },"
            + "    \"aggs\" : {"
            + "        \"price_ranges\" : {"
            + "            \"range\" : {"
            + "                \"field\" : \"time\","
            + "                \"ranges\" : %s"
            + "            },"
            + "            \"aggs\" : {"
            + "            }"
            + "        }"
            + "    }"
            + "}", cate, topic, JSONObject.toJSONString(ranges)));
        for (String k : ks) {
            queryJson.getJSONObject("query").getJSONObject("bool").getJSONArray("must")
                .add(JSONObject.parseObject(String.format(exists, k)));
            queryJson.getJSONObject("aggs").getJSONObject("price_ranges").getJSONObject("aggs")
                .putAll(JSONObject.parseObject(String.format(priceStats, k, k)));
        }
        JSONObject retJson = elasticSearchSearchService.search(".metric", null,
            JSONObject.toJSONString(queryJson), RequestMethod.POST, true);
        JSONArray jsonArray = retJson.getJSONObject("aggregations").getJSONObject("price_ranges").getJSONArray(
            "buckets");
        switch (flag) {
            case "min":
                // Double stable = 0d;
                return jsonArray.toJavaList(JSONObject.class).stream().map(jsonObject -> {
                    JSONObject tmpJson = new JSONObject();
                    // for (String k : ks) {
                    //     Double value = jsonObject.getJSONObject(k).getDouble("min");
                    // }
                    tmpJson.put("time", jsonObject.getLongValue("from"));
                    return tmpJson;
                }).collect(Collectors.toList());
            case "max":
                return jsonArray.toJavaList(JSONObject.class).stream().map(jsonObject -> {
                    JSONObject tmpJson = new JSONObject();
                    for (String k : ks) {
                        Double value = jsonObject.getJSONObject(k).getDouble("max");
                        tmpJson.put(k, value);
                    }
                    tmpJson.put("time", jsonObject.getLongValue("from"));
                    return tmpJson;
                }).collect(Collectors.toList());
            case "avg":
                return jsonArray.toJavaList(JSONObject.class).stream().map(jsonObject -> {
                    JSONObject tmpJson = new JSONObject();
                    for (String k : ks) {
                        Double value = jsonObject.getJSONObject(k).getDouble("avg");
                        tmpJson.put(k, value);
                    }
                    tmpJson.put("time", jsonObject.getLongValue("from"));
                    return tmpJson;
                }).collect(Collectors.toList());
            case "sum":
                return jsonArray.toJavaList(JSONObject.class).stream().map(jsonObject -> {
                    JSONObject tmpJson = new JSONObject();
                    for (String k : ks) {
                        Double value = jsonObject.getJSONObject(k).getDouble("sum");
                        tmpJson.put(k, value);
                    }
                    tmpJson.put("time", jsonObject.getLongValue("from"));
                    return tmpJson;
                }).collect(Collectors.toList());
            case "sum_per_sec":
                return jsonArray.toJavaList(JSONObject.class).stream().map(jsonObject -> {
                    JSONObject tmpJson = new JSONObject();
                    for (String k : ks) {
                        Double value = jsonObject.getJSONObject(k).getDouble("sum");
                        if (value != null) {
                            value = value / (jsonObject.getLongValue("to") - jsonObject.getLongValue("from"));
                        }
                        tmpJson.put(k, value);
                    }
                    tmpJson.put("time", jsonObject.getLongValue("from"));
                    return tmpJson;
                }).collect(Collectors.toList());
            default:
                return new ArrayList<>();
        }
    }

}
