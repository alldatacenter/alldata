package com.alibaba.tesla.tkgone.server.domain.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.Consumer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;

/**
 * @author yangjinghua
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Log4j
public class ConsumerDto extends Consumer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    JSONObject sourceInfoJson = new JSONObject();
    JSONArray importConfigArray = new JSONArray();
    JSONObject importConfigJson = new JSONObject();

    public ConsumerDto(Consumer consumer) {

        BeanUtils.copyProperties(consumer, this);

        setImportConfigArray(JSONArray.parseArray(getImportConfig()));
        setSourceInfoJson(JSONObject.parseObject(getSourceInfo()));
        setImportConfigJson(JSONObject.parseObject(getUserImportConfig()));

    }

    public Consumer toConsumer() {

        importConfigJson2importConfigArray();
        analysisSourceInfoJson();
        Consumer consumer = new Consumer();
        BeanUtils.copyProperties(this, consumer);

        if (!sourceInfoJson.isEmpty()) {
            consumer.setSourceInfo(JSONObject.toJSONString(getSourceInfoJson()));
        }
        if (!importConfigArray.isEmpty()) {
            consumer.setImportConfig(JSONArray.toJSONString(getImportConfigArray()));
        }
        consumer.setUserImportConfig(JSONObject.toJSONString(getImportConfigJson()));

        return consumer;

    }

    private void importConfigJson2importConfigArray() {
        if (CollectionUtils.isEmpty(importConfigArray) && !CollectionUtils.isEmpty(importConfigJson)) {
            JSONArray relations = importConfigJson.getJSONArray("relations");
            JSONObject nodes = importConfigJson.getJSONObject("nodes");
            for (int i = 0; i < relations.size(); i++) {
                JSONArray relation = relations.getJSONArray(i);
                for (int index = 0; index < relation.size(); index++) {
                    JSONObject node = relation.getJSONObject(index);
                    String nodeName = node.getString(Constant.IMPORT_CONFIG_JSON_NODE_NAME_KEY);
                    if (!StringUtils.isEmpty(nodeName)) {
                        if (!nodes.containsKey(nodeName)) {
                            log.error("不包含该节点: " + nodeName);
                        }
                        node.putAll(nodes.getJSONObject(nodeName));
                        node.remove(Constant.IMPORT_CONFIG_JSON_NODE_NAME_KEY);
                    }
                    relation.set(index, node);
                }
            }
            importConfigArray = relations;
        }
    }

    private void analysisSourceInfoJson() {
        if (!org.springframework.util.StringUtils.isEmpty(sourceInfoJson.get("cronExpression"))) {
            try {
                String cronExpression = sourceInfoJson.getString("cronExpression");
                cronExpression = StringUtils.strip(cronExpression, "\"");
                JSONObject jsonObject = Tools.analysisCronExpression(cronExpression);
                sourceInfoJson.put("startPeriod", jsonObject.getInteger("startPeriod"));
                sourceInfoJson.put("interval", jsonObject.getInteger("interval"));
            } catch (ParseException e) {
                log.error("cronExpression 解析失败: ", e);
            }
        }
    }

}