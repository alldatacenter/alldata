package com.alibaba.sreworks.dataset.services.otel;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.properties.ApplicationProperties;
import com.alibaba.sreworks.dataset.operator.SkywalkingOperator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;


/**
 * 追踪数据接口Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/03/12 19:47
 */

@Service
public class TraceService extends AbstractSkywalkingService {

    protected static final String BY_START_TIME_ORDER_TYPE = "BY_START_TIME";
    protected static final String BY_DURATION_ORDER_TYPE = "BY_DURATION";
    protected static final ImmutableList<String> ORDER_TYPES = ImmutableList.of(BY_START_TIME_ORDER_TYPE, BY_DURATION_ORDER_TYPE);

    protected static final String TRACE_STATE_ALL = "ALL";
    protected static final String TRACE_STATE_SUCCESS = "SUCCESS";
    protected static final String TRACE_STATE_ERROR = "ERROR";
    protected static final ImmutableList<String> TRACE_STATES = ImmutableList.of(TRACE_STATE_ALL, TRACE_STATE_SUCCESS, TRACE_STATE_ERROR);

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    SkywalkingOperator skywalkingOperator;

    public List<JSONObject> getAllTracingServices(Long startTimestamp, Long endTimestamp) throws Exception {
        JSONObject variables = new JSONObject();
        JSONObject duration = getDurationCondition(startTimestamp, endTimestamp, DURATION_MINUTE_STEP);
        variables.put("duration", duration);
        variables.put("keyword", "");

        JSONObject data = skywalkingOperator.executeGraphQL(buildGetAllServicesGraphQL(), variables);
        return data.getJSONArray("services").toJavaList(JSONObject.class);
    }

    public JSONObject getTracingService(String serviceName, Long startTimestamp, Long endTimestamp) throws Exception {
        List<JSONObject> services = getAllTracingServices(startTimestamp, endTimestamp);
        JSONObject tracingService = new JSONObject();
        if (!CollectionUtils.isEmpty(services)) {
            Optional<JSONObject> optional = services.stream().filter(service -> service.getString("label").equals(serviceName)).findAny();
            if (optional.isPresent()) {
                tracingService = optional.get();
            }
        }
        return tracingService;
    }

    public JSONObject getServiceTraces(String serviceName, String traceState, String orderType, Long startTimestamp, Long endTimestamp, Integer pageNum, Integer pageSize) throws Exception {
        traceState = StringUtils.isEmpty(traceState) ? TRACE_STATE_ALL : traceState;
        Preconditions.checkArgument(TRACE_STATES.contains(traceState), String.format("状态参数traceState[%s]非法, 支持状态[%s]", traceState, TRACE_STATES));

        orderType = StringUtils.isEmpty(orderType) ? BY_START_TIME_ORDER_TYPE : orderType;
        Preconditions.checkArgument(ORDER_TYPES.contains(orderType), String.format("排序参数orderType[%s]非法, 支持类型[%s]", traceState, ORDER_TYPES));


        JSONObject service = getTracingService(serviceName, startTimestamp, endTimestamp);
        JSONObject traceDatas = new JSONObject();
        if (!CollectionUtils.isEmpty(service)) {
            JSONObject queryDuration = getDurationCondition(startTimestamp, endTimestamp, DURATION_MINUTE_STEP);

            JSONObject paging = new JSONObject();
            paging.put("pageNum", pageNum);
            paging.put("pageSize", pageSize);
            paging.put("needTotal", true);

            JSONObject condition = new JSONObject();
            condition.put("paging", paging);
            condition.put("queryDuration", queryDuration);
            condition.put("queryOrder", orderType);
            condition.put("traceState", traceState);
            condition.put("serviceId", service.getString("key"));

            JSONObject variables = new JSONObject();
            variables.put("condition", condition);

            JSONObject data = skywalkingOperator.executeGraphQL(buildQueryBasicTracesGraphQL(), variables);
            traceDatas = data.getJSONObject("data");
        }
        return traceDatas;
    }

    public JSONObject getTraceById(String traceId) throws Exception {
        JSONObject variables = new JSONObject();
        variables.put("traceId", traceId);
        return skywalkingOperator.executeGraphQL(buildQueryTraceGraphQL(), variables);
    }
}
