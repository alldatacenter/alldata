package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.PartitionMapper;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class CategoryConfigService extends BaseConfigService {

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    private PartitionMapper partitionMapper;

    @Autowired
    private IndexMapper indexMapper;

    private JSONArray getCategoryIncludeIndexes(String category) {
        String name = "categoryIncludeIndexes";
        List<Object> indexes = new ArrayList<>(indexMapper.getAliasIndexes());
        return getCategoryNameContentWithDefault(category, name, new JSONArray(indexes));
    }

    private JSONArray getCategoryExcludeIndexes(String category) {
        String name = "categoryExcludeIndexes";
        return getCategoryNameContentWithDefault(category, name, new JSONArray());
    }

    public JSONArray getCategoryIndexes(String category) {
        JSONArray indexes = getCategoryIncludeIndexes(category);
        indexes.removeAll(getCategoryExcludeIndexes(category));
        indexes.retainAll(indexMapper.getAliasIndexes());
        return indexes;
    }

    public JSONObject getCategoryIndexFilterRule(String category, String index) {
        String name = "categoryIndexFilterRule";
        return getCategoryTypeNameContentWithDefault(category, index, name, new JSONObject());
    }

    public JSONArray getCategoryIndexSuggestFields(String category, String index) {
        String name = "categoryIndexSuggestFields";
        return getCategoryTypeNameContentWithDefault(category, index, name,
            Constant.ELASTICSEARCH_INDEX_SUGGEST_MATCH_FIELDS);
    }

    public JSONArray getCategoryIndexMatchFields(String category, String index) {
        String name = "categoryIndexMatchFields";
        return getCategoryTypeNameContentWithDefault(category, index, name,
            Constant.ELASTICSEARCH_INDEX_SEARCH_MATCH_FIELDS);
    }

    public String getCategoryIndexPartition(String category, String index) {
        String name = "categoryIndexPartition";
        List<String> partitions = partitionMapper.get(index);
        return getCategoryTypeNameContentWithDefault(category, index, name,
            CollectionUtils.isEmpty(partitions) ? "" : partitions.get(0));
    }

    public String getCategoryTypeAlias(String category, String type) {
        String name = "categoryTypeAlias";
        return getCategoryTypeNameContentWithDefault(category, type, name, type);
    }

    public String getRealTypeByAlias(String category, String typeAlias) {
        try {
            String name = "categoryTypeAlias";
            List<ConfigDto> configDtoList = getConfigDtoList(ConfigDto.builder().category(category).name(name).build());

            for (ConfigDto configDto : configDtoList) {
                if (configDto.getContent().equals(typeAlias)) {
                    return configDto.getNrType();
                }
            }
            configDtoList = getConfigDtoList(ConfigDto.builder()
                .category(Constant.DEFAULT_CATEGORY).name(name).build());
            for (ConfigDto configDto : configDtoList) {
                if (configDto.getContent().equals(typeAlias)) {
                    return configDto.getNrType();
                }
            }
            return typeAlias;
        } catch (Exception e) {
            log.error("getRealTypeByAlias: ", e);
            return typeAlias;
        }
    }

    public JSONObject getCategoryTypeFieldAlias(String category, String type) {
        String name = "categoryTypeFieldAlias";
        JSONObject defaultJson = getTypeNameContentWithDefault(type, name, new JSONObject());
        JSONObject categoryJson = getCategoryTypeNameContentWithDefault(category, type, name, new JSONObject());
        defaultJson.putAll(categoryJson);
        return defaultJson;
    }

    public String getCategoryTypeSpecFieldAlias(String category, String type, String field) {
        JSONObject jsonObject = getCategoryTypeFieldAlias(category, type);
        String retString = jsonObject.getString(field);
        if (StringUtils.isEmpty(retString)) {
            jsonObject = getCategoryTypeFieldAlias(Constant.DEFAULT_CATEGORY, type);
            retString = jsonObject.getString(field);
            if (StringUtils.isEmpty(retString)) {
                retString = field;
            }
        }
        return retString;
    }

    public JSONArray getCategoryTypeExLink(String category, String type) {
        String name = "categoryTypeExLink";
        return getCategoryTypeNameContentWithDefault(category, type, name, new JSONArray());
    }

    public String getCategoryTypeIdIcon(String category, String type, String id) {
        String name = "categoryTypeIdIcon";
        return getCategoryTypeIdNameContentWithDefault(category, type, id, name, "");
    }

    public String getCategoryTypeIdUrlKey(String category, String type, String id) {
        String name = "categoryTypeIdUrlKey";
        return getCategoryTypeIdNameContent(category, type, id, name);
    }

    public String getCategoryTypeIdTitleKey(String category, String type, String id) {
        String name = "categoryTypeIdTitleKey";
        return getCategoryTypeIdNameContentWithDefault(category, type, id, name,
            String.format("${%s}", Constant.INNER_ID));
    }

    public JSONArray getCategoryTypePreciseMatchFields(String category, String type) {
        String name = "preciseMatchFields";
        return getCategoryTypeNameContentWithDefault(category, type, name,
            new JSONArray(Arrays.asList(Constant.INNER_ID)));
    }

    public JSONArray getCategoryTypeExData(String category, String type) {
        String name = "categoryTypeExData";
        return getCategoryTypeNameContentWithDefault(category, type, name, new JSONArray());
    }

    public JSONObject getCategoryTypeExDataMeta(String category, String type) {
        JSONObject retJson = new JSONObject();
        JSONArray exDataConfigArray = getCategoryTypeExData(category, type);
        for (JSONObject tmpJson : exDataConfigArray.toJavaList(JSONObject.class)) {
            String exDataConfigType = tmpJson.getJSONObject("data").getString("type");
            String exDataConfigName = tmpJson.getJSONObject("data").getString("title");
            if (!retJson.containsKey(exDataConfigType)) {
                retJson.put(exDataConfigType, new JSONArray());
            }
            retJson.getJSONArray(exDataConfigType).add(exDataConfigName);
        }
        return retJson;
    }

    public JSONObject getCategoryTypeSpecExDataConfig(String category, String type, String exDataName) {
        JSONArray exDataConfigArray = getCategoryTypeExData(category, type);
        for (JSONObject tmpJson : exDataConfigArray.toJavaList(JSONObject.class)) {
            String exDataConfigName = tmpJson.getJSONObject("data").getString("title");
            if (exDataConfigName.equals(exDataName)) {
                return tmpJson;
            }
        }
        return new JSONObject();
    }

    public JSONArray getCategoryTypeExPropertiesConfig(String category, String type) {
        String name = "categoryTypeExPropertiesConfig";
        return getCategoryTypeNameContentWithDefault(category, type, name, new JSONArray());
    }

    public String getCategoryExtendCategory(String category) {
        String name = "categoryExtendCategory";
        return getCategoryNameContentWithOutExtend(category, name);
    }

    public JSONArray getCategoryTypeIncludeFields(String category, String type) {
        String name = "categoryTypeIncludeFields";
        return getCategoryTypeNameContentWithDefault(category, type, name, new JSONArray());
    }

    public JSONArray getCategoryTypeExcludeFields(String category, String type) {
        String name = "categoryTypeExcludeFields";
        return getCategoryTypeNameContentWithDefault(category, type, name, new JSONArray());
    }

    public int getCategoryChatKgNodesNum(String category) {
        String name = "categoryChatKgNodesNum";
        return getCategoryNameContentWithDefault(category, name, 2);
    }

    public int getCategoryChatDocumentNodesNum(String category) {
        String name = "categoryChatDocumentNodesNum";
        return getCategoryNameContentWithDefault(category, name, 2);
    }

    public String getCategoryChatTypeQueryGrammar(String category, String type) {
        String name = "categoryChatTypeQueryGrammar";
        StringBuilder defaultStringBuilder = new StringBuilder();
        defaultStringBuilder.append(type).append(" ");
        getCategoryTypePreciseMatchFields(category, type).toJavaList(String.class).forEach(s ->
            defaultStringBuilder.append(s).append(" "));
        return getCategoryTypeNameContentWithDefault(category, type, name, defaultStringBuilder.toString());
    }

    public String getCategoryTypeExtraTypes(String category, String type) {
        String name = "categoryTypeExtraType";
        return getCategoryTypeNameContentWithDefault(category, type, name, type);
    }
}
