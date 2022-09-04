package com.alibaba.tesla.gateway.server.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.tesla.gateway.common.enums.AuthErrorTypeEnum;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class TeslaGatewayMetric {

    private static final String REQUEST_ERROR_TOTAL = "gateway.requests.error.total";
    private static final String CONNECT_NUM = "gateway.request.connect.count";
    private static final String HEADER_AUTH_ERROR_TOTAL = "gateway.header.auth.error.total";
    private static final String BLACKLIST_REJECT_TOTAL = "gateway.blacklist.reject.total";

    @Autowired
    private MeterRegistry meterRegistry;

    public void httpErrorRecord(HttpStatus status, String path, String xEnv) {
        Tags tags = Tags.of("httpCode", String.valueOf(status.value()), "httpStatus", status.getReasonPhrase(), "path",
            path, "x-env", xEnv);
        meterRegistry.counter(REQUEST_ERROR_TOTAL, tags).increment();
    }

    /**
     * 记录连接数
     *
     * @param count count
     */
    public void connectNum(AtomicLong count) {
        meterRegistry.gauge(CONNECT_NUM, count);
    }

    /**
     * header校验失败记录
     */
    public void headerCheckError(String path, String remoteInfo, AuthErrorTypeEnum errorType, String requestId) {
        Tags tags = Tags.of("path", path, "remoteInfo", remoteInfo, "errorType",
            errorType.name(), "requestId", requestId);
        meterRegistry.counter(HEADER_AUTH_ERROR_TOTAL, tags).increment();

    }

    /**
     * 记录过黑名单的指标
     * @param routeId
     * @param type
     */
    public void recordBlackListReject(String routeId, String type){
        Tags tags = Tags.of("type", type, "routeId", routeId);
        meterRegistry.counter(BLACKLIST_REJECT_TOTAL, tags).increment();
    }

}
