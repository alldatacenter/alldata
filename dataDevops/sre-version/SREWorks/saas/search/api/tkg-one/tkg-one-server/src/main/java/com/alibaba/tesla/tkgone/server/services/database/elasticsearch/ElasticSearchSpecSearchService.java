package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class ElasticSearchSpecSearchService extends ElasticSearchSearchService {
    private static final long TIME_MULTIPLY_NUM_LONG = 1000L;
    private static final long TIME_VALID_DAYS = 2 * 32 * 24 * 60 * 60 * 1000L;
    private static final String TIME_ES_FORMAT = "yyyy-MM-dd HH:mm:ss.0";

    public JSONObject universeEventAggTimeSeriesSample(JSONObject jsonObject) throws Exception {
        JSONObject retJson = new JSONObject();

        JSONObject metaJson = jsonObject.getJSONObject("meta");
        JSONObject queryJson = jsonObject.getJSONObject("query");

        List<String> groupFields = metaJson.getJSONArray("groupFields").toJavaList(String.class);
        Integer page = (Integer) metaJson.getOrDefault("page", 1);
        Integer size = (Integer) metaJson.getOrDefault("size", 10);
        String rangeField = (String) metaJson.getOrDefault("rangeField", "time");

        if ("time".equals(rangeField)
                && !validateTimeRange(queryJson.getJSONObject("range").getJSONArray(rangeField))) {
            throw new IllegalArgumentException("查询范围大于64天，请缩小查询范围。");
        }

        JSONObject allAggs = getAllAggs(groupFields, rangeField);

        assert allAggs != null;
        allAggs.putAll(getQueryGrammar.get(queryJson, 0, 0));
        String uri = "universe_event/_search";
        JSONObject esRetJson = getOrPost(uri, "universe_event", null, JSONObject.toJSONString(allAggs),
                RequestMethod.POST, true);

        // total
        retJson.put("total", esRetJson.getJSONObject("hits").getIntValue("total"));
        // data
        JSONArray bucketArray = esRetJson.getJSONObject("aggregations").getJSONObject("group_by")
                .getJSONArray("buckets");
        JSONArray pagedBucketArray = this.getByPagination(bucketArray, (page - 1) * size, size);
        JSONArray dataArray = this.parseBuckets(pagedBucketArray, groupFields, rangeField,
                JSONObject.toJSONString(allAggs));

        retJson.put("data", dataArray);
        return retJson;

    }

    /**
     * 时间查询区间校验
     * 
     * @param timeArray 时间区间
     * @return boolean
     */
    private boolean validateTimeRange(JSONArray timeArray) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time1 = format.parse(timeArray.getString(0));
        Date time2 = format.parse(timeArray.getString(1));
        return 0 <= time2.getTime() - time1.getTime() && time2.getTime() - time1.getTime() <= TIME_VALID_DAYS;
    }

    private JSONObject getAllAggs(List<String> groupFields, String rangeField) {
        List<String> groupScripts = new ArrayList<>();
        for (String groupField : groupFields) {
            groupScripts.add(String.format("doc['%s'].value", groupField));
        }
        String inline = StringUtils.join(groupScripts, String.format(" + '%s' + ", Constant.SPECIAL_AFTER_SEPARATOR));
        String realRangeField = "time".equals(rangeField) ? "time_in_timestamp" : rangeField;
        JSONObject aggs = JSONObject.parseObject(String.format(
                "{\n" + "    \"aggs\":{\n" + "        \"group_by\":{\n" + "            \"terms\":{\n"
                        + "                \"show_term_doc_count_error\":true,\n" + "                \"size\":2000,\n"
                        + "                \"order\":{\n" + "                    \"max_%s\":\"desc\"\n"
                        + "                },\n" + "                \"script\":{\n"
                        + "                    \"source\":\"%s\"\n" + "                }\n" + "            },\n"
                        + "            \"aggs\":{\n" + "                \"max_%s\":{\n"
                        + "                    \"max\":{\n" + "                        \"field\":\"%s\"\n"
                        + "                    }\n" + "                },\n" + "                \"min_%s\":{\n"
                        + "                    \"min\":{\n" + "                        \"field\":\"%s\"\n"
                        + "                    }\n" + "                },\n" + "                \"sample\":{\n"
                        + "                    \"top_hits\":{\n" + "                        \"size\":1,\n"
                        + "                        \"sort\": [{\n"
                        + "                            \"time_in_timestamp\": {\"order\": \"desc\"}\n"
                        + "                        }]\n" + "                    }\n" + "                }\n"
                        + "            }\n" + "        }\n" + "    }\n" + "}",
                rangeField, inline, rangeField, realRangeField, rangeField, realRangeField));
        if (log.isDebugEnabled()) {
            log.debug(String.format("aggs is: %s", aggs));
        }
        return aggs;
    }

    private JSONArray parseBuckets(JSONArray bucketArray, List<String> groupFields, String rangeField,
            String queryAggs) {
        JSONArray retArray = new JSONArray();
        for (JSONObject bucket : bucketArray.toJavaList(JSONObject.class)) {
            JSONObject dataObj = this.getResultFromBucket(bucket, groupFields, rangeField, queryAggs);

            // 在这里过滤掉num=0的会导致前端分页功能失效；
            if (null != dataObj && dataObj.getInteger("num") != 0) {
                retArray.add(dataObj);
            }
        }
        return retArray;
    }

    private JSONObject getResultFromBucket(JSONObject bucketObj, List<String> groupFields, String rangeField,
            String queryAggs) {
        JSONObject resultObj = new JSONObject();

        // range
        resultObj.put(rangeField, this.getRangeArray(rangeField, bucketObj));

        // meta
        resultObj.put("meta", new JSONObject());
        String[] fieldValues = bucketObj.getString("key").split(Constant.SPECIAL_AFTER_SEPARATOR);
        if (fieldValues.length != groupFields.size()) {
            log.error(String.format("fieldValues %s length not equal with groupFields %s", Arrays.asList(fieldValues),
                    groupFields));
            return null;
        }
        for (int i = 0; i < groupFields.size(); i++) {
            resultObj.getJSONObject("meta").put(groupFields.get(i), fieldValues[i]);
        }

        // sample
        resultObj.put("sample", bucketObj.getJSONObject("sample").getJSONObject("hits").getJSONArray("hits")
                .getJSONObject(0).getJSONObject("_source"));

        // num
        final String docCountErrorUpperBound = "doc_count_error_upper_bound";
        if (0 != bucketObj.getInteger(docCountErrorUpperBound)) {
            this.setExactCountAndRangeFromBackend(resultObj, queryAggs, rangeField);
        } else {
            resultObj.put("num", bucketObj.getIntValue("doc_count"));
        }
        return resultObj;

    }

    private JSONArray getRangeArray(String rangeField, JSONObject bucketObj) {
        String rangeMin = new SimpleDateFormat(TIME_ES_FORMAT)
                .format(bucketObj.getJSONObject(String.format("min_%s", rangeField)).getBigInteger("value")
                        .multiply(BigInteger.valueOf(ElasticSearchSpecSearchService.TIME_MULTIPLY_NUM_LONG)));
        String rangeMax = new SimpleDateFormat(TIME_ES_FORMAT)
                .format(bucketObj.getJSONObject(String.format("max_%s", rangeField)).getBigInteger("value")
                        .multiply(BigInteger.valueOf(ElasticSearchSpecSearchService.TIME_MULTIPLY_NUM_LONG)));
        JSONArray rangeArray = new JSONArray();
        rangeArray.add(rangeMin);
        rangeArray.add(rangeMax);
        return rangeArray;
    }

    private void setExactCountAndRangeFromBackend(JSONObject resultObj, String queryAggs, String rangeField) {
        JSONObject exactQueryAggs = JSONObject.parseObject(queryAggs);
        JSONObject metaObj = resultObj.getJSONObject("meta");
        exactQueryAggs.getJSONObject("query").getJSONObject("bool").getJSONArray("must").getJSONObject(0)
                .getJSONObject("bool").put("must", getQueryGrammar.get(metaObj).getJSONObject("query"));

        String uri = "universe_event/_search";
        JSONObject esRetJson;
        if (log.isDebugEnabled()) {
            log.debug(String.format("get exact count from backend aggs is %s", exactQueryAggs));
        }
        try {
            esRetJson = getOrPost(uri, "universe_event", null, JSONObject.toJSONString(exactQueryAggs),
                    RequestMethod.POST, true);
        } catch (Exception e) {
            log.error("get exact count from backend failed!", e);
            resultObj.put("num", 0);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("get exact count from backend result is %s", esRetJson));
        }

        // range array
        JSONArray bucketArray = esRetJson.getJSONObject("aggregations").getJSONObject("group_by")
                .getJSONArray("buckets");
        resultObj.put(rangeField, this.getRangeArray(rangeField, bucketArray.getJSONObject(0)));
        resultObj.put("num", esRetJson.getJSONObject("hits").getIntValue("total"));
    }

    private JSONArray getByPagination(JSONArray bucketArray, int from, int size) {
        JSONArray retArray = new JSONArray();
        List<JSONObject> tmpList = bucketArray.toJavaList(JSONObject.class);
        // 过滤掉doc_count为0的结果
        tmpList = tmpList.stream().filter(bucket -> 0 != bucket.getIntValue("doc_count")).collect(Collectors.toList());
        if (from >= tmpList.size()) {
            tmpList = new ArrayList<>();
        } else {
            tmpList = tmpList.subList(from, Math.min(from + size, tmpList.size()));
        }
        retArray.addAll(tmpList);
        return retArray;

    }
}
