package com.alibaba.tesla.gateway.server.nacos;

import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ServerList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class TeslaNacosRibbonClientConfiguration {

    @SuppressWarnings("all")
    @Bean
    @ConditionalOnNacosDiscoveryEnabled
    public ServerList<?> ribbonServerList(IClientConfig config, NacosDiscoveryProperties nacosDiscoveryProperties) {
        log.info("start config tesla nacos riboon server, serverId=" + config.getClientName());
        TeslaNacosServerList serverList = new TeslaNacosServerList(nacosDiscoveryProperties);
        serverList.initWithNiwsConfig(config);
        log.info("start config tesla nacos riboon server success, serverId=" + config.getClientName());
        return serverList;
    }
}
