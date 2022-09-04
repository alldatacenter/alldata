package com.alibaba.tesla.gateway.server.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class SwaggerUiConfigurer implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            //classpath:/META-INF/resources
            .addResourceLocations("classpath:/META-INF/resources/")
            .resourceChain(false);
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
            .resourceChain(false);
    }
}
