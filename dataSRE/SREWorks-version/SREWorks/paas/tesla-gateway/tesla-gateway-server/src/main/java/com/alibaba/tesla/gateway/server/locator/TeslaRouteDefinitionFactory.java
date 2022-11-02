package com.alibaba.tesla.gateway.server.locator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaRouteDefinitionFactory {
    private static final int DEFAULT_LEN = 2;

    private TeslaRouteDefinitionFactory() {
    }

    public static FilterDefinition stripPrefixPath(String urlPrefix){
        if(!urlPrefix.startsWith("\\*")){
            urlPrefix = urlPrefix + "*";
        }
        int stripPrefix = urlPrefix.substring(0, urlPrefix.indexOf("*")).split("/").length - 1;
        FilterDefinition definition = new FilterDefinition();
        definition.setName("StripPrefix");
        Map<String, String> args = new HashMap<>(8);
        args.put("_genkey_0", String.valueOf(stripPrefix));
        definition.setArgs(args);
        return definition;
    }

    public static FilterDefinition toPrefixPath(String url){
        String[] values = url.split("://", 2)[1].split("/", 2);
        if(values.length == DEFAULT_LEN && StringUtils.isNotBlank(values[1])){
            String prefixPath = "/" + values[1];
            FilterDefinition definition = new FilterDefinition();
            definition.setName("PrefixPath");
            Map<String,String> map = new HashMap<>(8);
            map.put("_genkey_0", prefixPath);
            definition.setArgs(map);
            return definition;
        }
        return null;
    }


    public static FilterDefinition toCircuitBreaker(String routeId){
        FilterDefinition definition = new FilterDefinition();
        definition.setName("CircuitBreaker");
        Map<String,String> map = new HashMap<>(8);
        map.put("fallbackUri", "forward:http://127.0.0.1:7001/fallback");
        map.put("name", routeId);
        definition.setArgs(map);
        return definition;
    }


    public static PredicateDefinition toForwardEnvPredicate(String forwardEnv) {
        if(StringUtils.isBlank(forwardEnv)){
            return null;
        }
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Header");
        Map<String, String> args = new HashMap<>(8);
        args.put("_genkey_0", "X-ENV");
        args.put("_genkey_1", forwardEnv);
        predicate.setArgs(args);
        return predicate;
    }

    public static PredicateDefinition toStageIdPredicate(String stageId) {
        if(StringUtils.isBlank(stageId)){
            return null;
        }
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("StageId");
        Map<String, String> args = new HashMap<>(8);
        int index = 0;
        for (String id : stageId.split(",")) {
            args.put(String.format("_genkey_%s", index), id);
            index ++;
        }
        predicate.setArgs(args);
        return predicate;
    }

    public static PredicateDefinition toPathPredicate(String urlPrefix){
        if(StringUtils.isBlank(urlPrefix)){
            return null;
        }
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        Map<String, String> args = new HashMap<>(8);
        args.put("_genkey_0", urlPrefix);
        predicate.setArgs(args);
        return predicate;
    }

    public static PredicateDefinition toHostPredicate(String host) {
        if(StringUtils.isBlank(host)){
            return null;
        }
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Host");
        Map<String, String> args = new HashMap<>(8);
        args.put("_genkey_0", host);
        predicate.setArgs(args);
        return predicate;
    }

    public static FilterDefinition swaggerHeaderFilter() {
        FilterDefinition definition = new FilterDefinition();
        definition.setName("SwaggerHeaderFilter");
        return definition;
    }
}
