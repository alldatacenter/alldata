package com.alibaba.tesla.gateway.server.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Configuration
@EnableConfigurationProperties(value = {TeslaGatewayProperties.class})
public class TeslaGatewayPropertiesConfiguration {

}
