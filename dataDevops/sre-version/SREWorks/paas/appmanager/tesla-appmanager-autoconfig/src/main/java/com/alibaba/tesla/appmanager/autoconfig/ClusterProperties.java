package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 包相关配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@ConfigurationProperties(prefix = "appmanager.cluster")
public class ClusterProperties {

    private Boolean local = false;
}
