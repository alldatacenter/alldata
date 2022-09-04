package com.alibaba.tesla.gateway.server.locator;

import com.alibaba.tesla.gateway.common.enums.RouteTypeEnum;
import com.alibaba.tesla.gateway.common.enums.ServerTypeEnum;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import com.alibaba.tesla.gateway.server.util.RouteMetaDataKeyConstants;
import com.alibaba.tesla.gateway.server.util.TeslaObjectConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class RouteDefinitionFactory {

    private static final String SEPARATOR = "/";

    private RouteDefinitionFactory() {
    }

    public static RouteDefinition to(RouteInfoDO routeInfo) {
        //todo
        //规范化标准创建
        RouteDefinition route = new RouteDefinition();
        route.setId(routeInfo.getRouteId());
        String url= routeInfo.getUrl();
        if(url.endsWith(SEPARATOR)) {
            url = url.substring(0, url.length() - 1);
        }

        //set predicate
        List<PredicateDefinition> predicateDefinitions = new ArrayList<>(8);

        RouteTypeEnum routeTypeEnum = RouteTypeEnum.toTypeEnum(routeInfo.getRouteType());
        switch (routeTypeEnum){
            case PATH:
                //path模式
                PredicateDefinition pathPredicate = TeslaRouteDefinitionFactory.toPathPredicate(routeInfo.getPath());
                if(pathPredicate != null){
                    predicateDefinitions.add(pathPredicate);
                }
                break;
            case HOST:
                //域名模式
                PredicateDefinition hostPredicate = TeslaRouteDefinitionFactory.toHostPredicate(routeInfo.getHost());
                if(hostPredicate != null){
                    predicateDefinitions.add(hostPredicate);
                }
                break;
            default:
                throw new IllegalArgumentException("not support the route type, routeType=" + routeTypeEnum.name());
        }

        PredicateDefinition forwardEnvPredicate = TeslaRouteDefinitionFactory.toForwardEnvPredicate(
            routeInfo.getForwardEnv());
        if(forwardEnvPredicate != null){
            predicateDefinitions.add(forwardEnvPredicate);
        }

        //stage id 处理
        if (StringUtils.isNotEmpty(routeInfo.getStageId())) {
            PredicateDefinition predicateDefinition = TeslaRouteDefinitionFactory.toStageIdPredicate(
                routeInfo.getStageId());
            predicateDefinitions.add(predicateDefinition);
        }

        if(!CollectionUtils.isEmpty(predicateDefinitions)){
            route.setPredicates(predicateDefinitions);
        }


        //set filter
        List<FilterDefinition> filterDefs = new ArrayList<>(8);
        String stripPrefixPath = null;
        if(StringUtils.isNotBlank(routeInfo.getPath())){
            FilterDefinition stripPrefixPathFilterDefinition = TeslaRouteDefinitionFactory.stripPrefixPath(routeInfo.getPath());
            stripPrefixPath = stripPrefixPathFilterDefinition.getArgs().get("_genkey_0");
            filterDefs.add(stripPrefixPathFilterDefinition);
        }
        FilterDefinition prefixPathFilterDef = TeslaRouteDefinitionFactory.toPrefixPath(url);
        if(null != prefixPathFilterDef){
            filterDefs.add(prefixPathFilterDef);
        }
        //熔断
        filterDefs.add(TeslaRouteDefinitionFactory.toCircuitBreaker(routeInfo.getRouteId()));

        //doc
        if (routeInfo.isEnableSwaggerDoc()) {
            //SwaggerHeaderFilter
            FilterDefinition swaggerHeaderFiler = TeslaRouteDefinitionFactory.swaggerHeaderFilter();
            filterDefs.add(swaggerHeaderFiler);
        }


        //rate limit
        FilterDefinition requestRateFilter = TeslaRequestRateFilterDefinitionFactory.buildRateLimitFilter(routeInfo);
        if(null != requestRateFilter){
            filterDefs.add(requestRateFilter);
        }
        route.setFilters(filterDefs);


        try {

            String[] values = url.split("://", 2);

            String realUrl = values[0] + "://" + values[1].split("/")[0];
            route.setUri(new URI(realUrl));
        } catch (URISyntaxException e) {
            log.error("url error",e);
            return null;
        }

        route.setOrder(routeInfo.getOrder());

        //set meta data
        //1、ignoreUri,  2、路由服务类型
        Map<String, Object> metaData = new HashMap<>();
        if (StringUtils.isNotBlank(routeInfo.getAuthIgnorePath())) {
            List<String> ignorePathList = Arrays.asList(routeInfo.getAuthIgnorePath().split(","));
            metaData.put(RouteMetaDataKeyConstants.AUTH_IGNORE_PATHS, ignorePathList);
        }

        metaData.put(RouteMetaDataKeyConstants.ROUTE_TYPE, routeTypeEnum.name());

        metaData.put(RouteMetaDataKeyConstants.ENABLED_DOC, routeInfo.isEnableSwaggerDoc());

        if (routeInfo.isEnableSwaggerDoc() && StringUtils.isNotBlank(routeInfo.getDocUri())) {
            metaData.put(RouteMetaDataKeyConstants.DOC_URI, routeInfo.getDocUri());
        }

        //auth direction
        if (routeInfo.getAuthRedirect() != null && routeInfo.getAuthRedirect().isEnabled()) {
            metaData.put(RouteMetaDataKeyConstants.AUTH_DIRECTION, TeslaObjectConvertUtil.copyObject(routeInfo.getAuthRedirect()));
        }


        //blacklist
        if (routeInfo.getBlackListConf() !=null
            && (!CollectionUtils.isEmpty(routeInfo.getBlackListConf().getIpBlackList())
                || !CollectionUtils.isEmpty(routeInfo.getBlackListConf().getUserBlackList())
                || !CollectionUtils.isEmpty(routeInfo.getBlackListConf().getAppBlackList()))) {
            metaData.put(RouteMetaDataKeyConstants.BLACK_LIST, TeslaObjectConvertUtil.copyObject(routeInfo.getBlackListConf()));
        }

        if (StringUtils.isNotBlank(routeInfo.getServerType())) {
            ServerTypeEnum serverTypeEnum = ServerTypeEnum.valueOf(routeInfo.getServerType());
            metaData.put(RouteMetaDataKeyConstants.SERVER_TYPE, serverTypeEnum.name());
        }

        if (StringUtils.isNotBlank(stripPrefixPath)) {
            metaData.put(RouteMetaDataKeyConstants.STRIP_PREFIX, stripPrefixPath);
        }

        route.setMetadata(metaData);

        return route;
    }
}
