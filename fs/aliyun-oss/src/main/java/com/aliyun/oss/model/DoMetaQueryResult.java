package com.aliyun.oss.model;

import java.io.Serializable;

public class DoMetaQueryResult extends GenericResult implements Serializable {

    private String nextToken;
    private ObjectFiles files;
    private Aggregations aggregations;

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }

    public ObjectFiles getFiles() {
        return files;
    }

    public void setFiles(ObjectFiles files) {
        this.files = files;
    }

    public Aggregations getAggregations() {
        return aggregations;
    }

    public void setAggregations(Aggregations aggregations) {
        this.aggregations = aggregations;
    }
}
