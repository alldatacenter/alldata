package com.elasticsearch.cloud.monitor.metric.common.blink.utils;

import com.elasticsearch.cloud.monitor.metric.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.functions.FunctionContext;

/**
 * @author fangzong.lyj
 * @Date 2021-08-31
 */
@Slf4j
public class FlinkLogTracer {
    private boolean enable;

    public FlinkLogTracer(FunctionContext context) {
        String parameter = context.getJobParameter(Constants.LOG_TRACER_ENABLE, "false");
        enable = Boolean.parseBoolean(parameter);
    }

    public void trace(String format, Object... args) {
        if (enable) {
            log.error(format, args);
        }
    }
}
