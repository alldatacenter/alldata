package com.alibaba.tesla.gateway.server.service;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;

import java.util.List;
import java.util.Map;

/**
 * 服务发现服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DiscoveryService {

    void refresh(List<String> serviceNames) throws NacosException;

    Map<String, String> getServicePaths();

    List<ServiceInfo> getSubscribeServices() throws NacosException;
}
