package com.alibaba.tesla.gateway.server.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Configuration
public class CircuitBreakerConfiguration {

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    private void init() {
        //收集metric
        CircuitBreakerRegistry circuitBreakerRegistry =
            CircuitBreakerRegistry.ofDefaults();
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry)
            .bindTo(meterRegistry);
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .failureRateThreshold(50F)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(10)
                .minimumNumberOfCalls(100)
                .slidingWindowSize(500)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(300))
                .build())
            .build());
    }
}
