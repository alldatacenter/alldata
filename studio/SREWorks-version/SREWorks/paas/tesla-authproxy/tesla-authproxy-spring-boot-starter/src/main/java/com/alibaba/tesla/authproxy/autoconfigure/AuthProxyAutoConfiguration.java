package com.alibaba.tesla.authproxy.autoconfigure;

import com.alibaba.tesla.authproxy.AuthProxyClientProperties;
import com.alibaba.tesla.authproxy.api.AuthProxyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@ConditionalOnClass(AuthProxyClient.class)
@EnableConfigurationProperties(AuthProxyClientProperties.class)
public class AuthProxyAutoConfiguration {

    @Autowired
    private AuthProxyClientProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public AuthProxyClient authProxyClient() {
        String endpoint = properties.getEndpoint();
        String appId = properties.getAppId();
        Assert.notNull(endpoint, "tesla.authproxy.endpoint must be set");
        Assert.notNull(appId, "tesla.authproxy.app-id must be set");
        return new AuthProxyClient(endpoint);
    }

}
