package com.alibaba.tesla.gateway.server.locator;

import com.alibaba.tesla.gateway.domain.res.RouteOperatorRes;
import com.alibaba.tesla.gateway.server.constants.RouteConst;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import com.alibaba.tesla.gateway.server.util.GatewayRouteCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class GatewayDynamicRouteLocator implements DynamicBaseRouteLocator {


    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Override
    public Mono<Boolean> insert(RouteInfoDO route) {
        //配置文件写死的路由不可以被修改
        if(inProperties(route.getRouteId())){
            return Mono.just(Boolean.FALSE);
        }
        Boolean valid = GatewayRouteCheckUtil.checkLegitimate(route);
        if(!valid){
            log.error("route info is invalid, insert route definitionWrite failed, routeInfo={}", route.toString());
            return Mono.just(Boolean.FALSE);
        }
        RouteDefinition routeDefinition = RouteDefinitionFactory.to(route);
        if(routeDefinition == null){
            return Mono.just(Boolean.FALSE);
        }
        return this.routeDefinitionWriter.save(Mono.just(
            routeDefinition)).onErrorContinue((e, o) -> {
                log.error("error, routeId={}", route.getRouteId());
        }).then(Mono.just(Boolean.TRUE));
    }

    @Override
    public Mono<RouteOperatorRes> delete(String routeId) {
        //配置文件写死的路由不可以被修改
        if(inProperties(routeId)){
            return Mono.just(RouteOperatorRes.builder()
                .success(Boolean.FALSE)
                .errorMsg("the routeId can not be delete. routeId=" + routeId)
                .build());
        }
        return  this.routeDefinitionWriter.delete(Mono.just(routeId))
            .then(Mono.defer(() -> Mono.just(
                RouteOperatorRes.builder()
                .success(Boolean.TRUE)
                .build()
            )));

    }

    /**
     * 判断是否在配置文件中写死，如果有不允许修改
     * @param routeId routeId
     * @return
     */
    private boolean inProperties(String routeId) {
        Optional<RouteDefinition> routeDefine = gatewayProperties.getRoutes().stream().filter(
            routeDefinition -> Objects.equals(routeDefinition.getId(), routeId)).findFirst();
        return routeDefine.isPresent();
    }

    @Override
    public Mono<RouteDefinition> get(String routeId) {
        return this.routeDefinitionLocator.getRouteDefinitions()
            .filter(route -> route.getId().equals(routeId)).singleOrEmpty();
    }

    @Override
    public Flux<RouteDefinition> getAll() {
        return this.routeDefinitionLocator.getRouteDefinitions();
    }
}
