package com.elasticsearch.cloud.monitor.metric.common.pojo;

import lombok.Data;

/**
 * @author xiaoping
 * @date 2020/3/31
 */
@Data
public class Filter {
    private String filterKey;
    private String filterValue;

    public Filter() {
    }

    public Filter(String filterKey, String filterValue) {
        this.filterKey = filterKey;
        this.filterValue = filterValue;
    }

}
