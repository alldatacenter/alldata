package com.elasticsearch.cloud.monitor.metric.common.constant;

/**
 * @author xingming.xuxm
 * @Date 2019-11-29
 */
public class Constants {

    /**
     * monitor
     */
    public final static String MONITOR_TENANT_NAME = "monitor.tenant.name";
    public final static String MONITOR_SEREVICE_NAME = "monitor.service.name";
    public final static String MONITOR_SEREVICE_NAME_DEF = "elastic.monitor.blink";

    public final static String MONITOR_JOB_NAME = "monitor.job.name";

    /**
     * monitor
     */
    public final static String MONITOR_SLS_REPORT_ENABLE = "monitor_sls_report_enable";
    public final static String MONITOR_SLS_ENDPOINT = "monitor_sls_endpoint";
    public final static String MONITOR_SLS_PROJECT = "monitor_sls_project";
    public final static String MONITOR_SLS_LOGSTORE = "monitor_sls_logstore";
    public final static String MONITOR_SLS_ACCESS_KEY = "monitor_sls_access_key";
    public final static String MONITOR_SLS_ACCESS_SECRET = "monitor_sls_access_secret";

    public final static String MONITOR_KMON_FLUME_ADDRESS = "monitor_kmon_flume_address";
    public final static String MONITOR_KMON_FLUME_ADDRESS_ASSIGNED = "monitor_kmon_flume_address_assigned";
    public final static String MONITOR_KMON_FLUME_REPORT_ENABLE = "monitor_kmon_flume_report_enable";

    public final static String MONITOR_OPENTSDB_REPORT_ENABLE = "monitor_opentsdb_report_enable";
    public final static String MONITOR_OPENTSDB_ENDPOINT = "monitor_opentsdb_endpoint";

    public final static String MONITOR_SEND_METRIC_NAME_FIELD = "@metric_name";
    public final static String MONITOR_SEND_METRIC_VALUE_FIELD = "@metric_value";
    public final static String MONITOR_SEND_METRIC_TIMESTAMP_FIELD = "@timestamp";


    /**
     * rule 存储类型
     */
    public static final String RULE_STORAGE_TYPE = "rule.storage.type";

    /**
     * oss rule
     */
    public static final String RULE_PATH = "rule.oss.path";
    public static final String RULE_REFRESH_PERIOD = "rule.refresh.period";
    public static final String RULE_REFRESH_SHUFFLE = "rule.refresh.shuffle";
    public static final String RULE_FOR_EMON = "rule.for.emon";

    public static long RULE_REFRESH_PERIOD_DEF = 600000;
    public static long RULE_REFRESH_SHUFFLE_DEF = 600000;

    /**
     * minio rule
     */
    public static final String RULE_MINIO_ENDPOINT = "rule.minio.endpoint";
    public static final String RULE_MINIO_ACCESS_KEY = "rule.minio.accessKey";
    public static final String RULE_MINIO_SECRET_KEY = "rule.minio.secretKey";
    public static final String RULE_MINIO_BUCKET = "rule.minio.bucket";
    public static final String RULE_MINIO_FILE = "rule.minio.file";


    /**
     * sls source
     */
    public static final String SLS_DOWNSAMPLE_FILTER_ENABLE = "sls.downsample.filter.enable";
    public static final String SLS_DOWNSAMPLE_METRIC_PATTERN = "sls.downsample.metric.pattern";
    public static final String SLS_DOWNSAMPLE_FILTER_INCLUDE = "sls.downsample.filter.include";
    public static final String SLS_DOWNSAMPLE_TENANT_PREFIX = "sls.downsample.tenant.prefix";
    public static final String SLS_DOWNSAMPLE_DURATIONS = "sls.downsample.durations";
    public static final String SLS_DOWNSAMPLE_ENABLE_USE_DP_TIMESTAMP_RECYCLUE = "sls.downsample.use.dp.timstamp.recycle";

    public static final String SYNCSERIVICE_APP_FILTER_ENABLE = "syncserivce.app.filter.enable";
    public static final String REMOVE_TAG_KEYS = "remove_tag_keys";

    /**
     * log trace
     */
    public static final String LOG_TRACER_ENABLE = "log.trace.enable";

    /**
     * metrics
     */
    public static String METRIC_INSTANCE_CACHE_SECONDS = "metric.instance.cache.seconds";
    public static String METRIC_NAME = "metric.name";
    public static String METRIC_TEAM_ID = "metric.team.id";
    public static String METRIC_APP_ID = "metric.app.id";


    public static final String COMMON_FUNCTION_OPEN_CALLED = "common.function.open.called";
    public static final String COMMON_FUNCTION_CLOSE_CALLED = "common.function.close.called";

    public static final String TENANT_TAG = "__tenant__";
}
