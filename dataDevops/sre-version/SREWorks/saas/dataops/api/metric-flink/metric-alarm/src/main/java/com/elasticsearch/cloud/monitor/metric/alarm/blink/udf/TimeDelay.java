package com.elasticsearch.cloud.monitor.metric.alarm.blink.udf;

import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.ScalarFunction;

/**
 * 汇报一下metric
 *
 * @author xingming.xuxm
 * @Date 2019-12-04
 */
public class TimeDelay extends ScalarFunction {
    private long maxDelayThreshold = 15 * 60 * 1000L;

    @Override
    public void open(FunctionContext context) {
        String delay = context.getJobParameter("data.max_delay", "");
        if (!delay.isEmpty()) {
            maxDelayThreshold = Long.parseLong(delay);
        }
    }

    public boolean eval(long timestamp) {
        return eval(timestamp, maxDelayThreshold);
    }

    public boolean eval(long timestamp, long maxDelay) {
        long delay = System.currentTimeMillis() - timestamp;
        if (delay > maxDelay) {
            return true;
        }

        if (delay < 0) {
            return true;
        }
        return false;
    }
}
