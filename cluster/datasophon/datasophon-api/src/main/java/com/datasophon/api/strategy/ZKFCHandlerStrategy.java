package com.datasophon.api.strategy;

import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ZKFCHandlerStrategy implements ServiceRoleStrategy{

    private static final Logger logger = LoggerFactory.getLogger(ZKFCHandlerStrategy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String,String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables"+ Constants.UNDERLINE+clusterId);
        if(hosts.size() == 2 ){
            ProcessUtils.generateClusterVariable(globalVariables, clusterId,"${ZKFC1}",hosts.get(0));
            ProcessUtils.generateClusterVariable(globalVariables, clusterId,"${ZKFC2}",hosts.get(1));
        }
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + serviceRoleInfo.getClusterId());
        if(hostname.equals(globalVariables.get("${ZKFC2}"))){
            logger.info("set to slave zkfc");
            serviceRoleInfo.setSlave(true);
            serviceRoleInfo.setSortNum(6);
        }
    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {

    }
}
