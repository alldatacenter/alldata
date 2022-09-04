package com.elasticsearch.cloud.monitor.metric.common.rule.expression;

import com.elasticsearch.cloud.monitor.metric.common.rule.SubQuery;
import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/11 13:48
 */
public class SelectedMetric extends SubQuery implements Serializable{

    @JsonProperty("id")
    private String id;

    @JsonIgnore
    public Long parentId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @JsonIgnore
    public boolean isMultiTimeseries(){
        if(tags == null){
            return false;
        }
        for(TagVFilter filter:tagVFilters){
            String type = filter.getType();
            if( type.equals("not_literal_or")
                || type.equals("not_iliteral_or")
                || type.equals("regexp")
                || type.equals("wildcard")
                || type.equals("iwildcard")){
                return true;
            }
            if(type.equals("literal_or") || type.equals("iliteral_or") ){
                if( filter.getFilter().contains("|")){
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String getQueryId() {
        return parentId + "-" + id;
    }
}
