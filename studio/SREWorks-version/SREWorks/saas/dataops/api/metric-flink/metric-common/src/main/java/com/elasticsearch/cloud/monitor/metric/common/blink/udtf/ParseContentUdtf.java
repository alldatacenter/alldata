//package com.elasticsearch.cloud.monitor.metric.common.blink.udtf;
//
//import com.alibaba.fastjson.JSON;
//import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
//import com.elasticsearch.cloud.monitor.metric.common.blink.utils.Filter;
//import com.elasticsearch.cloud.monitor.metric.common.datapoint.ImmutableDataPoint;
//import com.google.common.collect.Maps;
//import org.apache.commons.lang.StringUtils;
//import org.apache.flink.api.java.tuple.Tuple4;
//import org.apache.flink.table.functions.FunctionContext;
//import org.apache.flink.table.functions.TableFunction;
//
//import java.sql.Time;
//import java.util.Arrays;
//import java.util.TreeMap;
//
//
//public class ParseContentUdtf extends TableFunction<Tuple4<String, Double, java.sql.Time, String>> {
//    private boolean filtered = false;
//    private String FILTER_PATTERN_KEY = "metric.filter.pattern";
//    private String FILTER_ENABLE_KEY = "metric.filter.enable";
//
//    @Override
//    public void open(FunctionContext context) {
//        this.filtered = Boolean.parseBoolean(context.getJobParameter(FILTER_ENABLE_KEY, "false"));
//        String filterPattern = context.getJobParameter(FILTER_PATTERN_KEY, "");
//        if (filtered && StringUtils.isNotEmpty(filterPattern)) {
//            Filter.setList(Arrays.asList(filterPattern.split(",")));
//        }
//    }
//
//    /**
//     * select S.id, S.content, T.a, T.b, T.c
//     * from input_stream as S,
//     * lateral table(parseUdtf(content)) as T(a, b, c);
//     *
//     * @param msg
//     */
//
//    public void eval(String msg) throws Exception {
//        DataPoint dp = ImmutableDataPoint.from(msg);
//        if (filtered && !Filter.match(dp.getName())) {
//            return;
//        }
//        TreeMap<String, String> map = Maps.newTreeMap();
//        map.putAll(dp.getTags());
//        Tuple4<String, Double, java.sql.Time, String> tuple4 = Tuple4.of(dp.getName(), dp.getValue(),
//                new Time(dp.getTimestamp()), JSON.toJSONString(map));
//        collect(tuple4);
//    }
//}
