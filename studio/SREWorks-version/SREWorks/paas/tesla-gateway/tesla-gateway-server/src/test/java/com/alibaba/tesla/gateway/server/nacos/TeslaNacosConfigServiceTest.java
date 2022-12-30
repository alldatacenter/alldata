package com.alibaba.tesla.gateway.server.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaNacosConfigServiceTest {

    @Test
    public void publish() throws NacosException {
        // 初始化配置服务，控制台通过示例代码自动获取下面参数
        String serverAddr = "http://nacos-daily.tesla.alibaba-inc.com:80";
        String dataId = "tesla.gateway.route.config";
        String group = "DEFAULT_GROUP";
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", "5d6df58b-79b3-4c18-ac5a-94cad555c906");
        ConfigService configService = NacosFactory.createConfigService(properties);
        boolean isPublishOk = configService.publishConfig(dataId, group, "content");
        System.out.println(isPublishOk);
    }

}