package com.elasticsearch.cloud.monitor.metric.common.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author xiaoping
 * @date 2020/6/10
 */
@Setter
@Getter
@NoArgsConstructor
public class SearchConfigData {
    private String projectId;
    private SearchFilterConfig searchConfig;

    public SearchConfigData(String projectId,
        SearchFilterConfig searchConfig) {
        this.projectId = projectId;
        this.searchConfig = searchConfig;
    }
}
