//package com.elasticsearch.cloud.monitor.metric.common.rule;
//
//import com.elasticsearch.cloud.monitor.commons.rule.Rule;
//import com.elasticsearch.cloud.monitor.commons.rule.filter.TagVFilter;
//import com.elasticsearch.cloud.monitor.metric.common.rule.constant.Agg;
//import com.elasticsearch.cloud.monitor.metric.common.rule.constant.SinkType;
//import com.elasticsearch.cloud.monitor.metric.common.rule.constant.SourceType;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * @author xiaoping
// * @date 2019/11/22
// */
//@Getter
//@Setter
//@Slf4j
//public class SimpleRule {
//
//    private static final String DEFAULT_GRANULARITY = "1m";
//
//    private String metricName;
//    private Agg aggregation;
//    private Agg downsampleAggregation;
//    private String expression;
//    private String granularity;
//    // private List<TagVFilter> tags;
//    private Map<String, TagVFilter> tags;
//    private List<RuleInfo> ruleInfos;
//
//    @Getter
//    @Setter
//    public static class RuleInfo {
//        private long ruleId;
//        private SinkType sinkType;
//        private SourceType sourceType;
//
//        public RuleInfo(long ruleId, SinkType sinkType,
//            SourceType sourceType) {
//            this.ruleId = ruleId;
//            this.sinkType = sinkType;
//            this.sourceType = sourceType;
//        }
//    }
//
//    public static SimpleRule createRule(String metricName, String agg, String dsAgg, Rule rule,
//        List<RuleInfo> ruleInfos) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
//        SimpleRule simpleRule = new SimpleRule();
//        simpleRule.setMetricName(metricName);
//        simpleRule.setAggregation(Agg.valueOf(agg));
//        simpleRule.setDownsampleAggregation(Agg.valueOf(dsAgg));
//        simpleRule.setGranularity(DEFAULT_GRANULARITY);
//        simpleRule.setTags(rule.getFilterMap());
//        if (simpleRule.getTags() == null) {
//            try {
//                log.error(rule.getName() + rule.getId() + objectMapper.writeValueAsString(simpleRule.getTags()));
//            } catch (JsonProcessingException e) {
//                log.error("", e);
//            }
//        }
//        simpleRule.setRuleInfos(ruleInfos);
//
//        return simpleRule;
//    }
//}
