package com.aliyun.oss.model;

public class Aggregation {
    private String field;
    private String operation;
    private double value;
    private AggregationGroups groups;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public AggregationGroups getGroups() {
        return groups;
    }

    public void setGroups(AggregationGroups groups) {
        this.groups = groups;
    }
}
