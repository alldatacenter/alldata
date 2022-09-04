package com.elasticsearch.cloud.monitor.metric.common.checker.nodata;

import com.elasticsearch.cloud.monitor.metric.common.checker.ConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.nodata.NoDataCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVFilter;
import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVLiteralOrFilter;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NoDataConditionChecker implements ConditionChecker {
    private int continuousTimes = 0;
    private Map<String, Map<String, Integer>> expectSingleTagNodataValueCount = Maps.newConcurrentMap();
    private Cache<Map<String, String>, Integer> multiTagValueCount;

    public NoDataConditionChecker(NoDataCondition condition, Map<String, TagVFilter> tagVFilterMap) {
        multiTagValueCount = CacheBuilder.newBuilder().expireAfterAccess(45, TimeUnit.MINUTES).build();
        convertToMap(tagVFilterMap);
    }

    private void convertToMap(Map<String, TagVFilter> tagVFilterMap) {
        if (tagVFilterMap == null) {
            return;
        }
        for (Entry<String, TagVFilter> entry : tagVFilterMap.entrySet()) {
            String tagKey = entry.getKey();
            TagVFilter tagVFilter = entry.getValue();
            if (tagVFilter instanceof TagVLiteralOrFilter) {
                Set<String> values = ((TagVLiteralOrFilter)tagVFilter).getLiterals();
                if (values != null && values.size() > 0) {
                    Map<String, Integer> valueCount = Maps.newConcurrentMap();
                    for (String value : values) {
                        valueCount.put(value, 0);
                    }
                    expectSingleTagNodataValueCount.put(tagKey, valueCount);
                }
            }
        }
    }

    public List<Alarm> check(final Set<Long> ruleIds, final Rule rule, Map<String, Set<String>> occurTags,
                             Set<Map<String, String>> occurMultiTags) {
        NoDataCondition condition = rule.getNoDataCondition();
        List<Alarm> alarms = Lists.newArrayList();
        if (ruleIds.contains(rule.getId())) {
            continuousTimes = 0;
        } else {
            continuousTimes++;
            AlarmLevel level = condition.check(continuousTimes);
            if (level != null) {
                String time = constructTimeDescription(continuousTimes);
                alarms.add(new Alarm(level, "连续" + time + "没有数据"));
            }
        }

        if (occurTags == null) {
            occurTags = Maps.newConcurrentMap();
        }
        if (occurMultiTags == null) {
            occurMultiTags = Sets.newConcurrentHashSet();
        }
        List<Map<String, String>> tempNoDataLines = condition.getNoDataLines();
        if (tempNoDataLines != null && tempNoDataLines.size() > 0) {
            for (Map<String, String> line : tempNoDataLines) {
                Integer count = multiTagValueCount.getIfPresent(line);
                if (count == null) {
                    count = 0;
                    multiTagValueCount.put(line, 0);
                }
                if (occurMultiTags.contains(line)) {
                    multiTagValueCount.put(line, 0);
                } else {
                    multiTagValueCount.put(line, count + 1);
                }
                int tempCount = multiTagValueCount.getIfPresent(line);
                AlarmLevel level = condition.check(tempCount);
                if (level != null) {
                    String time = constructTimeDescription(tempCount);
                    alarms.add(new Alarm(level,
                        String.format("multi tag: %s 连续%s没有数据", line.toString(), time)));
                }

            }

        } else if (expectSingleTagNodataValueCount != null) {
            for (Entry<String, Map<String, Integer>> entry : expectSingleTagNodataValueCount.entrySet()) {
                String tagKey = entry.getKey();
                Set<String> occurValues = occurTags.get(tagKey);
                if (occurValues == null) {
                    occurValues = Sets.newConcurrentHashSet();
                }
                Map<String, Integer> countMap = entry.getValue();
                for (String value : countMap.keySet()) {
                    if (occurValues.contains(value)) {
                        countMap.put(value, 0);
                    } else {
                        countMap.put(value, countMap.get(value) + 1);
                        int tempCount = countMap.get(value);
                        AlarmLevel level = condition.check(tempCount);
                        if (level != null) {
                            String time = constructTimeDescription(tempCount);
                            alarms.add(new Alarm(level,
                                String.format("single tag: %s=%s 连续%s没有数据", tagKey, value, time)));
                        }
                    }
                }
            }
        }
        return alarms;
    }

    private String constructTimeDescription(int continuousTimes) {
        return TimeUtils.getDurationChineseName(continuousTimes + Constants.CHECK_INTERVAL_UNIT);
    }

}
