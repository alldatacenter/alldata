package com.datasophon.api.strategy;

import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.List;
import java.util.Map;

public interface ServiceRoleStrategy {
    void handler(Integer clusterId,List<String> hosts);

    void handlerConfig(Integer clusterId, List<ServiceConfig> list);

    void getConfig(Integer clusterId, List<ServiceConfig> list);

    void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo,String hostname);

    void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map);
}
