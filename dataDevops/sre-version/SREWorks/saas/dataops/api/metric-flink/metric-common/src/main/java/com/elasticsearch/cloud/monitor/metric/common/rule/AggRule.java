package com.elasticsearch.cloud.monitor.metric.common.rule;


import com.elasticsearch.cloud.monitor.metric.common.rule.expression.SelectedMetric;
import com.elasticsearch.cloud.monitor.metric.common.utils.FilterUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by colin on 2017/5/15.
 */
public class AggRule extends  SubQuery implements Serializable {

    private Set<Long> ruleIds = new HashSet<>();

    private Set<Long> noDataRuleIds = new HashSet<>();

    public Set<Long> getRuleIds() {
        return ruleIds;
    }

    public Set<Long> getNoDataRuleIds() {
        return noDataRuleIds;
    }

    @Override
    public String getQueryId() {

        return StringUtils.join(ruleIds, "-");
    }

    /**
     * no parent is -1.
     * @return -1
     */
    @Override
    public Long getParentId() {
        return -1L;
    }

    public void addId(Long id){
        ruleIds.add(id);
    }

    public void addNodataId(Long id){
        noDataRuleIds.add(id);
    }

    public static AggRule from(SubQuery subQuery){
        AggRule aggRule = new AggRule();
        aggRule.setAggregator(subQuery.getAggregator());
        aggRule.setDsAggregator(subQuery.getDsAggregator());
        aggRule.setMetric(subQuery.getMetric());
        aggRule.setTags(subQuery.getTags());
        aggRule.setRate(subQuery.isRate());

        if( subQuery instanceof Rule && ((Rule) subQuery).getNoDataCondition()!= null){
            aggRule.addNodataId(subQuery.getParentId());
        }

        if( (subQuery instanceof Rule && ((Rule) subQuery).getDurationCondition()!= null)  ||  subQuery instanceof SelectedMetric){
            aggRule.addId(subQuery.getParentId());
        }

        return  aggRule;
    }

    @Override
    public String toString(){
        final StringBuilder sb = new StringBuilder();
        sb.append("ids=[").append(StringUtils.join(getRuleIds(),",")).append("]");
        sb.append(",").append("nodata_ids=[").append(StringUtils.join(getNoDataRuleIds(),",")).append("]");
        sb.append(",").append("metric=").append(getMetric());
        sb.append(",").append("ds-aggrator=").append(getDsAggregator());
        sb.append(",").append("aggrator=").append(getAggregator());
        sb.append(",").append("tags=").append(FilterUtils.getFilters(getFilterMap()));
        sb.append(",").append("rate=").append(isRate());
        return sb.toString();
    }

    @Override
    public boolean equals(Object o){
        return hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        int ret = 0;
        ret = ret*31 + (metric != null? metric.hashCode():0);
        ret = ret*31 + (dsAggregator != null? dsAggregator.hashCode():0);
        ret = ret*31 + (aggregator != null? aggregator.hashCode():0);
        ret = ret*31 + (tags != null? tags.hashCode(): 0);
        ret = ret*31 + (noDataRuleIds != null? noDataRuleIds.hashCode():0);
        ret = ret*31 + (ruleIds != null? ruleIds.hashCode():0);
        ret = ret*31 + (rate ? 1 : 0);

        return ret;
    }
}
