package com.elasticsearch.cloud.monitor.metric.common.blink.mock;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.table.functions.FunctionContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xingming.xuxm
 * @Date 2019-11-29
 */
public class FakeFunctionContext extends FunctionContext {
    private Map<String, String> jobParaMap = new HashMap<>();
    @Setter
    @Getter
    private int indexOfThisSubtask = 0;
    @Setter
    @Getter
    private int numberOfParallelSubtasks = 1;

    public FakeFunctionContext(RuntimeContext context) {
        super(context);
    }

    @Override
    public MetricGroup getMetricGroup() {
        return null;
    }

    @Override
    public File getCachedFile(String s) {
        return null;
    }

    public void setJobParameter(String s, String s1) {
        jobParaMap.put(s, s1);
    }

    @Override
    public String getJobParameter(String s, String s1) {
        if (jobParaMap.containsKey(s)) {
            return jobParaMap.get(s);
        }
        return s1;
    }
}
