package com.alibaba.tesla.gateway.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.common.exception.GatewayException;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.domain.SwitchViewMapping;
import com.alibaba.tesla.gateway.server.domain.SwitchViewUser;
import com.alibaba.tesla.gateway.server.service.SwitchViewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Service
@Slf4j
public class SwitchViewServiceImpl implements SwitchViewService {
    private static final String HTTP = "http://";

    @Autowired
    private TeslaGatewayProperties gatewayProperties;

    private WebClient webClient;



    @PostConstruct
    private void init(){
        String authAddr = gatewayProperties.getAuthAddress();
        if (StringUtils.isNotBlank(authAddr) && !authAddr.startsWith(HTTP)) {
            authAddr = HTTP + authAddr;
        }
        webClient = WebClient.create(authAddr);
    }

    /**
     * 检查指定用户是否可以切换为目标用户
     *
     * @param empId       当前用户 EmpId
     * @param switchEmpId 目标用户 EmpId
     * @return
     */
    @Override
    public Mono<SwitchViewMapping> checkUser(String empId, String switchEmpId) {
        return webClient.get().uri(uriBuilder -> uriBuilder
                .path("/switchView/users/" + empId)
                .queryParam("switchEmpId", switchEmpId)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(httpStatus -> !httpStatus.equals(HttpStatus.OK), response -> {
                log.warn("request failed, code={}, msg={}", response.statusCode().value(), response.statusCode().getReasonPhrase());
                return Mono.error(new RuntimeException(response.statusCode().value() + ":" + response.statusCode().getReasonPhrase()));
            })
            .bodyToMono(JSONObject.class)
            .map(res -> {
                log.info("res={}", res.toJSONString());
                if(res.getInteger("code") == HttpStatus.OK.value()){
                    return JSONObject.parseObject(res.getJSONObject("data").toJSONString(), SwitchViewMapping.class);
                }
                throw new RuntimeException("requst auth proxy service failed, res=" + res.toJSONString());
            });
    }

    /**
     * 获取当前全量的白名单用户列表
     */
    @Override
    public Flux<SwitchViewUser> getWhiteListUsers() {
        return webClient.get().uri(uriBuilder -> uriBuilder.path("/switchView/users").build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(httpStatus -> !httpStatus.equals(HttpStatus.OK), response -> {
                log.warn("request failed, code={}, msg={}", response.statusCode().value(), response.statusCode().getReasonPhrase());
                return Mono.error(new RuntimeException(response.statusCode().value() + ":" + response.statusCode().getReasonPhrase()));
            })
            .bodyToMono(JSONObject.class)
            .map(res -> {
                if(null == res.get("code") || res.getInteger("code") != 200 || null == res.get("data")){
                    throw new GatewayException(String.format("authproxy response data is null, raw=%s", res.toJSONString()));
                }
                List<SwitchViewUser> users = new ArrayList<>();
                for (Object item : res.getJSONArray("data")) {
                    JSONObject data = JSONObject.parseObject(JSONObject.toJSONString(item));
                    users.add(SwitchViewUser.builder()
                        .empId(data.getString("empId"))
                        .loginName(data.getString("loginName"))
                        .bucId(data.getString("bucId"))
                        .build());
                }
                return users;
            }).flatMapMany(Flux::fromIterable);
    }
}
