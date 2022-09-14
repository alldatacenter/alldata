package com.alibaba.sreworks.dataset.common.constant;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 合法常量配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/08/10 16:17
 */
public class ValidConstant {
    public final static String DEFAULT_SORT_MODE = "asc";

    public static String MYSQL_SOURCE = "mysql";

    public static String ES_SOURCE = "es";

    public static List<String> SOURCE_TYPE_LIST = Arrays.asList(MYSQL_SOURCE, ES_SOURCE);

    public static List<String> ORDER_TYPE_LIST = Arrays.asList("asc", "desc");

    public static List<String> ES_SORT_MODE_LIST = Arrays.asList("min", "max", "sum", "avg", "median");

    public static List<String> ES_SORT_FORMAT_LIST = Arrays.asList("epoch_millis", "epoch_second", "date_optional_time", "strict_date_optional_time", "strict_date_optional_time_nanos", "basic_date", "basic_date_time", "basic_date_time_no_millis", "basic_ordinal_date", "basic_ordinal_date_time", "basic_ordinal_date_time_no_millis", "basic_time", "basic_time_no_millis", "basic_t_time", "basic_t_time_no_millis", "basic_week_date", "strict_basic_week_date", "basic_week_date_time", "strict_basic_week_date_time", "basic_week_date_time_no_millis", "strict_basic_week_date_time_no_millis", "date", "strict_date", "date_hour", "strict_date_hour", "date_hour_minute", "strict_date_hour_minute", "date_hour_minute_second", "strict_date_hour_minute_second", "date_hour_minute_second_fraction", "strict_date_hour_minute_second_fraction", "date_hour_minute_second_millis", "strict_date_hour_minute_second_millis", "date_time", "strict_date_time", "date_time_no_millis", "strict_date_time_no_millis", "hour", "strict_hour", "hour_minute", "strict_hour_minute", "hour_minute_second", "strict_hour_minute_second", "hour_minute_second_fraction", "strict_hour_minute_second_fraction", "hour_minute_second_millis", "strict_hour_minute_second_millis", "ordinal_date", "strict_ordinal_date", "ordinal_date_time", "strict_ordinal_date_time", "ordinal_date_time_no_millis", "strict_ordinal_date_time_no_millis", "time", "strict_time", "time_no_millis", "strict_time_no_millis", "t_time", "strict_t_time", "t_time_no_millis", "strict_t_time_no_millis", "week_date", "strict_week_date", "week_date_time", "strict_week_date_time", "week_date_time_no_millis", "strict_week_date_time_no_millis", "weekyear", "strict_weekyear", "weekyear_week", "strict_weekyear_week", "weekyear_week_day", "strict_weekyear_week_day", "year", "strict_year", "year_month", "strict_year_month", "year_month_day", "strict_year_month_day");

    public static List<String> GRANULARITY_LIST = Arrays.asList("1m", "5m", "10m", "30m", "1h", "6h", "1d", "7d", "1M", "12M");

    public static List<String> ES_PIPELINE_AGG_TYPE_LIST = Arrays.asList("derivative");

    public static List<String> ES_METRIC_AGG_TYPE_LIST = Arrays.asList("min", "max", "avg", "sum", "cardinality");

    public static List<String> ES_FIELD_AGG_TYPE_LIST = Stream.concat(ES_METRIC_AGG_TYPE_LIST.stream(),
            ES_PIPELINE_AGG_TYPE_LIST.stream()).collect(Collectors.toList());

    public static String ES_TERMS_BUCKET = "terms";

    public static String ES_DATE_HISTOGRAM_BUCKET = "date_histogram";

    public static List<String> ES_BUCKET_AGG_TYPE_LIST = Arrays.asList(ES_DATE_HISTOGRAM_BUCKET, ES_TERMS_BUCKET);
}
