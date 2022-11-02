package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@ConfigurationProperties(prefix = "tesla.app.nacos")
public class AppNacosProperties {

    private String agentNamespace;

    private String serverAddr;

    private String agentGroup = "DEFAULT_GROUP";

    private String agentServerName;
}
