package com.alibaba.sreworks.dataset.services.otel;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.properties.ApplicationProperties;
import com.alibaba.sreworks.dataset.operator.SkywalkingOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;


/**
 * 日志数据接口Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/03/12 19:47
 */

@Service
public class LogService extends AbstractSkywalkingService {

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    SkywalkingOperator skywalkingOperator;

    @Autowired
    TraceService traceService;

    public JSONObject getServiceLogs(String serviceName, String traceId, List<String> keywordsInclude, List<String> keywordsExclude, Long startTimestamp, Long endTimestamp, Integer pageNum, Integer pageSize) throws Exception {
        JSONObject service = traceService.getTracingService(serviceName, startTimestamp, endTimestamp);
        JSONObject logDatas = new JSONObject();

        if (!CollectionUtils.isEmpty(service)) {
            JSONObject condition = new JSONObject();
            if (StringUtils.isBlank(traceId)) {
                JSONObject queryDuration = getDurationCondition(startTimestamp, endTimestamp, DURATION_MINUTE_STEP);
                condition.put("queryDuration", queryDuration);
            } else {
                JSONObject relatedTrace = new JSONObject();
                relatedTrace.put("traceId", traceId);
                condition.put("relatedTrace", relatedTrace);
            }

            JSONObject paging = new JSONObject();
            paging.put("pageNum", pageNum);
            paging.put("pageSize", pageSize);
            paging.put("needTotal", true);

            condition.put("paging", paging);
            condition.put("serviceId", service.getString("key"));

            condition.put("keywordsOfContent", keywordsInclude.toArray());
            condition.put("excludingKeywordsOfContent", keywordsExclude.toArray());

            JSONObject variables = new JSONObject();
            variables.put("condition", condition);

            JSONObject data = skywalkingOperator.executeGraphQL(buildQueryLogGraphQL(), variables);
            logDatas = data.getJSONObject("queryLogs");
        }
        return logDatas;
    }
}
