package com.alibaba.tesla.gateway.server.web.admin;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.gateway.api.GatewayManagerConfigService;
import com.alibaba.tesla.gateway.api.GatewayRouteConfigService;
import com.alibaba.tesla.gateway.common.enums.ServerTypeEnum;
import com.alibaba.tesla.gateway.common.exception.OperatorRouteException;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.domain.req.*;
import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import com.alibaba.tesla.gateway.server.locator.DynamicBaseRouteLocator;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import com.alibaba.tesla.gateway.server.service.SwitchViewService;
import com.alibaba.tesla.web.util.HttpHeaderResolveUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.*;


/**
 * 切换视角请求处理，获取请求者的用户user\emp_ID,切换后的user\emp_ID
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
@RestControllerEndpoint(id = "admin")
public class RouteAdminController {

    private static final String SWITCH_VIEW_COOKIE_NAME = "tesla_switch_view";

    private static final int SWITCH_VIEW_COOKIE_EXPIRES = 86400;

    private static final Integer UNLIMITED_PAGE_SIZE = 100000;

    @Autowired
    private DynamicBaseRouteLocator dynamicBaseRouteLocator;

    @Autowired
    private GatewayRouteConfigService routeConfigService;

    @Autowired
    private GatewayCache gatewayCache;

    @Autowired
    private SwitchViewService switchViewService;

    @Autowired(required = false)
    private NacosDiscoveryProperties discoveryProperties;

    @Value("${tesla.cookie-domain:alibaba-inc.com}")
    private String cookieDomain;


    @Autowired
    private GatewayManagerConfigService gatewayManagerConfigService;

    @GetMapping("/route/gateway")
    public Flux<RouteDefinition> getZuulRoutes() {
        return this.dynamicBaseRouteLocator.getAll();
    }

    @GetMapping("/route/all")
    @ResponseBody
    public TeslaBaseResult getAll() throws OperatorRouteException {
        return TeslaResultFactory.buildSucceedResult(this.routeConfigService.getAll());
    }

    @GetMapping
    @ResponseBody
    public TeslaBaseResult get(@RequestParam String appId) throws OperatorRouteException {
        return TeslaResultFactory.buildSucceedResult(this.routeConfigService.getByAppId(appId));
    }

    @GetMapping("/route/{routeId}")
    public TeslaBaseResult queryByRouteId(@PathVariable("routeId") String routeId) throws OperatorRouteException {
        return TeslaResultFactory.buildSucceedResult(this.routeConfigService.getByRouteId(routeId));
    }

    @GetMapping("/route/list")
    @ResponseBody
    public TeslaBaseResult queryByCondition(@Valid @ModelAttribute RouteInfoQueryReq queryReq, ServerHttpRequest httpRequest)
        throws OperatorRouteException {
        if(StringUtils.isEmpty(queryReq.getAppId())){
            String bizAppId = HttpHeaderResolveUtil.getBizAppId(httpRequest);
            if(StringUtils.isEmpty(bizAppId) && !Objects.equals(queryReq.getServerType(), ServerTypeEnum.PAAS.name())){
                throw new IllegalArgumentException("appId not be empty");
            }
            queryReq.setAppId(bizAppId);
        }

        List<RouteInfoDTO> routes = this.routeConfigService.getByCondition(queryReq);
        return TeslaResultFactory.buildSucceedResult(routes);
    }

    @GetMapping("/route/functionServers")
    public TeslaBaseResult listFunctionServer(@ModelAttribute FunctionServerReq req,  ServerHttpRequest httpRequest) throws OperatorRouteException {
        if (StringUtils.isBlank(req.getAppId()) && StringUtils.isNotBlank(HttpHeaderResolveUtil.getBizAppId(httpRequest))) {
            req.setAppId(HttpHeaderResolveUtil.getBizAppId(httpRequest));
        }
        return TeslaResultFactory.buildSucceedResult(this.routeConfigService.listFunctionServer(req));
    }

    @PutMapping("/route/{routeId}")
    @ResponseBody
    public TeslaBaseResult update(@Valid @RequestBody RouteInfoDTO routeInfoDTO, @PathVariable("routeId") String routeId,
                                  ServerHttpRequest httpRequest) throws OperatorRouteException {
        routeInfoDTO.setRouteId(routeId);
        String appId = HttpHeaderResolveUtil.getBizAppId(httpRequest);
        if(StringUtils.isEmpty(routeInfoDTO.getAppId()) && StringUtils.isNotBlank(appId)){
            routeInfoDTO.setAppId(appId);
        }
        if (!Objects.equals(routeId, routeInfoDTO.getRouteId())) {
            throw new IllegalArgumentException("routeId not equals to path param");
        }
        this.routeConfigService.updateRoute(routeInfoDTO);
        return TeslaResultFactory.buildSucceedResult();

    }

    @DeleteMapping("/route/{routeId}")
    @ResponseBody
    public TeslaBaseResult deleteRoute(@PathVariable("routeId") String routeId) throws OperatorRouteException {
        log.info("delete route||routeId={}", routeId);
        this.routeConfigService.deleteRoute(routeId);
        return TeslaResultFactory.buildSucceedResult();
    }

    @PostMapping("/route")
    @ResponseBody
    public TeslaBaseResult create(@Valid @RequestBody RouteInfoDTO routeInfoDTO, ServerHttpRequest httpRequest) throws OperatorRouteException {
        if(StringUtils.isBlank(routeInfoDTO.getAppId())){
            String appId = HttpHeaderResolveUtil.getBizAppId(httpRequest);
            if(StringUtils.isBlank(appId)){
                throw new IllegalArgumentException("appId can not be null");
            }
            routeInfoDTO.setAppId(appId);
        }
        this.routeConfigService.insertRoute(routeInfoDTO);
        return TeslaResultFactory.buildSucceedResult();
    }

    @PostMapping("/route/import")
    @ResponseBody
    public TeslaBaseResult batchImport(@RequestBody RouteInfoDTO[] routeInfoDTOS) throws Exception {
        List<RouteInfoDTO> infoDTOS = Arrays.asList(routeInfoDTOS);
        this.routeConfigService.batchImportRoute(infoDTOS);
        return TeslaResultFactory.buildSucceedResult();
    }

    @GetMapping("/cache/route")
    public TeslaBaseResult getRouteCache(){
        Map<String, Route> routes = gatewayCache.getRoutes();
        return TeslaResultFactory.buildSucceedResult(routes);
    }

    @GetMapping("/cache/routeInfo")
    public TeslaBaseResult getRouteInfoCache(){
        Map<String, RouteInfoDO> routeInfos = this.gatewayCache.getRouteInfos();
        return TeslaResultFactory.buildSucceedResult(routeInfos);
    }



    /**
     * 获取请求中的用户信息参数、并判断是否在白名单内，是的话直接生成cookie返回
     */
    @GetMapping("view/switch")
    @ResponseBody
    public Mono<TeslaBaseResult> switchCookie(@Valid @ModelAttribute SwitchViewRequest param,
                                              ServerHttpRequest request,
                                              ServerHttpResponse response) {
        String empId = request.getHeaders().getFirst("X-EmpId");
        if (StringUtils.isEmpty(empId)) {
            return Mono.just(TeslaResultFactory.buildValidationErrorResult("X-EmpId", "required header"));
        }

        String switchEmpId = param.getSwitchEmpId();
        return switchViewService.checkUser(empId, switchEmpId)
            .map(user -> {
                String cookieValue = Base64Utils.encodeToString(TeslaGsonUtil.toJson(user).getBytes());
                return ResponseCookie.from(SWITCH_VIEW_COOKIE_NAME, cookieValue)
                    .path("/")
                    .domain(cookieDomain)
                    .maxAge(SWITCH_VIEW_COOKIE_EXPIRES)
                    .httpOnly(false)
                    .build();

            }).doOnNext(cookie -> response.getCookies().set(SWITCH_VIEW_COOKIE_NAME, cookie))
            .thenReturn(TeslaResultFactory.buildSucceedResult());
    }


    /**
     * 关闭切换逻辑，删除对应的 Cookie
     */
    @GetMapping("view/switchBack")
    @ResponseBody
    public Mono<TeslaBaseResult> switchBackCookie(ServerHttpRequest request, ServerHttpResponse response) {
        if(request.getCookies().containsKey(SWITCH_VIEW_COOKIE_NAME)){
            ResponseCookie resCookie = ResponseCookie.from(SWITCH_VIEW_COOKIE_NAME, StringUtils.EMPTY)
                .domain(cookieDomain)
                .maxAge(0L)
                .path("/")
                .build();
            response.addCookie(resCookie);
        }

        return Mono.just(TeslaResultFactory.buildSucceedResult());
    }

    @GetMapping(value = "/discovery/services")
    @ResponseBody
    public TeslaBaseResult services() throws NacosException {
        if (discoveryProperties == null) {
            return TeslaResultFactory.buildSucceedResult();
        }

        NamingService namingService = discoveryProperties.namingServiceInstance();
        List<DiscoveryServiceInstances> data = new Vector<>();
        namingService.getServicesOfServer(1, UNLIMITED_PAGE_SIZE).getData().parallelStream().forEach(serviceName -> {
            List<Instance> instances;
            try {
                instances = namingService.selectInstances(serviceName, true);
            } catch (NacosException e) {
                log.error("Cannot get all instances with service name {}", serviceName);
                return;
            }
            if (!CollectionUtils.isEmpty(instances)) {
                data.add(DiscoveryServiceInstances.builder()
                    .serviceName(serviceName)
                    .instances(instances)
                    .build());
            }
        });
        return TeslaResultFactory.buildSucceedResult(data);
    }
    @Data
    @Builder
    private static class DiscoveryServiceInstances {

        private String serviceName;
        private List<Instance> instances;
    }


    @ResponseBody
    @PostMapping("/docAdminUser/{empId}")
    public TeslaBaseResult addDocAdminUser(@PathVariable("empId") String empId) {
        return TeslaResultFactory.buildSucceedResult(this.gatewayManagerConfigService.addDocAdminUser(empId));
    }

    @ResponseBody
    @DeleteMapping("/docAdminUser/{empId}")
    public TeslaBaseResult removeDocAdminUser(@PathVariable("empId") String empId) {
        return TeslaResultFactory.buildSucceedResult(this.gatewayManagerConfigService.removeDocAdminUser(empId));
    }


    @ResponseBody
    @GetMapping("/docAdminUser")
    public TeslaBaseResult listAdminUsers() {
        return TeslaResultFactory.buildSucceedResult(this.gatewayManagerConfigService.listDocAdminUsers());
    }

}
