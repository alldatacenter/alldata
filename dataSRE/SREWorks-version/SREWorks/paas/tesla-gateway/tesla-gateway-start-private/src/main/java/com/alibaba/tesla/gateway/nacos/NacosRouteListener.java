package com.alibaba.tesla.gateway.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.event.RouteRefreshEvent;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * todo
 * 使用spring el
 * @author tandong.td@alibaba-inc.com
 */
@Slf4j
@Component
public class NacosRouteListener {

    private List<RouteInfoDO> routeInfoDOS;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TeslaGatewayProperties teslaGatewayProperties;

    private ConfigService configService;


    @PostConstruct
    private void init() throws Exception {
        try {
            Properties properties = new Properties();
            log.info("nacosAddr={}, namespace={}, dataId={}, group={}", teslaGatewayProperties.getStoreNacosAddr(),
                teslaGatewayProperties.getStoreNacosNamespace(), teslaGatewayProperties.getStoreNacosDataId(),
                teslaGatewayProperties.getStoreNacosGroup());

            properties.put(PropertyKeyConst.SERVER_ADDR, teslaGatewayProperties.getStoreNacosAddr());
            properties.put(PropertyKeyConst.NAMESPACE, teslaGatewayProperties.getStoreNacosNamespace());

            configService = NacosFactory.createConfigService(properties);


            //先加载一遍
            this.firstLoadRouteConfig();

            //路由信息
            configService.addListener(teslaGatewayProperties.getStoreNacosDataId(),
                teslaGatewayProperties.getStoreNacosGroup(),
                new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        List<RouteInfoDO> routeInfo = JSON.parseArray(configInfo, RouteInfoDO.class);

                        log.info(String.format("Get config from nacos, routeSize=%s", routeInfo.size()));
                        //不变化则不刷新路由
                        if (!Objects.equals(routeInfoDOS, routeInfo)) {
                            log.info("### publishRouteFreshEvent ###");
                            routeInfoDOS = routeInfo;
                            applicationEventPublisher.publishEvent(new RouteRefreshEvent(routeInfo));
                        }
                    }
                });
        }catch (Exception e){
            log.error("init route failed,", e);
            throw e;
        }

    }

    /**
     * 初始化加载配置
     * @throws NacosException exception
     */
    private void firstLoadRouteConfig() throws NacosException {
        String config = this.configService.getConfig(teslaGatewayProperties.getStoreNacosDataId(),
            teslaGatewayProperties.getStoreNacosGroup(), 10000);
        log.info("routeConfig={}", config);
        if (StringUtils.isBlank(config)) {
            config = "[]";
        }
        //如果未空创建一个空array
        //拉取系统级别的配置
        //当前只允许系统级别路由
        if (this.teslaGatewayProperties.isReloadConfig()) {
            String systemConfig = this.configService.getConfig(teslaGatewayProperties.getDefaultRouteDataId(),
                teslaGatewayProperties.getDefaultRouteGroup(), 1000);
            log.info("systemRouteConfig={}", systemConfig);
            this.configService.publishConfig(teslaGatewayProperties.getStoreNacosDataId(), teslaGatewayProperties.getStoreNacosGroup(),
                systemConfig);
            config = systemConfig;
        }
        List<RouteInfoDO> routeInfoDOS = JSON.parseArray(config, RouteInfoDO.class);
        this.applicationEventPublisher.publishEvent(new RouteRefreshEvent(routeInfoDOS));
    }

    private String mergeRoute(JSONArray configRoutes, JSONArray systemConfigRoutes) {
        //系统级别的路由强行覆盖
        Map<String, JSONObject> routesMap = new HashMap<>(configRoutes.size() + systemConfigRoutes.size());
        for (Object configRoute : configRoutes) {
            JSONObject route = (JSONObject) configRoute;
            routesMap.put(route.getString("routeId"), route);
        }

        for (Object systemConfigRoute : systemConfigRoutes) {
            JSONObject route = (JSONObject) systemConfigRoute;
            routesMap.put(route.getString("routeId"), route);
        }
        return JSONObject.toJSONString(new ArrayList<>(routesMap.values()));
    }

}
