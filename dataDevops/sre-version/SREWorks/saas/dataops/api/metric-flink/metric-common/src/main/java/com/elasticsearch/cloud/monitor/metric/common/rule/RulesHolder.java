package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.expression.SelectedMetric;
import com.elasticsearch.cloud.monitor.metric.common.utils.FilterUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import lombok.Getter;

import java.util.*;

import static com.google.common.base.Charsets.UTF_8;

public class RulesHolder {

    private final static String APACK_DETECT_TYPE = "apack";

    private final List<Rule> rules;
    private final Map<String, List<SubQuery>> rulesMap;
    private final Map<String, List<SubQuery>> wildcardRulesMap;
    private final Map<String, List<SubQuery>> rulesCache;
    @Getter
    private final MetricStat metricStat;



    public RulesHolder(final List<Rule> rules) {
        this.rules = rules;
        this.rulesMap = Maps.newHashMap();
        this.wildcardRulesMap = Maps.newHashMap();
        this.rulesCache = Maps.newConcurrentMap();
        this.metricStat = new MetricStat();
        init(rules);
    }


    @SuppressWarnings("unchecked")
    public List<SubQuery> match(DataPoint dp) {
        // 通过metric+rules—map快速过滤掉空metric匹配点
        List<SubQuery> metricMatchRules = matchMetric(dp);
        if(metricMatchRules == null || metricMatchRules.size() == 0){
            metricStat.getEmptyMetricHitCnt().getAndIncrement();
            return null;
        }
        String md5Key = getMd5(dp);
        List<SubQuery> tagMatchRules = rulesCache.get(md5Key);
        if(tagMatchRules == null){
            metricStat.getCrossJoinMatchCnt().getAndAdd(metricMatchRules.size());
            tagMatchRules = matchTags(dp, metricMatchRules);
            rulesCache.put(md5Key, tagMatchRules);
            metricStat.getCacheMissCnt().getAndIncrement();
        }else{
            metricStat.getCacheHitCnt().getAndIncrement();
        }
        if(tagMatchRules.size() != 0){
            metricStat.getDownStreamCnt().getAndIncrement();
            metricStat.getCrossJoinCnt().getAndAdd(tagMatchRules.size());
        }
        return tagMatchRules;
    }

    private String getMd5(DataPoint dp){
        String metric = dp.getName();
        TreeMap<String, String> sortedTags = new TreeMap<>(dp.getTags());
        String tagStr = Joiner.on(",").withKeyValueSeparator("=").join(sortedTags);
        String key = metric + tagStr;
        return Hashing.md5().newHasher().putString(key, UTF_8).hash().toString();
    }


    private List<SubQuery> matchNoCache(DataPoint dp) {
        List<SubQuery> metricMatchRules = matchMetric(dp);
        if(metricMatchRules == null){
            return null;
        }
        return matchTags(dp, metricMatchRules);
    }


    private List<SubQuery> matchMetric(DataPoint dp){
        // 1 find normal
        List<SubQuery> ruleList = rulesMap.get(dp.getName());
        if (ruleList == null) {
            //2 find wildcard
            ruleList = new ArrayList<>();
            for(String name: wildcardRulesMap.keySet()){
                for(SubQuery subQuery: wildcardRulesMap.get(name)){
                    if(subQuery instanceof Rule){
                        Rule rule = (Rule) subQuery;
                        if(rule.match(dp.getName())){
                            ruleList.add(rule);
                        }
                    }
                }
            }
            if(ruleList.size() == 0) {
                return null;
            }
        }
        return ruleList;
    }

    private List<SubQuery> matchTags(DataPoint dp, List<SubQuery> rules){
        List<SubQuery> result = new LinkedList<>();
        for(SubQuery subQuery: rules){
            boolean find = FilterUtils.matchTags(dp.getTags(), subQuery.getFilterMap());
            if (find) {
                result.add(subQuery);
            }
        }
        return result;
    }

    public List<Rule> getRules() {
        return rules;
    }

    private void init(final Collection<Rule> rules) {
        int wildcardRulesCnt = 0;
        int splitRulesCnt = 0;
        int multiMetricRulesCnt = 0;
        int singleMetricRulesCnt = 0;
        int apackRulesCnt = 0;
        for (Rule rule : rules) {
            Map<String,List<SubQuery>> curMap = rulesMap;
            if(rule.getType() == 1){
                curMap = wildcardRulesMap;
                wildcardRulesCnt++;
            }
            if(rule.getType() == 0 || rule.getType() == 1) {
                List<SubQuery> ruleList = curMap.get(rule.getMetric());
                if (ruleList == null) {
                    ruleList = new ArrayList<>();
                    curMap.put(rule.getMetric(), ruleList);
                }
                ruleList.add(rule);
                singleMetricRulesCnt++;
            }else {
                List<SelectedMetric> metrics =  rule.getMetricCompose().getMetrics();
                for(SelectedMetric metric: metrics){
                    List<SubQuery> ruleList = curMap.get(metric.getMetric());
                    if (ruleList == null) {
                        ruleList = new ArrayList<>();
                        curMap.put(metric.getMetric(), ruleList);
                    }
                    ruleList.add(metric);
                    splitRulesCnt++;
                }
                multiMetricRulesCnt++;
            }
            if(APACK_DETECT_TYPE.equals(rule.getDetectType())){
                apackRulesCnt++;
            }
        }
        metricStat.setTotalRulesCnt(rules.size());
        metricStat.setTotalWildcardRulesCnt(wildcardRulesCnt);
        metricStat.setTotalSplitRulesCnt(splitRulesCnt);
        metricStat.setTotalMultiMetricRulesCnt(multiMetricRulesCnt);
        metricStat.setTotalSingleMetricRulesCnt(singleMetricRulesCnt);
        metricStat.setTotalApackRulesCnt(apackRulesCnt);
    }


    @Override
    public String toString() {
        return "RulesHolder{" + "rules=" + rulesMap + '}';
    }
}
