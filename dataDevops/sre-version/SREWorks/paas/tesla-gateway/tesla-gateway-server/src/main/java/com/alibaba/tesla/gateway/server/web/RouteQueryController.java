package com.alibaba.tesla.gateway.server.web;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.gateway.api.GatewayRouteConfigService;
import com.alibaba.tesla.gateway.common.exception.OperatorRouteException;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.server.domain.RouteConfig;
import com.alibaba.tesla.gateway.server.locator.DynamicBaseRouteLocator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 路由配置管理请求处理
 * 获取所有api的路由信息
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/admin/route")
@RestController
public class RouteQueryController {

    @Autowired
    private DynamicBaseRouteLocator dynamicBaseRouteLocator;

    @Autowired
    private GatewayRouteConfigService routeConfigService;


    @GetMapping("/gateway")
    public Flux<RouteDefinition> getGatewayRoutes() {
        return this.dynamicBaseRouteLocator.getAll();
    }

    @GetMapping("/all")
    @ResponseBody
    public TeslaBaseResult getAll() throws OperatorRouteException {
        List<RouteInfoDTO> all = this.routeConfigService.getAll();
        RouteConfig routeConfig = RouteConfig.builder()
            .routes(all)
            .build();
        return TeslaResultFactory.buildSucceedResult(routeConfig);
    }

}
