package com.alibaba.sreworks.dataset.processors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.exception.InterfaceConfigException;
import com.alibaba.sreworks.dataset.common.exception.RequestException;
import com.alibaba.sreworks.dataset.common.type.ColumnType;
import com.alibaba.sreworks.dataset.connection.ESClient;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceGroupField;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceQueryField;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceSortField;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfig;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.metrics.*;
import org.elasticsearch.search.aggregations.pipeline.ParsedDerivative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ES处理器
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 15:12
 */

@Slf4j
@Service
public class ESProcessor extends AbstractProcessor {

    @Autowired
    ESClient esClient;

    /**
     * 聚合查询记录数0
     */
    private static final Integer MIN_QUERY_SIZE = 0;

    private static String PAGE_FORMATTER = "\"from\": %d, \"size\": %d";

    private static String SORT_FORMATTER = "\"sort\":[%s]";

    private static String QUERY_FORMATTER = "\"query\":{\"bool\":{\"filter\":[${filters}]}}";

//    private static String TIME_RANGE_FILTER_FORMATTER = "{\"range\":{\"@timestamp\":{\"gte\":{{sTimestamp}},\"lte\":{{eTimestamp}},\"format\":\"epoch_millis\"}}}";

    private static String QUERY_STRING_FORMATTER = "{\"query_string\":{\"analyze_wildcard\":true,\"query\":\"${query}\"}}";

    private static String AGGS_DATE_HISTORGRAM_FOLLOW_AGGS_FORMATTER = "\"aggs\":{\"${bucketName}\":{\"date_histogram\":{\"interval\":\"${granularity}\",\"field\":\"${field}\",\"min_doc_count\":0,\"format\":\"epoch_millis\"},${followAggs} } }";

    private static String AGGS_TERMS_FOLLOW_AGGS_FORMATTER = "\"aggs\":{\"${bucketName}\":{\"terms\":{\"field\":\"${field}\",\"size\":10000},${followAggs}}}";

    private static String AGGS_METRIC_FORMATTER = "\"aggs\":{${metrics}}";

    private static String METRIC_FORMATTER = "\"${metricName}\":{\"${metricAggType}\":{\"field\":\"${field}\"}}";

    private static String PIPELINE_FORMATTER = "\"${metricName}\":{\"${metricAggType}\":{\"buckets_path\":\"${field}\"}}";

    /**
     * 查询模型数据
     * @param interfaceConfig 接口配置
     * @param params params 参数,支持简单的参数替换
     * @throws Exception
     */
    @Override
    public Map<String, Object> getModelData(InterfaceConfig interfaceConfig, Map<String, Object> params) throws Exception {
        if (StringUtils.isEmpty(interfaceConfig.getResponseParams())) {
            throw new InterfaceConfigException(String.format("接口配置错误, 返回参数不允许为空, 接口信息 %s:%s", interfaceConfig.getId(), interfaceConfig.getName()));
        }

        return getModelDataByTemplate(interfaceConfig, params);
    }

    /**
     * 查询
     * @param params
     * @return
     * @throws Exception
     */
    private Map<String, Object> getModelDataByTemplate(InterfaceConfig interfaceConfig, Map<String, Object> params) throws Exception {
        String aggTemplate = "";

        // 数值模版 (metric+pipeline agg)
        List<InterfaceQueryField> queryFieldList = parseInterfaceQueryField(interfaceConfig.getQueryFields());
        List<String> metrics = new ArrayList<>();
        for(InterfaceQueryField queryField : queryFieldList) {
            HashMap<String, String> valuesMap = new HashMap();
            String metricOperator = queryField.getOperator();
            valuesMap.put("metricName", queryField.getField());
            valuesMap.put("metricAggType", metricOperator);
            valuesMap.put("field", queryField.getDim());

            if (ValidConstant.ES_METRIC_AGG_TYPE_LIST.contains(metricOperator)) {
                metrics.add(new StringSubstitutor(valuesMap).replace(METRIC_FORMATTER));
            } else if (ValidConstant.ES_PIPELINE_AGG_TYPE_LIST.contains(metricOperator)) {
                metrics.add(new StringSubstitutor(valuesMap).replace(PIPELINE_FORMATTER));
            } else {
                // TODO 其他的聚合方式
            }
        }
        if (!metrics.isEmpty()) {
            aggTemplate = new StringSubstitutor(new HashMap<String, String>(){
                {put("metrics", String.join(COMMA, metrics));}
            }).replace(AGGS_METRIC_FORMATTER);
        }
        log.info(aggTemplate);

        // 分组模板
        List<InterfaceGroupField> groupFieldList = parseInterfaceGroupField(interfaceConfig.getGroupFields());
        if (!CollectionUtils.isEmpty(groupFieldList)) {
            Collections.reverse(groupFieldList);
            for(InterfaceGroupField groupField : groupFieldList) {
                HashMap<String, String> groupsMap = new HashMap();
                groupsMap.put("bucketName", groupField.getField());
                groupsMap.put("field", groupField.getDim());

                String operator = groupField.getOperator();
                if (operator.equals(ValidConstant.ES_DATE_HISTOGRAM_BUCKET)) {
                    groupsMap.put("granularity", groupField.getGranularity());
                    groupsMap.put("followAggs", aggTemplate);
                    aggTemplate = new StringSubstitutor(groupsMap).replace(AGGS_DATE_HISTORGRAM_FOLLOW_AGGS_FORMATTER);
                } else if (operator.equals(ValidConstant.ES_TERMS_BUCKET)) {
                    groupsMap.put("followAggs", aggTemplate);
                    aggTemplate = new StringSubstitutor(groupsMap).replace(AGGS_TERMS_FOLLOW_AGGS_FORMATTER);
                }
            }
        }
        log.info(aggTemplate);

        // 查询条件模板
        String queryString = new StringSubstitutor(new HashMap<String, String>(){
            {put("query", StringEscapeUtils.escapeJava(interfaceConfig.getQlTemplate().trim()));}
        }).replace(QUERY_STRING_FORMATTER);

//        String queryTemplate = new StringSubstitutor(new HashMap<String, String>(){
//            {put("filters", String.join(COMMA, TIME_RANGE_FILTER_FORMATTER, queryString));}
//        }).replace(QUERY_FORMATTER);

        String queryTemplate = new StringSubstitutor(new HashMap<String, String>(){
            {put("filters", queryString);}
        }).replace(QUERY_FORMATTER);

        // 需要分页
        String pageCondition;
        if (interfaceConfig.getPaging()) {
            int pageNum = Integer.parseInt(String.valueOf(params.getOrDefault(PAGE_NUM_KEY, DEFAULT_PAGE_NUM)));
            Preconditions.checkArgument(pageNum > 0, "page num must gt 0");
            // 聚合查询置0
            int pageSize = StringUtils.isNotEmpty(aggTemplate) ? MIN_QUERY_SIZE : Integer.parseInt(String.valueOf(params.getOrDefault(PAGE_SIZE_KEY, DEFAULT_PAGE_SIZE)));
            pageCondition = String.format(PAGE_FORMATTER, (pageNum -  1) * pageSize, pageSize);
        } else {
            // 聚合查询置0
            int pageSize = StringUtils.isNotEmpty(aggTemplate) ? MIN_QUERY_SIZE : DEFAULT_PAGE_SIZE;
            pageCondition = String.format(PAGE_FORMATTER, DEFAULT_PAGE_NUM -  1, pageSize);
        }

        // 需要排序
        String sortCondition = null;
        if (StringUtils.isNotEmpty(interfaceConfig.getSortFields())) {
            List<InterfaceSortField> sortFieldList = parseInterfaceSortField(interfaceConfig.getSortFields());
            List<String> conditions = sortFieldList.stream().map(ESProcessor::buildOrderCondition).collect(Collectors.toList());
            sortCondition = String.format(SORT_FORMATTER, String.join(COMMA, conditions));
        }

        // 查询body拼接
        List<String> querySections = new ArrayList<>();
        querySections.add(pageCondition);
        querySections.add(queryTemplate);
        if (StringUtils.isNotEmpty(sortCondition)) {
            querySections.add(sortCondition);
        }
        if (StringUtils.isNotEmpty(aggTemplate)) {
            querySections.add(aggTemplate);
        }

        String body = String.join(COMMA, querySections);
        String searchTemplate = "{" + body + "}";
        log.info(searchTemplate);

        SearchResponse response = execTemplateSearch(interfaceConfig.getDataSourceId(), interfaceConfig.getDataSourceTable(), searchTemplate, params);
        if (response == null) {
            return new HashMap<>();
        }
        List<JSONObject> datas = fetchModelDataFromResponse(response, interfaceConfig, StringUtils.isNotEmpty(aggTemplate));
        return buildRetData(datas, params, interfaceConfig.getPaging());
    }

    private SearchResponse execTemplateSearch(String dataSourceId, String index, String searchTemplate, Map<String, Object> params) throws Exception {
        SearchTemplateRequest requestTemplate = new SearchTemplateRequest();
        requestTemplate.setScriptType(ScriptType.INLINE);
        requestTemplate.setScript(searchTemplate);
        requestTemplate.setScriptParams(params);

        SearchRequest request = new SearchRequest(index);
        requestTemplate.setRequest(request);

        SearchTemplateResponse response;
        try {
            response = esClient.getHighLevelClient(dataSourceId).searchTemplate(requestTemplate, RequestOptions.DEFAULT);
            return response.getResponse();
        } catch (ElasticsearchException eex) {
            log.error(eex.toString());
            return null;
        }
        catch (IOException ex) {
            throw new RequestException(String.format("请求模型数据异常, 请检查接口参数, %s", ex.getMessage()));
        }
    }

    private List<JSONObject> fetchModelDataFromResponse(SearchResponse response, InterfaceConfig interfaceConfig, boolean isAgg) {
        List<InterfaceQueryField> queryFieldList = parseInterfaceQueryField(interfaceConfig.getQueryFields());
        List<InterfaceGroupField> groupFieldList = parseInterfaceGroupField(interfaceConfig.getGroupFields());

        List<String> responseFields = parseInterfaceResponseParam(interfaceConfig.getResponseParams());

        List<JSONObject> results;
        if (isAgg) {
            Aggregations aggs = response.getAggregations();
            results = parseAggs(aggs, groupFieldList, queryFieldList, responseFields, new JSONObject());
        } else {
            SearchHits hits = response.getInternalResponse().hits();
            results = parseHits(hits, queryFieldList, responseFields);
        }
        return results;
    }

    private List<JSONObject> parseHits(SearchHits hits, List<InterfaceQueryField> queryFieldList, List<String> responseFields) {
        List<JSONObject> hitResults = new ArrayList<>();
        if (hits.getTotalHits().value > 0) {
            List<SearchHit> hitList = Arrays.asList(hits.getHits());
            hitList.forEach(hit -> {
                JSONObject result = parseHitValue(hit, queryFieldList);

                JSONObject respResult = new JSONObject();
                responseFields.forEach(responseField -> respResult.put(responseField, result.get(responseField)));
                hitResults.add(respResult);
            });
        }
        return hitResults;
    }

    private JSONObject parseHitValue(SearchHit hit, List<InterfaceQueryField> queryFieldList) {
        JSONObject result = new JSONObject();
        queryFieldList.forEach(modelValueField -> {
            Object value = null;
            ColumnType columnType = ColumnType.valueOf(modelValueField.getType().toUpperCase());
            String metricName = modelValueField.getField();
            String dim = modelValueField.getDim();
            switch (dim) {
                case "_id":
                    value = hit.getId();
                    break;
                case "_index":
                    value = hit.getIndex();
                    break;
                case "_score":
                    value = hit.getScore();
                    break;
                case "_source":
                    value = hit.getSourceAsString();
                    break;
                default:
                    if (dim.startsWith("_source")) {
                        dim = dim.substring(dim.indexOf(".") + 1);
                    }
                    JSONObject sourceObject = JSONObject.parseObject(hit.getSourceAsString());
                    String[] dims = dim.split(REGEX_DOT);
                    for(int i = 0; i < dims.length - 1; i ++) {
                        sourceObject = sourceObject.getJSONObject(dims[i]);
                    }
                    value = sourceObject.get(dims[dims.length - 1]);
            }
            result.put(metricName, getValue(value, columnType));
        });

        return result;
    }

    private List<JSONObject> parseAggs(Aggregations aggs, List<InterfaceGroupField> groupFieldList,
                                       List<InterfaceQueryField> queryFieldList, List<String> responseFields, JSONObject lastResult) {
        // metric聚合
        if (CollectionUtils.isEmpty(groupFieldList)) {
            List<JSONObject> aggsResult = new ArrayList<>();
            JSONObject result = parseAggregationsValue(aggs, queryFieldList);
            result.putAll(lastResult);

            // 仅返回接口所需字段
            JSONObject respResult = new JSONObject();
            responseFields.forEach(responseField -> respResult.put(responseField, result.get(responseField)));

            aggsResult.add(respResult);
            return aggsResult;
        } else {
            ArrayList<InterfaceGroupField> restGroupFieldList = new ArrayList<>(groupFieldList);
            InterfaceGroupField groupField = restGroupFieldList.remove(0);

            Aggregation agg = aggs.get(groupField.getField());

            List<JSONObject> aggsResult = new ArrayList<>();

            // date_histogram聚合
            if (groupField.getOperator().equals(ValidConstant.ES_DATE_HISTOGRAM_BUCKET)) {
                ParsedDateHistogram dateHistogramAgg = (ParsedDateHistogram)agg;
                List<ParsedDateHistogram.ParsedBucket> dateHistogramBuckets = (List<ParsedDateHistogram.ParsedBucket>) dateHistogramAgg.getBuckets();
                for (ParsedDateHistogram.ParsedBucket dateHistogramBucket : dateHistogramBuckets) {
                    ZonedDateTime datetime = (ZonedDateTime) dateHistogramBucket.getKey();
                    Long timestamp = Timestamp.valueOf(datetime.toLocalDateTime()).getTime();

                    JSONObject result = new JSONObject();
                    result.put(groupField.getField(), timestamp);
                    result.putAll(lastResult);

                    Aggregations nextAggs = dateHistogramBucket.getAggregations();

                    List<JSONObject> nextResult = parseAggs(nextAggs, restGroupFieldList, queryFieldList, responseFields, result);
                    aggsResult.addAll(nextResult);
                }
            }
            // terms聚合
            else {
                ParsedTerms termsAgg = (ParsedTerms)agg;
                List<ParsedTerms.ParsedBucket> termsBuckets = (List<ParsedTerms.ParsedBucket>)termsAgg.getBuckets();
                for(ParsedTerms.ParsedBucket termsBucket : termsBuckets) {
                    JSONObject result = new JSONObject();
                    ColumnType columnType = ColumnType.valueOf(groupField.getType().toUpperCase());
                    result.put(groupField.getField(), getValue(termsBucket.getKey(), columnType));
                    result.putAll(lastResult);

                    Aggregations nextAggs = termsBucket.getAggregations();

                    List<JSONObject> nextResult = parseAggs(nextAggs, restGroupFieldList, queryFieldList, responseFields, result);
                    aggsResult.addAll(nextResult);
                }
            }

            return aggsResult;
        }
    }

    /**
     * 解析单个Aggregations指标数据
     * @param valueAggs
     * @param queryFieldList
     * @return 返回解析结果
     */
    private JSONObject parseAggregationsValue(Aggregations valueAggs, List<InterfaceQueryField> queryFieldList) {
        JSONObject result = new JSONObject();

        for (InterfaceQueryField queryField : queryFieldList) {
            Object value = null;
            ColumnType columnType = ColumnType.valueOf(queryField.getType().toUpperCase());
            ESMetricAggType aggType = ESMetricAggType.valueOf(queryField.getOperator().toUpperCase());
            String metricName = queryField.getField();
            switch (aggType) {
                case MAX:
                    ParsedMax maxAgg = valueAggs.get(metricName);
                    value = maxAgg == null ? null : maxAgg.getValue();
                    break;
                case MIN:
                    ParsedMin minAgg = valueAggs.get(metricName);
                    value = minAgg == null ? null : minAgg.getValue();
                    break;
                case AVG:
                    ParsedAvg avgAgg = valueAggs.get(metricName);
                    value = avgAgg == null ? null : avgAgg.getValue();
                    break;
                case SUM:
                    ParsedSum sumAgg = valueAggs.get(metricName);
                    value = sumAgg == null ? null : sumAgg.getValue();
                    break;
                case CARDINALITY:
                    ParsedCardinality cardinalityAgg = valueAggs.get(metricName);
                    value = cardinalityAgg == null ? null : cardinalityAgg.getValue();
                    break;
                case DERIVATIVE:
                    ParsedDerivative derivativeAgg = valueAggs.get(metricName);
                    value = derivativeAgg == null ? null : derivativeAgg.value();
                    break;
                default:
                    value = null;
            }
            result.put(metricName, getValue(value, columnType));
        }

        return result;
    }

    /**
     * 构建字段排序条件
     * @param sortField
     * @return
     */
    private static String buildOrderCondition(InterfaceSortField sortField) {
        JSONObject orderCfg = new JSONObject();
        orderCfg.put("order", sortField.getOrder());
        if (StringUtils.isNotEmpty(sortField.getMode())) {
            orderCfg.put("mode", sortField.getMode());
        }
        if (StringUtils.isNotEmpty(sortField.getFormat())) {
            orderCfg.put("format", sortField.getFormat());
        }
        orderCfg.put("unmapped_type", "long");

        JSONObject orderCondition = new JSONObject();
        orderCondition.put(sortField.getDim(), orderCfg);
        return orderCondition.toJSONString();
    }
}
