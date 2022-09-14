package com.alibaba.tesla.gateway.server.handler;

import com.alibaba.tesla.gateway.server.util.TeslaHostUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import springfox.documentation.swagger.web.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "tesla.config.gateway.enable-doc", havingValue = "true", matchIfMissing = true)
public class SwaggerHandler {

    private static final List<String> teslaHosts = Arrays.asList("tesla.alibaba-inc.com", "tesladaily.alibaba-inc.com", "tesla-pre.alibaba-inc.com");

    @Autowired(required = false)
    private SecurityConfiguration securityConfiguration;

    @Autowired(required = false)
    private UiConfiguration uiConfiguration;

    private final SwaggerResourcesProvider swaggerResources;

    @Autowired
    public SwaggerHandler(SwaggerResourcesProvider swaggerResources) {
        this.swaggerResources = swaggerResources;
    }


    @GetMapping("/swagger-resources/configuration/security")
    public Mono<ResponseEntity<SecurityConfiguration>> securityConfiguration() {
        return Mono.just(new ResponseEntity<>(
            Optional.ofNullable(securityConfiguration).orElse(SecurityConfigurationBuilder.builder().build()), HttpStatus.OK));
    }

    @GetMapping("/swagger-resources/configuration/ui")
    public Mono<ResponseEntity<UiConfiguration>> uiConfiguration() {
        return Mono.just(new ResponseEntity<>(
            Optional.ofNullable(uiConfiguration).orElse(UiConfigurationBuilder.builder().build()), HttpStatus.OK));
    }

    @GetMapping("/swagger-resources")
    public Mono<ResponseEntity> swaggerResources(ServerWebExchange webExchange) {
        String host = webExchange.getRequest().getHeaders().getFirst("Host");
        List<SwaggerResource> swaggerResources = this.swaggerResources.get();
        //使用tesla域名
        if (TeslaHostUtil.inTeslaHost(host)) {
            for (SwaggerResource resource : swaggerResources) {
                resource.setLocation("/gateway" + resource.getLocation());
                resource.setUrl("/gateway" + resource.getUrl());
            }
        }
        return Mono.just((new ResponseEntity<>(this.swaggerResources.get(), HttpStatus.OK)));
    }
}
