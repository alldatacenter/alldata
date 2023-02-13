package com.alibaba.tesla.gateway.server.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class TeslaNacosServerList extends AbstractServerList<NacosServer> {

    private NacosDiscoveryProperties discoveryProperties;

    private String serviceId;

    private Map<String, List<Instance>> instanceCache = new ConcurrentHashMap<>(256);


    public TeslaNacosServerList(NacosDiscoveryProperties discoveryProperties) {
        this.discoveryProperties = discoveryProperties;
    }

    @Override
    public List<NacosServer> getInitialListOfServers() {
        return getServers();
    }

    @Override
    public List<NacosServer> getUpdatedListOfServers() {
        return getServers();
    }

    private List<NacosServer> getServers() {
        try {
            List<Instance> instances = discoveryProperties.namingServiceInstance()
                .selectInstances(serviceId, true);
            instances = instances.stream().filter(Instance::isHealthy).collect(Collectors.toList());
            if(log.isDebugEnabled()){
                log.debug("get server instance list, serverId={}, instances={}", serviceId, JSONObject.toJSONString(instances));
            }
            if(CollectionUtils.isEmpty(instances)){
                log.warn("get server instance list from nacos, instance list is empty, serverId=" + serviceId);
                instances = this.instanceCache.get(serviceId);
            }else {
                this.instanceCache.put(serviceId, instances);
            }
            return instancesToServerList(instances);
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Can not get service instances from nacos, serviceId=" + serviceId,
                e);
        }
    }

    private List<NacosServer> instancesToServerList(List<Instance> instances) {
        List<NacosServer> result = new ArrayList<>();
        if (null == instances) {
            return result;
        }
        for (Instance instance : instances) {
            result.add(new NacosServer(instance));
        }

        return result;
    }

    public String getServiceId() {
        return serviceId;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig iClientConfig) {
        this.serviceId = iClientConfig.getClientName();
    }
}
