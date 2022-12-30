package com.alibaba.tesla.gateway.repository;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class NacosRouteInfoRepository implements RouteInfoRepository {

    @Autowired
    private TeslaGatewayProperties gatewayProperties;

    @Override
    public List<RouteInfoDO> listAll() throws Exception {
        try {
            ConfigService configService = this.createConfigService();
            String jsonConfig = configService.getConfig(gatewayProperties.getStoreNacosDataId(),
                gatewayProperties.getStoreNacosGroup(), 10000);
            return StringUtils.isEmpty(jsonConfig) ? new ArrayList<>(8) : JSONObject.parseArray(
                jsonConfig, RouteInfoDO.class);
        }catch (NacosException e){
            throw new Exception(e);
        }

    }

    @Override
    public boolean saveAll(List<RouteInfoDO> routeInfoDOS) throws Exception {
        try {
            ConfigService configService = this.createConfigService();
            return configService.publishConfig(gatewayProperties.getStoreNacosDataId(), gatewayProperties.getStoreNacosGroup(),
                JSONObject.toJSONString(routeInfoDOS));
        }catch (NacosException e){
            throw new Exception(e);
        }

    }

    private ConfigService createConfigService() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, gatewayProperties.getStoreNacosAddr());
        properties.put(PropertyKeyConst.NAMESPACE, gatewayProperties.getStoreNacosNamespace());

        return NacosFactory.createConfigService(properties);
    }
}
