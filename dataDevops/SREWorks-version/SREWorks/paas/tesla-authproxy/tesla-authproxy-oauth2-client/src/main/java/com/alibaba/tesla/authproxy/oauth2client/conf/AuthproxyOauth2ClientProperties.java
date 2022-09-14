package com.alibaba.tesla.authproxy.oauth2client.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author cdx
 * @date 2020/1/7 14:38
 */
@Data
@Configuration
public class AuthproxyOauth2ClientProperties {

    @Value("${tesla.gateway.endpoint:}")
    private String teslaGatewayEndpoint;
}
