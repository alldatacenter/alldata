package com.elasticsearch.cloud.monitor.metric.alarm.blink.utils;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * 报警事件, sqi现在的事件格式
 *
 * @author xingming.xuxm
 */
@Getter
@Setter
public class AlarmEvent {
    private String id;
    /**
     * 报警项的名称
     */
    private String title;
    /**
     * 报警内容
     */
    private String text;
    private long time;
    /**
     * sqi业务名(应用名称)
     */
    private String service;
    private Set<String> tags;
    /**
     * sqi识别的类型 alert-critical, alert-warning, 还有别的, 和报警无关
     */
    private String type;
    /**
     * 事件来源
     */
    private String source;
    /**
     * for emon alarm-group
     */
    private String group;
    private String uid;

    public void addTag(String tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
    }

    public void addTags(Map<String, String> tagMap) {
        if (tagMap == null) {
            return;
        }
        if (tags == null) {
            tags = new HashSet<>();
        }
        for (String key : tagMap.keySet()) {
            addTag(key + "=" + tagMap.get(key));
        }
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
