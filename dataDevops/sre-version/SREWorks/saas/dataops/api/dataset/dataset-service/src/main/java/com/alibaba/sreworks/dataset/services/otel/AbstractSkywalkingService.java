package com.alibaba.sreworks.dataset.services.otel;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.exception.ParamException;

import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * 数据接口Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/03/12 19:47
 */


public abstract class AbstractSkywalkingService {
    protected static final String DURATION_DAY_STEP = "DAY";
    protected static final String DURATION_MINUTE_STEP = "MINUTE";

    protected JSONObject getDurationCondition(Long startTimestamp, Long endTimestamp, String step) throws Exception {
        if (startTimestamp > endTimestamp) {
            throw new ParamException("时间范围异常,开始时间晚于结束时间");
        }

        SimpleDateFormat simpleDateFormat;
        if (step.equals(DURATION_DAY_STEP)) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        } else if (step.equals(DURATION_MINUTE_STEP)) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HHmm");
        } else {
            throw new ParamException(String.format("时间粒度非法:%s, 支持粒度:%s", step, Arrays.asList(DURATION_DAY_STEP, DURATION_MINUTE_STEP)));
        }
        String start = simpleDateFormat.format(startTimestamp);
        String end = simpleDateFormat.format(endTimestamp);

        JSONObject duration = new JSONObject();
        duration.put("step", step);
        duration.put("start", start);
        duration.put("end", end);

        return duration;
    }

    protected String buildGetAllServicesGraphQL() {
        String graphQL = "query queryServices($duration: Duration!,$keyword: String!) {\n" +
                "    services: getAllServices(duration: $duration, group: $keyword) {\n" +
                "      key: id\n" +
                "      label: name\n" +
                "      group\n" +
                "    }\n" +
                "  }";
        return graphQL;
    }

    protected String buildQueryBasicTracesGraphQL() {
        String graphQL = "query queryTraces($condition: TraceQueryCondition) {\n" +
                "  data: queryBasicTraces(condition: $condition) {\n" +
                "    traces {\n" +
                "      key: segmentId\n" +
                "      endpointNames\n" +
                "      duration\n" +
                "      start\n" +
                "      isError\n" +
                "      traceIds\n" +
                "    }\n" +
                "    total\n" +
                "  }}";
        return graphQL;
    }

    protected String buildQueryTraceGraphQL() {
        String graphQL = "query queryTrace($traceId: ID!) {\n" +
                "  trace: queryTrace(traceId: $traceId) {\n" +
                "    spans {\n" +
                "      traceId\n" +
                "      segmentId\n" +
                "      spanId\n" +
                "      parentSpanId\n" +
                "      refs {\n" +
                "        traceId\n" +
                "        parentSegmentId\n" +
                "        parentSpanId\n" +
                "        type\n" +
                "      }\n" +
                "      serviceCode\n" +
                "      serviceInstanceName\n" +
                "      startTime\n" +
                "      endTime\n" +
                "      endpointName\n" +
                "      type\n" +
                "      peer\n" +
                "      component\n" +
                "      isError\n" +
                "      layer\n" +
                "      tags {\n" +
                "        key\n" +
                "        value\n" +
                "      }\n" +
                "      logs {\n" +
                "        time\n" +
                "        data {\n" +
                "          key\n" +
                "          value\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  }";
        return graphQL;
    }

    protected String buildQueryLogGraphQL() {
        String graphQL = "query queryLogs($condition: LogQueryCondition) {\n" +
                "    queryLogs(condition: $condition) {\n" +
                "        logs {\n" +
                "          serviceName\n" +
                "          serviceId\n" +
                "          serviceInstanceName\n" +
                "          serviceInstanceId\n" +
                "          endpointName\n" +
                "          endpointId\n" +
                "          traceId\n" +
                "          timestamp\n" +
                "          contentType\n" +
                "          content\n" +
                "          tags {\n" +
                "            key\n" +
                "            value\n" +
                "          }\n" +
                "        }\n" +
                "        total\n" +
                "    }}";
        return graphQL;
    }

}
