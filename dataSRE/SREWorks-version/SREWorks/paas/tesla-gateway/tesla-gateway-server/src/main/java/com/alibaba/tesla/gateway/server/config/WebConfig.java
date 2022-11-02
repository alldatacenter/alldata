package com.alibaba.tesla.gateway.server.config;

import com.alibaba.tesla.gateway.server.gateway.StageIdRoutePredicateFactory;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedMethods("*")
            .allowedHeaders("*");

    }


    @Bean
    @ConditionalOnEnabledPredicate
    public StageIdRoutePredicateFactory stageIdPredicateFactory() {
        return new StageIdRoutePredicateFactory();
    }
}
