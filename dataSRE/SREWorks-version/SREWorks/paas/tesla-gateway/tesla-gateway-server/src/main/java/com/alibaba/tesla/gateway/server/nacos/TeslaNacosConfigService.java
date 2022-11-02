package com.alibaba.tesla.gateway.server.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
//@Component
public class TeslaNacosConfigService {

    @Autowired
    private TeslaGatewayProperties gatewayProperties;


    @PostConstruct
    private void init() {

        Thread thread = new Thread(() -> {
            Properties properties = new Properties();
            properties.put("namespace", gatewayProperties.getStoreNacosNamespace());
            properties.put("serverAddr", gatewayProperties.getStoreNacosAddr());

            ConfigService configService = null;
            try {
                configService = NacosFactory.createConfigService(properties);
                configService.addListener(gatewayProperties.getStoreDiamondDataId(), gatewayProperties.getStoreNacosGroup()
                    , new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return null;
                        }

                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            log.info("nacos-data={}", configInfo);
                        }
                    });
            } catch (NacosException e) {
                log.error("listen nacos faild", e);
            }
            log.info("listenNacosConfig||status=success||dataId={}||group={}", gatewayProperties.getStoreNacosDataId(),
                gatewayProperties.getStoreNacosGroup());

        });
        thread.setDaemon(true);
        thread.setName("tesla-nacos-config-listen");
        thread.start();

    }
}
