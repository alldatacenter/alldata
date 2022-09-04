package com.elasticsearch.cloud.monitor.metric.common.datapoint;

import java.io.Serializable;
import java.util.Map;

/**
 * @author xiaoping
 * @date 2019/12/3
 */
public interface MultiValueDataPoint extends Serializable {
    String getTenant();
    String getName();

    long getTimestamp();

    /**
     * avg sum count max min
     * @return
     */
    String getValues();

    Map<String, String> getTags();

    String getGranularity();
}
