package com.alibaba.tesla.gateway.server.filter.global;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.gateway.server.domain.SwitchViewMapping;
import com.alibaba.tesla.gateway.server.domain.SwitchViewUser;
import com.alibaba.tesla.gateway.server.scheduler.SwitchViewScheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class ViewSwitchGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_NAME_USER = "X-Auth-User";

    private static final String HEADER_NAME_EMP_ID = "X-EmpId";

    private static final String HEADER_NAME_USER_ID = "X-Auth-UserId";

    private static final String HEADER_NAME_BUC_ID = "X-Buc-Id";

    private static final String HEADER_NAME_FROM_EMP_ID = "X-From-EmpId";

    private static final String HEADER_NAME_FROM_USER = "X-From-Auth-User";

    private static final String HEADER_NAME_FROM_BUC_ID = "X-From-Buc-Id";

    private static final String COOKIE_NAME = "tesla_switch_view";

    @Autowired
    private SwitchViewScheduler switchViewScheduler;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getCookies().containsKey(COOKIE_NAME) && null != exchange.getRequest().getCookies()
            .getFirst(COOKIE_NAME)) {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_NAME);
            String empId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME_EMP_ID);
            SwitchViewMapping mapping = TeslaGsonUtil.gson.fromJson(
                new String(Base64Utils.decodeFromString(cookie.getValue())), SwitchViewMapping.class);
            SwitchViewUser switchUser = SwitchViewUser.builder()
                .empId(empId)
                .loginName(mapping.getFromLoginName())
                .bucId(mapping.getFromBucId())
                .build();
            if (switchViewScheduler.isInWhiteList(switchUser)) {
                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(HEADER_NAME_EMP_ID, new String[] {mapping.getToEmpId()})
                    .header(HEADER_NAME_USER, new String[] {mapping.getToLoginName()})
                    .header(HEADER_NAME_USER_ID, new String[] {mapping.getToEmpId()})
                    .header(HEADER_NAME_BUC_ID, new String[] {mapping.getToBucId()})
                    .header(HEADER_NAME_FROM_EMP_ID, new String[] {mapping.getFromEmpId()})
                    .header(HEADER_NAME_FROM_USER, new String[] {mapping.getFromLoginName()})
                    .header(HEADER_NAME_FROM_BUC_ID, new String[] {mapping.getFromBucId()})
                    .build();

                log.info("header={}", JSONObject.toJSONString(request.getHeaders()));
                log.info("actionName=switchView||id={}||empId={}||viewUserId={}||viewUserName={}||viewBucId={}",
                    exchange.getRequest().getId(), empId, mapping.getToEmpId(), mapping.getToLoginName(),
                    mapping.getToBucId());
                return chain.filter(exchange.mutate().request(request).build());
            } else {
                log.warn("not in white list, empId={}", empId);
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrderManager.VIEW_SWITCH_FILTER;
    }
}
