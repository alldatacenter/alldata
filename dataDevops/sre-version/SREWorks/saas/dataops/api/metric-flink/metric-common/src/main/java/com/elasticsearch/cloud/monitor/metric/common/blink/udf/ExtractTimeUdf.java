package com.elasticsearch.cloud.monitor.metric.common.blink.udf;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.ImmutableDataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.exception.InvalidParameterException;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.ScalarFunction;

import java.sql.Timestamp;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class ExtractTimeUdf extends ScalarFunction {
    private static final Log log = LogFactory.getLog(ExtractTimeUdf.class);

    private long alignTimeMs;

    public static final String ALIGN_TIME_SECOND = "align_time_second";
    public static final String FOR_HOLO = "for_holo";
    public static final String HOLO_DELAY_TIME_SECOND = "holo_delay_time_second";
    public static final String PRINT_LATE_METRIC = "print_late_metric";
    private boolean forHolo;
    public volatile static long holoDelayTime = TimeUnit.MINUTES.toMillis(2);
    private boolean printLateMetric;
    private volatile long lastPrintTime;
    private volatile Set<String> late120s;
    private volatile Set<String> late90s;
    private volatile Set<String> late60s;

    @Override
    public void open(FunctionContext context) throws InvalidParameterException {
        alignTimeMs = TimeUnit.SECONDS.toMillis(Long.parseLong(context.getJobParameter(ALIGN_TIME_SECOND, "1")));
        forHolo = Boolean.parseBoolean(context.getJobParameter(FOR_HOLO, "false"));
        holoDelayTime = Long.parseLong(context.getJobParameter(HOLO_DELAY_TIME_SECOND, "120")) * 1000;
        printLateMetric = Boolean.parseBoolean(context.getJobParameter(PRINT_LATE_METRIC, "false"));
        if (printLateMetric) {
            late120s = Sets.newConcurrentHashSet();
            late90s = Sets.newConcurrentHashSet();
            late60s = Sets.newConcurrentHashSet();
            lastPrintTime = System.currentTimeMillis();
        }
    }

    public java.sql.Timestamp eval(String msg) {
        ImmutableDataPoint.MetricAndTimestamp metricAndTimestamp;
        try {
            metricAndTimestamp = ImmutableDataPoint.getTimestamp(msg);
        } catch (Exception e) {
            log.warn(String.format("ExtractTimeUdf parse content %s error %s", msg, e.getMessage()), e);
            return new Timestamp(0);
        }

        if (printLateMetric) {
            long diff = System.currentTimeMillis() - metricAndTimestamp.getTimestamp();
            if (diff > TimeUnit.SECONDS.toMillis(120)) {
                late120s.add(metricAndTimestamp.getMetric());
            } else if (diff > TimeUnit.SECONDS.toMillis(90)) {
                late90s.add(metricAndTimestamp.getMetric());
            } else if (diff > TimeUnit.SECONDS.toMillis(60)) {
                late60s.add(metricAndTimestamp.getMetric());
            }
            if (System.currentTimeMillis() - lastPrintTime > TimeUnit.MINUTES.toMillis(5)) {
                log.error("bigger than 120s metric, count " + late120s.size() + " " + new Gson().toJson(late120s));
                log.error("bigger than 90s smaller than 120s metric, count " + late90s.size() + " " + new Gson()
                    .toJson(late90s));
                log.error("bigger than 60s smaller than 90s metric, count " + late60s.size() + " " + new Gson()
                    .toJson(late60s));
                late60s.clear();
                late90s.clear();
                late120s.clear();
                lastPrintTime = System.currentTimeMillis();
            }
        }

        if (metricAndTimestamp.getTimestamp() > System.currentTimeMillis()) {
            return new Timestamp(0);
        }
        long alignTime = metricAndTimestamp.getTimestamp() / alignTimeMs * alignTimeMs;
        if (forHolo && holoLateMetric(metricAndTimestamp.getMetric())) {
            alignTime = metricAndTimestamp.getTimestamp() + holoDelayTime;
        }

        return new Timestamp(alignTime);
    }

    public static boolean holoLateMetric(String metric) {

        if (metric.startsWith("hologram") && (metric.endsWith("cpu_percent") || metric.endsWith("memory_percent"))) {
            return true;
        }
        return false;
    }
}
