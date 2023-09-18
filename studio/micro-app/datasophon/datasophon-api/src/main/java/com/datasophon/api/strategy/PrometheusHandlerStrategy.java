package com.datasophon.api.strategy;

import cn.hutool.http.HttpUtil;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;

import java.util.List;
import java.util.Map;

public class PrometheusHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handler(Integer clusterId, List<String> hosts) {

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        String url = "http://" + roleInstanceEntity.getHostname() + ":9090";
        try {
            HttpUtil.get(url);
            //recover alert
            ProcessUtils.recoverAlert(roleInstanceEntity);
        } catch (Exception e) {
            String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
            ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }
    }
}
