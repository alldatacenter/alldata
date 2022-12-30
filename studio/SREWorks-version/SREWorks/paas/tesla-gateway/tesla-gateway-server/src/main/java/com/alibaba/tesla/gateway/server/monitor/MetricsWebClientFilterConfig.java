package com.alibaba.tesla.gateway.server.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Configuration
public class MetricsWebClientFilterConfig {

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    public MetricsWebClientFilterFunction webClientFilterFunction() {
        return new MetricsWebClientFilterFunction(
            meterRegistry, new DefaultWebClientExchangeTagsProvider(), "tesla.gateway.webClientMetrics",
            AutoTimer.ENABLED);
    }


}
