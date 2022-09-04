package com.alibaba.tesla.tkgone.server.services.category;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.config.ApplicationProperties;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.tsearch.TeslasearchService;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjinghua
 */
@Service
@Data
@Log4j
@SuppressWarnings("Duplicates")
public class AddEx {

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    BaseConfigService baseConfigService;

    @Autowired
    GetExData getExData;

    @Autowired
    TeslasearchService teslasearchService;

    @Autowired
    ApplicationProperties applicationProperties;

    String exTitleField = "_title";

    public JSONObject buildSimpleRetNode(String category, JSONObject node) {
        JSONObject retJson = new JSONObject();
        JSONObject fieldJson = delNodeMetaField(getNodeAlias(category, ruleFields(category, node)));
        retJson.put("content", fieldJson);
        retJson.put("content_str", stringValue(fieldJson));
        return retJson;
    }

    public JSONObject buildRetNode(String category, JSONObject node) {
        node.putAll(getProperties(category, node));
        String type = node.getString(Constant.INNER_TYPE);
        String id = node.getString(Constant.INNER_ID);
        JSONObject retJson = new JSONObject();
        retJson.put("content", new JSONObject());
        retJson.getJSONObject("content").put("exdata", categoryConfigService.getCategoryTypeExDataMeta(category, type));
        retJson.getJSONObject("content").put("kv",
            stringValue(delNodeMetaField(getNodeAlias(category, ruleFields(category, node)))));
        retJson.getJSONObject("content").put(exTitleField, trimXYTag(getTitle(category, node)));
        retJson.put("icon", categoryConfigService.getCategoryTypeIdIcon("", type, id));
        retJson.put("exlink", getOldExLink(category, node));
        return retJson;
    }

    public JSONObject ruleFields(String category, JSONObject node) {
        JSONObject retNode = JSONObject.parseObject(JSONObject.toJSONString(node));
        String type = node.getString(Constant.INNER_TYPE);
        JSONArray includeFields = categoryConfigService.getCategoryTypeIncludeFields(category, type);
        JSONArray excludeFields = categoryConfigService.getCategoryTypeExcludeFields(category, type);
        List<String> removeKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(includeFields)) {
            for (String key : retNode.keySet()) {
                if (!includeFields.contains(key)) {
                    removeKeys.add(key);
                }
            }
        }
        if (!CollectionUtils.isEmpty(excludeFields)) {
            for (String key : retNode.keySet()) {
                if (excludeFields.contains(key)) {
                    removeKeys.add(key);
                }
            }
        }
        for (String key : removeKeys) {
            retNode.remove(key);
        }
        return retNode;
    }

    public String getUrl(String category, JSONObject node) {
        String type = node.getString(Constant.INNER_TYPE);
        String urlKey = categoryConfigService.getCategoryTypeIdUrlKey(category, type, null);
        return Tools.processTemplateString(urlKey, node);
    }

    public JSONArray getExLink(String category, JSONObject node) {
        String type = node.getString(Constant.INNER_TYPE);
        JSONArray exLinkArray = categoryConfigService.getCategoryTypeExLink(category, type);
        JSONArray retArray = new JSONArray();
        exLinkArray.toJavaList(JSONObject.class).forEach(exLinkJson -> {
            if (exLinkJson.containsKey("rule")) {
                if (exLinkJson.getJSONObject("rule").containsKey("properties")) {
                    JSONObject param = exLinkJson.getJSONObject("rule").getJSONObject("properties");
                    if (!Tools.propertiesMatch(param, node)) {
                        return;
                    }
                }
            }
            String dataString = JSONObject.toJSONString(exLinkJson.getJSONArray("data"));
            dataString = Tools.processTemplateString(dataString, node);
            retArray.addAll(JSONObject.parseArray(dataString));
        });
        return retArray;
    }

    public JSONObject getOldExLink(String category, JSONObject node) {
        log.info(category);
        log.info(node);
        if (applicationProperties.getProductopsStdIndex().equals(node.getString("__type"))) {
            return getProductopsExLink(node);
        }
        if (applicationProperties.getProductopsPathMappingIndex().equals(node.getString("__type"))) {
            return getProductopsPathExLink(node);
        }
        JSONArray exLink = getExLink(category, node);
        JSONObject retJson = new JSONObject();
        for (JSONObject tmpLink : exLink.toJavaList(JSONObject.class)) {
            retJson.putAll(tmpLink);
        }
        return retJson;
    }

    private JSONObject getProductopsExLink(JSONObject node) {
        JSONObject resultObj = new JSONObject();
        JSONArray pathArray = teslasearchService.getPathByProduct(node);

        for (JSONObject pathObj : pathArray.toJavaList(JSONObject.class)) {
            resultObj.put(String.format("%s", pathObj.getString("tabLabel")),
                    buildProductPathExLink(node, pathObj));
        }
        return resultObj;
    }

    private JSONObject getProductopsPathExLink(JSONObject node) {
        JSONObject resultObj = new JSONObject();
        String tabLabel = node.getString("tabLabel");
        JSONArray productArray = teslasearchService.getProductByPath(node);
        for (JSONObject productObj : productArray.toJavaList(JSONObject.class)) {
            String productLabel = trimXYTag(productObj.getString("label")).toString();
            String productName = trimXYTag(productObj.getString("name")).toString();
            productLabel = StringUtils.isNotEmpty(productLabel) ? productLabel : productName;
            resultObj.put(String.format("%s-%s", productLabel, tabLabel),
                    buildProductPathExLink(productObj,node));
        }
        return resultObj;
    }

    private String buildProductPathExLink(JSONObject productObj, JSONObject pathObj) {
        String path = trimXYTag(pathObj.getString("path")).toString();
        String tabId = trimXYTag(pathObj.getString("tabId")).toString();
        String nodeId = trimXYTag(productObj.getString("nodeId")).toString();

        return String.format("%s/%s/%s?nodeId=%s", applicationProperties.getProductopsPathEndpoint(), path,
                tabId.replaceAll("\\.", "/"), nodeId);
    }

    public String getTitle(String category, JSONObject node) {
        String type = node.getString(Constant.INNER_TYPE);
        String titleKey = categoryConfigService.getCategoryTypeIdTitleKey(category, type, null);
        String title = Tools.processTemplateString(titleKey, node);
        if (StringUtils.isEmpty(title)) {
            title = node.getString(Constant.INNER_ID);
        }
        return title;
    }

    public JSONObject getNodeAlias(String category, JSONObject node) {
        String type = node.getString(Constant.INNER_TYPE);
        JSONObject retJson = new JSONObject();
        for (String key : node.keySet()) {
            retJson.put(categoryConfigService.getCategoryTypeSpecFieldAlias(category, type, key), node.get(key));

        }
        return retJson;
    }

    public JSONObject delNodeMetaField(JSONObject node) {
        JSONObject retJson = new JSONObject();
        for (String key : node.keySet()) {
            if (key.equals(Constant.INNER_ID) || key.equals(Constant.INNER_TYPE) ||
                    key.equals(Constant.INNER_GMT_CREATE) || key.equals(Constant.INNER_GMT_MODIFIED) ||
                    key.equals(Constant.PARTITION_FIELD) || key.equals(Constant.UPSERT_TIME_FIELD)) {
                continue;
            }
            retJson.put(key, trimXYTag(node.get(key)));
        }
        return retJson;
    }

    private Object trimXYTag(Object val) {
        if (ObjectUtils.isEmpty(val)) {
            return "";
        }

        if (val.getClass().equals(String.class) && val.toString().startsWith("__xy__")) {
            return val.toString().substring("__xy__".length());
        }
        return val;
    }

    public JSONObject stringValue(JSONObject node) {
        JSONObject retNode = new JSONObject();
        for (String key : node.keySet()) {
            Object value = JSONObject.toJSON(node.get(key));
            if (value instanceof JSONObject || value instanceof JSONArray) {
                value = JSONObject.toJSONString(value);
            }
            retNode.put(key, value);
        }
        return retNode;
    }

    public JSONObject getPreciseNode(String category, JSONObject node) {
        String type = node.getString(Constant.INNER_TYPE);
        JSONObject retJson = new JSONObject();
        JSONArray preciseFields = categoryConfigService.getCategoryTypePreciseMatchFields(category, type);
        for (String field : preciseFields.toJavaList(String.class)) {
            if (node.containsKey(field)) {
                retJson.put(field, node.getString(field));
            }
        }
        return retJson;
    }

    public JSONObject getProperties(String category, JSONObject node) {
        String type = node.getString(Constant.INNER_TYPE);
        JSONArray exPropertiesConfig = categoryConfigService.getCategoryTypeExPropertiesConfig(category, type);
        String exPropertiesString = JSONObject.toJSONString(exPropertiesConfig);
        exPropertiesString = Tools.processTemplateString(exPropertiesString, node);
        exPropertiesConfig = JSONObject.parseArray(exPropertiesString);
        JSONObject retJson = new JSONObject();
        for (JSONObject singleExPropertiesConfig : exPropertiesConfig.toJavaList(JSONObject.class)) {
            String singleExPropertiesConfigType = singleExPropertiesConfig.getString("type");
            JSONObject singleExPropertiesConfigInfo = singleExPropertiesConfig.getJSONObject("info");
            switch (singleExPropertiesConfigType) {
                case "table":
                    JSONArray jsonArray;
                    try {
                        jsonArray = getExData.getResourceFromTable(singleExPropertiesConfigInfo);
                    } catch (SQLException e) {
                        log.error(e);
                        continue;
                    }
                    if (jsonArray.size() > 0) {
                        retJson.putAll(jsonArray.getJSONObject(0));
                    }
                    break;
                case "url":
                    Object object;
                    try {
                        object = getExData.getResourceFromUrl(singleExPropertiesConfigInfo);
                    } catch (Exception e) {
                        log.error(e);
                        break;
                    }
                    object = JSON.toJSON(object);
                    log.info(JSONObject.toJSONString(object, true));
                    if (object instanceof JSONArray) {
                        retJson.putAll(((JSONArray)object).getJSONObject(0));
                    } else if (object instanceof JSONObject) {
                        retJson.putAll((JSONObject)object);
                    }
                    break;
                case "metric_url":
                    try {
                        object = getExData.getResourceFromUrl(singleExPropertiesConfigInfo);
                    } catch (Exception e) {
                        log.error(e);
                        continue;
                    }
                    try {
                        String flag = singleExPropertiesConfig.getString("flag");
                        flag = StringUtils.isEmpty(flag) ? "avg" : flag;
                        JSONObject jsonObject = (JSONObject)object;
                        log.info(jsonObject);
                        JSONObject yJson = jsonObject.getJSONArray("data").getJSONObject(0).getJSONObject("y");
                        log.info(yJson);
                        for (String key : yJson.keySet()) {
                            retJson.put(key, yJson.getJSONObject(key).get(flag));
                        }
                    } catch (Exception e) {
                        log.error(e);
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
        return retJson;
    }

}
