package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar;

/**
 * 封装集合范围判断预算符
 *
 * @author feiquan
 */
public enum RangeType {
    /**
     * start <= x < end
     * 适用于大部份时间匹配场景，由其是按天查询
     * 为了兼容之前的配置方式
     */
    DEFAULT("range", "gte", "lt"),
    /**
     * 等同 DEFAULT
     */
    INCLUDE_START("rangeStart", "gte", "lt"),
    /**
     * start < x <= end
     * 几乎不会用
     */
    INCLUDE_END("rangeEnd", "gt", "lte"),
    /**
     * start <= x <= end
     * 适用于非时间，或时间已经细化到了毫秒的查询
     */
    INCLUDE_ALL("rangeAll", "gte", "lte"),
    /**
     * start < x < end
     * 几乎不会用
     */
    EXCLUDE_ALL("rangeMiddle", "gt", "lt");

    private String key;
    private String startOper;
    private String endOper;

    RangeType(String key, String startOper, String endOper) {
        this.key = key;
        this.startOper = startOper;
        this.endOper = endOper;
    }

    public String getKey() {
        return key;
    }

    public String getStartOper() {
        return startOper;
    }

    public String getEndOper() {
        return endOper;
    }
}
