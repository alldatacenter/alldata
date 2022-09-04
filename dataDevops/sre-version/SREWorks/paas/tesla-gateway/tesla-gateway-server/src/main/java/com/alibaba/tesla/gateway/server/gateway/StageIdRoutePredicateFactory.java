package com.alibaba.tesla.gateway.server.gateway;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class StageIdRoutePredicateFactory extends AbstractRoutePredicateFactory<StageIdRoutePredicateFactory.Config> {

    private PathMatcher pathMatcher = new AntPathMatcher();

    private static final Log log = LogFactory.getLog(StageIdRoutePredicateFactory.class);

    public StageIdRoutePredicateFactory() {
        super(Config.class);
    }


    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("patterns");
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return new GatewayPredicate() {
            @Override
            public boolean test(ServerWebExchange exchange) {
                String bizApp = exchange.getRequest().getHeaders().getFirst("x-biz-app");
                if (StringUtils.isBlank(bizApp)) {
                    return false;
                }
                String[] values = bizApp.split(",");
                String stageId = values[values.length - 1];
                Optional<String> optionalPattern = config.getPatterns().stream()
                    .filter(pattern -> pathMatcher.match(pattern, stageId)).findFirst();
                return optionalPattern.isPresent();
            }

            @Override
            public String toString() {
                return String.format("StageId: %s", config.getPatterns());
            }
        };
    }

    @Validated
    public static class Config {

        private List<String> patterns = new ArrayList<>();

        private boolean matchTrailingSlash = true;

        public List<String> getPatterns() {
            return patterns;
        }

        public StageIdRoutePredicateFactory.Config setPatterns(List<String> patterns) {
            this.patterns = patterns;
            return this;
        }



    }

}
