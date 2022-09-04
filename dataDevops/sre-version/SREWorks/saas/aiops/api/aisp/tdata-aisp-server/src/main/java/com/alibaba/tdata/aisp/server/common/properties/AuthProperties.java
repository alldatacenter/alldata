package com.alibaba.tdata.aisp.server.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: AuthorizationProperties
 * @Author: dyj
 * @DATE: 2021-12-13
 * @Description:
 **/
@Component
@Data
@ConfigurationProperties(prefix = "aisp.authorization")
public class AuthProperties {
    private String url;
    private String user;
    private String password;
    private String clientId;
    private String clientSecret;
}
