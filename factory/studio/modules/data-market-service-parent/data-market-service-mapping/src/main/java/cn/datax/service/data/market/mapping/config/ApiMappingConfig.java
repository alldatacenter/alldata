package cn.datax.service.data.market.mapping.config;

import cn.datax.service.data.market.mapping.handler.MappingHandlerMapping;
import cn.datax.service.data.market.mapping.handler.RequestHandler;
import cn.datax.service.data.market.mapping.handler.RequestInterceptor;
import cn.datax.service.data.market.mapping.service.impl.ApiMappingEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class ApiMappingConfig {

    @Bean
    public MappingHandlerMapping mappingHandlerMapping(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                                       ApiMappingEngine apiMappingEngine,
                                                       RedisTemplate redisTemplate,
                                                       ObjectMapper objectMapper) {
        MappingHandlerMapping mappingHandlerMapping = new MappingHandlerMapping();
        mappingHandlerMapping.setHandler(requestHandler(apiMappingEngine, redisTemplate, objectMapper));
        mappingHandlerMapping.setRequestMappingHandlerMapping(requestMappingHandlerMapping);
        return mappingHandlerMapping;
    }

    @Bean
    public RequestHandler requestHandler(ApiMappingEngine apiMappingEngine, RedisTemplate redisTemplate, ObjectMapper objectMapper) {
        RequestHandler handler = new RequestHandler();
        handler.setApiMappingEngine(apiMappingEngine);
        handler.setObjectMapper(objectMapper);
        handler.setRequestInterceptor(new RequestInterceptor(redisTemplate));
        return handler;
    }
}
