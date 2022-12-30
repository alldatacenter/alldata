package com.elasticsearch.cloud.monitor.metric.common.rule.expression;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/11 13:49
 */
public class MetricCompose {

    @JsonProperty("metrics")
    private List<SelectedMetric> metrics;

    @JsonProperty("expression")
    private String expression;

    @JsonIgnore
    private String name;

    /**
     * 0 - no join
     * 1 - inner join
     * 2 - cross join
     */
    @JsonProperty("join")
    private int join;

    public List<SelectedMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<SelectedMetric> metrics) {
        this.metrics = metrics;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getName() {
        if(name != null){
            return name;
        }else {
            StringBuilder sb = new StringBuilder(48);
            sb.append("compose");
            if(metrics.size() > 0){
                sb.append(".");
                sb.append(metrics.get(0).getParentId());
            }
            for(SelectedMetric metric :metrics){
                sb.append(".");
                sb.append(metric.getId());
            }

            return  sb.toString();
        }

    }

    public void setName(String name) {
        this.name = name;
    }

    public int getJoin() {
        return join;
    }

    public void setJoin(int join) {
        this.join = join;
    }

    @JsonIgnore
    public void setInnerJoin() {
        this.join = 1;
    }
    @JsonIgnore
    public void setCrossJoin() {
        this.join = 2;
    }
    @JsonIgnore
    public boolean isInnerJoin(){
        return join == 1;
    }
    @JsonIgnore
    public boolean isCrossJoin(){
        return join == 2;
    }


}
