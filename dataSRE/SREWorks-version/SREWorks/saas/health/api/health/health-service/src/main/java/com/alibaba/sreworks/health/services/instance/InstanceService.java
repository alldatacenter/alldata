package com.alibaba.sreworks.health.services.instance;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.definition.DefinitionService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实例Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/30 10:13
 */
@Slf4j
@Service
public class InstanceService {
    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    IncidentTypeMapper incidentTypeMapper;

    @Autowired
    DefinitionService definitionService;

    protected Map<String, Object> buildTimeAggParams(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("dateFormat", "%Y-%m-%d");
        if (timeUnit.equals("m")) {
            params.put("dateFormat", "%Y-%m");
        }
        params.put("startTimestamp", new Date(startTimestamp));
        params.put("endTimestamp", new Date(endTimestamp));
        if (StringUtils.isNotEmpty(appInstanceId)) {
            params.put("appInstanceId", appInstanceId);
        }

        return params;
    }

    protected Set<String> buildTimeAggKeys(String timeUnit, Long startTimestamp, Long endTimestamp) {
        Set<String> timeKeys = new TreeSet<>();

        long gapTimestamp = 86400000L;
        DateFormat dateFormat = new SimpleDateFormat(timeUnit.equals("m") ? "yyyy-MM" : "yyyy-MM-dd");

        Long timstamp = startTimestamp;
        while (timstamp <= endTimestamp) {
            timeKeys.add(dateFormat.format(new Date(timstamp)));
            timstamp += gapTimestamp;
        }

        return timeKeys;
    }

    protected List<JSONObject> richTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, List<JSONObject> raws) {
        Set<String> timeKeys = buildTimeAggKeys(timeUnit, startTimestamp, endTimestamp);
        Map<String, JSONObject> rich = new TreeMap<>();
        for(String timeKey : timeKeys) {
            JSONObject item = new JSONObject();
            item.put("aggTime", timeKey);
            item.put("cnt", 0);
            rich.put(timeKey, item);
        }

        Map<String, JSONObject> agg = new TreeMap<>();
        raws.forEach(raw -> agg.put(raw.getString("aggTime"), raw));
        rich.putAll(agg);

        return Arrays.asList(rich.values().toArray(new JSONObject[0]));
    }

    protected List<Integer> getDefIds(String appId, String appComponentName, String category) {
        List<CommonDefinition> definitions = getDefs(appId, appComponentName, category);
        if (CollectionUtils.isEmpty(definitions)) {
            return null;
        }
        return definitions.stream().map(CommonDefinition::getId).collect(Collectors.toList());
    }

    protected List<CommonDefinition> getDefs(String appId, String appComponentName, String category) {
        CommonDefinitionExample definitionExample = new CommonDefinitionExample();
        CommonDefinitionExample.Criteria criteria = definitionExample.createCriteria();
        if(StringUtils.isNotEmpty(appId)) {
            criteria.andAppIdEqualTo(appId);
        }
        if (StringUtils.isNotEmpty(appComponentName)) {
            criteria.andAppComponentNameEqualTo(appComponentName);
        }
        if (StringUtils.isNotEmpty(category)) {
            criteria.andCategoryEqualTo(category);
        }

        List<CommonDefinition> definitions = definitionMapper.selectByExample(definitionExample);
        if (CollectionUtils.isEmpty(definitions)) {
            return null;
        }
        return definitions;
    }

    /**
     * 实例是否存在关联定义
     * @return
     */
    protected boolean existRefDefinition(Integer defId, String category) {
        CommonDefinition definition = definitionMapper.selectByPrimaryKey(defId);
        return definition != null && definition.getCategory().equals(category);
    }

    /**
     * 查询关联定义
     * @return
     */
    protected CommonDefinition getRefDefinition(Integer defId, String category) {
        CommonDefinition definition = definitionMapper.selectByPrimaryKey(defId);
        return definition != null && definition.getCategory().equals(category) ? definition : null;
    }

    protected JSONObject richInstance(JSONObject result) {
        if (!result.isEmpty()) {
            JSONObject definition = definitionService.getDefinitionById(result.getInteger("defId"));
            CommonDefinition commonDefinition = JSONObject.toJavaObject(definition, CommonDefinition.class);
            result.put("defName", commonDefinition.getName());
            result.put("appId", commonDefinition.getAppId());
            result.put("appName", commonDefinition.getAppName());
            result.put("appComponentName", commonDefinition.getAppComponentName());

            if (commonDefinition.getCategory().equals(Constant.INCIDENT)) {
                IncidentType incidentType = definition.getObject("incidentType", IncidentType.class);
                result.put("typeName", incidentType.getName());
                result.put("typeId", incidentType.getId());
            }
        }

        return result;
    }

    protected List<JSONObject> richInstances(List<JSONObject> results) {
        if (!results.isEmpty()) {
            List<Integer> defIds = results.stream().map(result -> result.getInteger("defId")).collect(Collectors.toList());
            List<JSONObject> definitions = definitionService.getDefinitionByIds(defIds);
            Map<Integer, JSONObject> definitionMap = new HashMap<>();
            definitions.forEach(definition -> definitionMap.put(definition.getInteger("id"), definition));

            results.forEach(result -> {
                CommonDefinition commonDefinition = JSONObject.toJavaObject(definitionMap.get(result.getInteger("defId")), CommonDefinition.class);
                result.put("defName", commonDefinition.getName());
                result.put("appId", commonDefinition.getAppId());
                result.put("appName", commonDefinition.getAppName());
                result.put("appComponentName", commonDefinition.getAppComponentName());

                if (commonDefinition.getCategory().equals(Constant.INCIDENT)) {
                    IncidentType incidentType = definitionMap.get(result.getInteger("defId")).getObject("incidentType", IncidentType.class);
                    result.put("typeName", incidentType.getName());
                    result.put("typeId", incidentType.getId());
                }
            });
        }

        return results;
    }
}
