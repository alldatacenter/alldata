package com.alibaba.tesla.tkgone.server.services.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class NodeManagerService {

    @Autowired
    BaseConfigService baseConfigService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    RedisHelper redisHelper;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    // appName作为category使用
    public JSONArray getTypeList(String appName) {
        String name = "nodeManagerTypeList";
        return baseConfigService.getCategoryNameContentWithDefault(appName, name, new JSONArray());
    }

    public void setTypeList(String appName, JSONArray typeList, String modifier) {
        String name = "nodeManagerTypeList";
        baseConfigService.setCategoryNameContent(appName, name, typeList, modifier);
    }

    public void addType(String appName, String type, String modifier) {
        String name = "nodeManagerTypeList";
        JSONArray typeList = getTypeList(appName);
        typeList.add(type);
        baseConfigService.setCategoryNameContent(appName, name, typeList, modifier);
    }

    public void delType(String appName, String type, String modifier) {
        String name = "nodeManagerTypeList";
        JSONArray typeList = getTypeList(appName);
        typeList.remove(type);
        baseConfigService.setCategoryNameContent(appName, name, typeList, modifier);
    }

    public Set<String> getAllTypeList() {
        Set<String> retSet = new HashSet<>();
        String name = "nodeManagerTypeList";
        List<ConfigDto> configDtoList = baseConfigService.select(ConfigDto.builder().name(name).build());
        for (ConfigDto configDto : configDtoList) {
            List<String> list = JSONObject.parseArray(configDto.getCategory()).toJavaList(String.class);
            retSet.addAll(list);
        }
        return retSet;
    }

    public String getAppNameByType(String type) {
        String name = "nodeManagerTypeList";
        List<ConfigDto> configDtoList = baseConfigService.select(ConfigDto.builder().category(Constant.DEFAULT_CATEGORY)
            .name(name).build());
        for (ConfigDto configDto : configDtoList) {
            List<String> list = JSONObject.parseArray(configDto.getContent()).toJavaList(String.class);
            if (list.contains(type)) {
                return "通用实体";
            }
        }
        configDtoList = baseConfigService.select(ConfigDto.builder().name(name).build());
        for (ConfigDto configDto : configDtoList) {
            List<String> list = JSONObject.parseArray(configDto.getContent()).toJavaList(String.class);
            if (configDto.getCategory().equals(Constant.DEFAULT_CATEGORY)) {
                continue;
            }
            if (list.contains(type)) {
                return configDto.getCategory();
            }
        }
        return null;
    }

    public void checkTypeExists(String type) throws Exception {
        String alreadyExistAppName = getAppNameByType(type);
        if (!StringUtils.isEmpty(alreadyExistAppName)) {
            throw new Exception(String.format("%s中已经存在该实体[%s]", alreadyExistAppName, type));
        }
        if (elasticSearchIndicesService.getIndexes().contains(type)) {
            throw new Exception(String.format("已经存在该实体[%s]", type));
        }
    }

    public void createType(String appName, String type, String alias, String description, String modifier)
        throws Exception {
        checkTypeExists(type);
        if (StringUtils.isEmpty(alias)) { alias = type; }
        if (StringUtils.isEmpty(description)) { description = type; }
        baseConfigService.setCategoryTypeNameContent(appName, type, "categoryTypeAlias", alias, modifier);
        baseConfigService.setCategoryTypeNameContent(appName, type, "categoryTypeDescription", description, modifier);
        addType(appName, type, modifier);
    }

    public void updateType(String appName, String type, String alias, String description, String modifier) {
        updateTypeAlias(appName, type, alias, modifier);
        updateTypeDescription(appName, type, description, modifier);
    }

    public void updateTypeAlias(String appName, String type, String alias, String modifier) {
        baseConfigService.setCategoryTypeNameContent(appName, type, "categoryTypeAlias", alias, modifier);
    }

    public void updateTypeDescription(String appName, String type, String description, String modifier) {
        baseConfigService.setCategoryTypeNameContent(appName, type, "categoryTypeDescription", description, modifier);
    }

    public JSONArray getTypeMetaInfos(String appName) {
        JSONArray retArray = new JSONArray();
        for (String type : getTypeList(appName).toJavaList(String.class)) {
            retArray.add(getTypeMetaInfo(appName, type));
        }
        return retArray;
    }

    public JSONObject getTypeMetaInfo(String appName, String type) {
        JSONObject retJson = new JSONObject();
        List<ConfigDto> configDtos = baseConfigService.select(ConfigDto.builder().category(appName).nrType(type)
            .name("categoryTypeAlias").build());
        String alias = baseConfigService.getCategoryTypeNameContent(appName, type, "categoryTypeAlias");
        String description = baseConfigService.getCategoryTypeNameContent(appName, type, "categoryTypeDescription");
        String modifier = configDtos.get(0).getModifier();
        Date gmtCreate = configDtos.get(0).getGmtCreate();
        Date gmtModified = configDtos.get(0).getGmtModified();
        retJson.put("alias", alias);
        retJson.put("description", description);
        retJson.put("modifier", modifier);
        retJson.put("gmtCreate", gmtCreate);
        retJson.put("gmtModified", gmtModified);
        retJson.put("type", type);
        return retJson;
    }

    public JSONObject getPropertiesMapping(String type) {
        String name = "nodeManagerPropertiesMapping";
        return baseConfigService.getTypeNameContentWithDefault(type, name, new JSONObject());
    }

    public String setPropertiesMapping(String type, JSONObject propertiesMapping, String modifier) throws Exception {
        boolean shouldReindex = false;
        String name = "nodeManagerPropertiesMapping";
        baseConfigService.setTypeNameContent(type, name, propertiesMapping, modifier);
        JSONObject innerProperties = new JSONObject();
        if (elasticSearchIndicesService.getIndexes().contains(type)) {
            String realIndex = elasticSearchIndicesService.getRealIndexName(type);
            JSONObject properties = elasticSearchIndicesService.getMapping(type).getJSONObject(realIndex)
                .getJSONObject("mappings").getJSONObject("nr");
            innerProperties = getFlatProperties(properties);
            shouldReindex = true;
        }
        log.info("propertiesMapping: " + JSONObject.toJSONString(propertiesMapping, true));
        JSONObject setProperties = transferPropertiesFlat(propertiesMapping);
        log.info("setProperties: " + JSONObject.toJSONString(setProperties, true));
        for (String key : innerProperties.keySet()) {
            if (!setProperties.containsKey(key)) {
                elasticSearchIndicesService.removeTypeProperty(type, key);
            } else {
                if (!setProperties.getString(key).equals(innerProperties.getString(key))) {
                    if (setProperties.getString(key).equals("string")) {
                        elasticSearchConfigService.delElasticsearchProperties(type, key);
                        shouldReindex = true;
                    } else {
                        elasticSearchConfigService.addElasticsearchProperties(type, key,
                            setProperties.getString(key));
                        shouldReindex = true;
                    }
                }
            }
        }
        for (String key : setProperties.keySet()) {
            if (!innerProperties.containsKey(key)) {
                if (!setProperties.getString(key).equals("string")) {
                    elasticSearchConfigService.addElasticsearchProperties(type, key,
                        setProperties.getString(key));
                    shouldReindex = true;
                }
            }
        }
        if (!elasticSearchIndicesService.getIndexes().contains(type)) {
            elasticSearchIndicesService.create(type);
        } else if (shouldReindex) {
            return elasticSearchIndicesService.startReindex(type);
        }
        return null;
    }

    JSONObject getFlatProperties(JSONObject properties) {
        return flatProperties(getProperties(properties));
    }

    JSONObject transferPropertiesFlat(JSONObject properties) {
        return flatProperties(transferProperties(properties));
    }

    JSONObject getProperties(JSONObject properties) {
        JSONObject retJson = new JSONObject();
        properties = properties.getJSONObject("properties");
        for (String key : properties.keySet()) {
            JSONObject value = properties.getJSONObject(key);
            if (value.containsKey("properties")) {
                value.put("type", "object");
                properties.put(key, getProperties(value));
            } else {
                JSONObject newValue = new JSONObject();
                newValue.put("type", value.get("type"));
                properties.put(key, newValue);
            }
        }
        retJson.put("properties", properties);
        return retJson;
    }

    JSONObject transferProperties(JSONObject properties) {
        JSONObject subProperties = properties.getJSONObject("properties");
        for (String key : subProperties.keySet()) {
            JSONObject value = subProperties.getJSONObject(key);
            while (value.containsKey("items")) {
                value = value.getJSONObject("items");
            }
            subProperties.put(key, value);
        }
        properties.put("properties", subProperties);
        return properties;
    }

    JSONObject flatProperties(JSONObject properties) {
        JSONObject retJson = new JSONObject();
        JSONObject jsonObject = flatProperties(properties, null);
        for (String key : jsonObject.keySet()) {
            String value = jsonObject.getString(key);
            switch (value) {
                case "number":
                    retJson.put(key, "double");
                    break;
                case "integer":
                    retJson.put(key, "long");
                    break;
                case "string":
                    retJson.put(key, "string");
                    break;
                case "double":
                    retJson.put(key, "boolean");
                    break;
                default:
                    break;
            }
        }
        return retJson;
    }

    JSONObject flatProperties(JSONObject properties, String header) {
        JSONObject retJson = new JSONObject();
        properties = properties.getJSONObject("properties");
        for (String key : properties.keySet()) {
            String newKey = key;
            if (!StringUtils.isEmpty(header)) {
                newKey = header + "." + key;
            }
            JSONObject value = properties.getJSONObject(key);
            if (value.containsKey("properties")) {
                retJson.putAll(flatProperties(value, newKey));
            } else {
                retJson.put(newKey, value.getString("type"));
            }
        }
        return retJson;
    }

}
