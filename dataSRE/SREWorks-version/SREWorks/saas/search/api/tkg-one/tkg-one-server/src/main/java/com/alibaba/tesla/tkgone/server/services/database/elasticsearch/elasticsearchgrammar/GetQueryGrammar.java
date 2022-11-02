package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.PartitionMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Service
@Slf4j
public class GetQueryGrammar {

    private static final String MATCH_OPERATOR_AND = "and";
    private static final String MATCH_OPERATOR_OR = "or";
    private List<String> connectRelations = Arrays.asList("AND", "OR", "NOT");
    private List<String> types = Arrays.asList("term", "terms", "match", "match_anyword", "exists",
            "range", "rangeAll", "rangeStart", "rangeEnd", "rangeMiddle");

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    GetSearchHighlightGrammar getSearchHighlightGrammar;

    @Autowired
    PartitionMapper partitionMapper;

    public JSONObject get(JSONObject queryJson, Number from, Number size) {
        JSONObject retJson = get(queryJson);
        retJson.put("from", from);
        retJson.put("size", size);
        return retJson;
    }

    public JSONObject get(JSONObject queryJson, Number from, Number size, String analyzer) {
        JSONObject retJson = get(queryJson, analyzer);
        retJson.put("from", from);
        retJson.put("size", size);
        return retJson;
    }

    public JSONObject get(JSONObject queryJson) {
        JSONObject boolJson = propertiesToElasticsearchBool(queryJson, null, null, null);
        JSONObject retJson = new JSONObject();
        retJson.put("query", boolJson);
        return retJson;
    }

    public JSONObject get(JSONObject queryJson, String analyzer) {
        JSONObject boolJson = propertiesToElasticsearchBool(queryJson, null, null, analyzer);
        JSONObject retJson = new JSONObject();
        retJson.put("query", boolJson);
        return retJson;
    }

    public JSONObject getByCategory(String category, JSONArray indexes, JSONArray matchFields, String query,
                                    JSONObject params, String matchAnalyzer, Number from, Number size,
                                    Boolean isSuggest) {
        JSONObject retJson = getByCategory(category, indexes, matchFields, query, params, matchAnalyzer, isSuggest);
        retJson.put("from", from);
        retJson.put("size", size);
        return retJson;
    }

    private boolean checkNeedPartitionFilter(String category, String index) {

        String categoryIndexPartition = categoryConfigService.getCategoryIndexPartition(category, index);
        List<String> partitions = partitionMapper.get(index);
        if (CollectionUtils.isEmpty(partitions)) {
            if (StringUtils.isEmpty(categoryIndexPartition)) {
                return false;
            }
        }

        if (partitions.size() == 1) {
            return !categoryIndexPartition.equals(partitions.get(0));
        }

        return true;

    }

    private JSONArray getSuggestMatchFields(String category, String index) {
        JSONArray matchFields = categoryConfigService.getCategoryIndexSuggestFields(category, index);
        for (String word : categoryConfigService.getCategoryTypePreciseMatchFields(category, index).toJavaList(String.class)) {
//            boolean notMatch = matchFields.stream().noneMatch(matchField ->
//                    String.valueOf(matchField).startsWith(word) && String.valueOf(matchField).split("\\.")[0].equals(word));
//            if (notMatch) {
//                matchFields.add(word);
//            }
            if(!matchFields.contains(word)) {
                matchFields.add(word);
            }
        }
        matchFields = matchFields.toJavaList(String.class).stream().map(field -> getField(field, "pre"))
            .collect(Collectors.toCollection(JSONArray::new));
        return matchFields;
    }

    private JSONArray getSearchMatchFields(String category, String index) {
        JSONArray matchFields;
        matchFields = JSONObject.parseArray(JSONObject.toJSONString(
            categoryConfigService.getCategoryIndexMatchFields(category, index)));
        log.info(String.format("category: %s; index: %s; matchFields: %s", category, index, matchFields));
        for (String field : categoryConfigService.getCategoryTypePreciseMatchFields(category, index)
            .toJavaList(String.class)) {
            if (!field.equals(Constant.INNER_ID)) {
                matchFields.add(String.format("%s.lowercase^20", field));
                matchFields.add(String.format("%s.keyword^40", field));
            }
        }
        return matchFields;
    }

    private JSONObject getByCategory(String category, JSONArray indexes, JSONArray matchFields, String query,
                                     JSONObject params, String matchAnalyzer, Boolean isSuggest) {

        boolean isMatchFieldsEmpty = CollectionUtils.isEmpty(matchFields);
        JSONObject searchHighlightJson = getSearchHighlightGrammar.init();
        JSONObject retJson = new JSONObject();
        if (CollectionUtils.isEmpty(indexes)) { indexes = categoryConfigService.getCategoryIndexes(category); }
        JSONObject queryJson = new JSONObject();
        JSONObject queryOrJson = new JSONObject();
        queryJson.put("OR", queryOrJson);
        String basicMatchKey;
        if (isSuggest) {
            basicMatchKey = String.join(";", getSuggestMatchFields(category, "").toJavaList(String.class));
        } else {
            basicMatchKey = String.join(";", getSearchMatchFields(category, "").toJavaList(String.class));
        }

        JSONArray otherIndexes = new JSONArray();
        for (String index : indexes.toJavaList(String.class)) {
            if (isMatchFieldsEmpty) {
                if (isSuggest) {
                    matchFields = getSuggestMatchFields(category, index);
                    getSearchHighlightGrammar.add(searchHighlightJson, matchFields);
                    retJson.putAll(searchHighlightJson);
                } else {
                    matchFields = getSearchMatchFields(category, index);
                }
            }

            boolean ifAddTypeTerm = false;
            JSONObject andArrayJson = new JSONObject();
            JSONArray andArray = new JSONArray();
            andArrayJson.put("AND", andArray);
            queryOrJson.put(index, andArrayJson);

            JSONObject tmpJson = categoryConfigService.getCategoryIndexFilterRule(category, index);
            if (!CollectionUtils.isEmpty(tmpJson)) {
                andArray.add(tmpJson);
                ifAddTypeTerm = true;
            }

            if (checkNeedPartitionFilter(category, index)) {
                JSONObject partitionJson = JSONObject.parseObject(String.format("{"
                    + "    \"term\": {"
                    + "        \"%s\": \"%s\""
                    + "    }"
                    + "}", Constant.PARTITION_FIELD, categoryConfigService.getCategoryIndexPartition(category, index)));
                andArray.add(partitionJson);
                ifAddTypeTerm = true;
            }

            String matchKey = String.join(";", matchFields.toJavaList(String.class));
            if (!matchKey.equals(basicMatchKey) || ifAddTypeTerm) {
                JSONObject matchJson = JSONObject.parseObject("{\"match\": {}}");
                matchJson.getJSONObject("match").put(matchKey, query);
                if (!StringUtils.isEmpty(query)) {
                    andArray.add(matchJson);
                    ifAddTypeTerm = true;
                }
            }

            if (ifAddTypeTerm) {
                JSONObject typeJson = JSONObject.parseObject(String.format("{"
                    + "    \"term\": {"
                    + "        \"%s\": \"%s\""
                    + "    }"
                    + "}", Constant.INNER_TYPE, index));
                andArray.add(typeJson);
            } else {
                otherIndexes.add(index);
            }

        }

        if (!CollectionUtils.isEmpty(otherIndexes)) {
            queryOrJson.put("otherIndexes", new JSONObject());
            queryOrJson.getJSONObject("otherIndexes").put("AND", new JSONObject());
            JSONObject otherIndexQueryJson = queryOrJson.getJSONObject("otherIndexes").getJSONObject("AND");
            otherIndexQueryJson.put("match", new JSONObject());
            otherIndexQueryJson.getJSONObject("match").put(basicMatchKey, query);

            otherIndexQueryJson.put("terms", new JSONObject());
            otherIndexQueryJson.getJSONObject("terms").put(Constant.INNER_TYPE, otherIndexes);
        }

        String queryJsonString = JSONObject.toJSONString(queryJson);
        queryJsonString = Tools.processTemplateString(queryJsonString, params);

        retJson.put("query",
            propertiesToElasticsearchBool(JSONObject.parseObject(queryJsonString), null, null, matchAnalyzer)
        );

        if (log.isInfoEnabled()) {
            log.info("query grammar: "+ JSONObject.toJSONString(retJson));
        }

        return retJson;

    }

    private JSONArray objectToArray(Object object) {
        JSONArray retArray = new JSONArray();
        object = JSON.toJSON(object);
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)object;
            for (String key : jsonObject.keySet()) {
                JSONObject tmpJson = new JSONObject();
                tmpJson.put(key, jsonObject.get(key));
                retArray.add(tmpJson);
            }
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray)object;
            for (Object tmpObject : jsonArray) {
                retArray.addAll(objectToArray(tmpObject));
            }
        }
        return retArray;
    }

    private String connectRelationToBoolRelation(String connectRelation) {
        if (StringUtils.isEmpty(connectRelation)) {
            connectRelation = "must";
        }
        switch (connectRelation) {
            case "AND":
                connectRelation = "must";
                break;
            case "OR":
                connectRelation = "should";
                break;
            case "NOT":
                connectRelation = "must_not";
                break;
            default:
                break;
        }
        return connectRelation;
    }

    private JSONObject transferExists(JSONObject propertyJson) {
        JSONObject retJson = new JSONObject();
        retJson.put("exists", propertyJson);
        return retJson;
    }

    private JSONObject transferTerm(JSONObject propertyJson) {
        JSONObject retJson = new JSONObject();
        retJson.put("term", propertyJson);
        return retJson;
    }

    private JSONObject transferTerms(JSONObject propertyJson) {
        JSONObject retJson = new JSONObject();
        retJson.put("terms", propertyJson);
        return retJson;
    }

    private JSONObject transferRange(JSONObject propertyJson, RangeType type) {
        JSONObject retJson = new JSONObject();
        retJson.put("range", new JSONObject());
        for (String key : propertyJson.keySet()) {
            retJson.getJSONObject("range").put(key, new JSONObject());
            Object start = propertyJson.getJSONArray(key).get(0);
            Object end = propertyJson.getJSONArray(key).get(1);
            if (start != null) {
                retJson.getJSONObject("range").getJSONObject(key).put(type.getStartOper(), start);
            }
            if (end != null) {
                retJson.getJSONObject("range").getJSONObject(key).put(type.getEndOper(), end);
            }
        }
        return retJson;
    }

    private JSONObject transferMatch(JSONObject propertyJson, String analyzer, String operator) {
        String matchFieldsSplitString = ";";
        if (StringUtils.isEmpty(analyzer)) {
            analyzer = "*";
        }
        JSONObject retJson = new JSONObject();
        retJson.put("multi_match", new JSONObject());
        for (String key : propertyJson.keySet()) {
            String finalAnalyzer = analyzer;
            List<String> fields = Arrays.stream(key.split(matchFieldsSplitString))
                .map(field -> getField(field, finalAnalyzer)).collect(Collectors.toList());
            Object value = propertyJson.get(key);
            retJson.getJSONObject("multi_match").put("query", value);
            retJson.getJSONObject("multi_match").put("fields", fields);
            retJson.getJSONObject("multi_match").put("lenient", true);
            retJson.getJSONObject("multi_match").put("type", "most_fields");
            retJson.getJSONObject("multi_match").put("operator", operator);
            if (!"*".equalsIgnoreCase(finalAnalyzer)) {
                retJson.getJSONObject("multi_match").put("analyzer", finalAnalyzer);
            }
        }
        return retJson;
    }

    private static Object cleanElasticsearchBool(Object object) {

        object = JSONObject.toJSON(object);
        if (object instanceof JSONArray) {
            JSONArray retArray = new JSONArray();
            JSONArray jsonArray = (JSONArray)object;
            for (Object tmpObject : jsonArray) {
                tmpObject = cleanElasticsearchBool(tmpObject);
                if (!Tools.isJsonEmpty(tmpObject)) {
                    retArray.add(tmpObject);
                }
            }
            if (CollectionUtils.isEmpty(retArray)) {
                return null;
            } else {
                return retArray;
            }
        } else if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)object;
            for (String key : new HashSet<>(jsonObject.keySet())) {
                Object value = cleanElasticsearchBool(jsonObject.get(key));
                if (Tools.isJsonEmpty(value)) {
                    jsonObject.remove(key);
                } else {
                    jsonObject.put(key, value);
                }
            }
            if (CollectionUtils.isEmpty(jsonObject)) {
                return null;
            } else {
                return jsonObject;
            }
        } else {
            return object;
        }
    }

    private JSONObject propertiesToElasticsearchBool(Object properties, String connectRelation, String type,
                                                     String matchAnalyzer) {
        connectRelation = connectRelationToBoolRelation(connectRelation);
        JSONArray propertiesArray = objectToArray(properties);

        JSONObject retJson = JSONObject.parseObject(String.format("{"
            + "    \"bool\": {"
            + "        \"%s\": []"
            + "    }"
            + "}", connectRelation));

        JSONArray retJsonBoolArray = retJson.getJSONObject("bool").getJSONArray(connectRelation);

        if (!types.contains(type)) {
            type = "term";
        }
        for (JSONObject propertyJson : propertiesArray.toJavaList(JSONObject.class)) {
            String key = new ArrayList<>(propertyJson.keySet()).get(0);
            Object value = propertyJson.get(key);
            value = JSON.toJSON(value);

            if (connectRelations.contains(key)) {
                retJsonBoolArray.add(
                    propertiesToElasticsearchBool(value, key, type, matchAnalyzer));
                continue;
            }
            if (types.contains(key)) {
                retJsonBoolArray.add(
                    propertiesToElasticsearchBool(value, connectRelation, key, matchAnalyzer));
                continue;
            }

            if (value instanceof JSONObject) {
                retJsonBoolArray.add(
                    propertiesToElasticsearchBool(value, connectRelation, type, matchAnalyzer));
                continue;
            }

            if (value instanceof JSONArray) {
                if (((JSONArray)value).size() > 0 && JSON.toJSON(((JSONArray)value).get(0)) instanceof JSONObject) {
                    retJsonBoolArray.add(propertiesToElasticsearchBool(value, connectRelation, type, matchAnalyzer));
                    continue;
                } else {
                    if ("term".equals(type)) {
                        type = "terms";
                    }
                }
            } else {
                if (Arrays.asList("terms", "range").contains(type)) {
                    type = "term";
                }
            }

            switch (type) {
                case "term":
                    retJsonBoolArray.add(transferTerm(propertyJson));
                    break;
                case "terms":
                    retJsonBoolArray.add(transferTerms(propertyJson));
                    break;
                case "match":
                    retJsonBoolArray.add(transferMatch(propertyJson, matchAnalyzer, MATCH_OPERATOR_AND));
                    break;
                case "match_anyword":
                    retJsonBoolArray.add(transferMatch(propertyJson, matchAnalyzer, MATCH_OPERATOR_OR));
                    break;
                case "exists":
                    retJsonBoolArray.add(transferExists(propertyJson));
                    break;
                default:
                    boolean exists = false;
                    for (RangeType item : RangeType.values()) {
                        if (item.getKey().equalsIgnoreCase(type)) {
                            retJsonBoolArray.add(transferRange(propertyJson, item));
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        throw new IllegalArgumentException("Invalid type: " + type);
                    }
                    break;
            }
        }
        return (JSONObject)cleanElasticsearchBool(retJson);
    }

    private String getField(String realField, String analyzer) {
        if (!(realField.contains(".") || realField.endsWith("*"))) {
            return realField + "." + analyzer;
        }
        return realField;
    }

}
