package com.elasticsearch.cloud.monitor.metric.common.utils;

import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregator;
import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregators;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AggregatorUtils {

    private static final Log LOG = LogFactory.getLog(AggregatorUtils.class);

    public static String getDownsampler(String aggregator) {
        if(aggregator.equals("sum") ||aggregator.equals("count")){
            return "avg";
        }else{
            return aggregator;
        }
    }

    public static Aggregator getDownsampleAggregator(DataPoint dataPoint) {
        String aggregatorName = "avg";
        String metricName = dataPoint.getName();
        if (metricName.endsWith(".min")) {
            aggregatorName = "min";
        } else if (metricName.endsWith(".max")) {
            aggregatorName = "max";
        } else if (metricName.endsWith(".75percentile.5min")) {
            aggregatorName = "p75";
        } else if (metricName.endsWith(".95percentile.5min")) {
            aggregatorName = "p95";
        } else if (metricName.endsWith(".99percentile.5min")) {
            aggregatorName = "p99";
        }
        return Aggregators.get(aggregatorName);
    }
}
