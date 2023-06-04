package com.datasophon.api.strategy;

import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ProcInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.StarRocksUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class FEHandlerStartegy implements ServiceRoleStrategy{

    private static final Logger logger = LoggerFactory.getLogger(FEHandlerStartegy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String,String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables"+ Constants.UNDERLINE+clusterId);
        if(hosts.size() >= 1){
            ProcessUtils.generateClusterVariable(globalVariables, clusterId,"${feMaster}",hosts.get(0));
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
        String feMaster = globalVariables.get("${feMaster}");
        if(hostname.equals(feMaster)){
            logger.info("fe master is {}",feMaster);
            serviceRoleInfo.setSortNum(1);
        }else{
            logger.info("set fe follower master");
            serviceRoleInfo.setMasterHost(feMaster);
            serviceRoleInfo.setSlave(true);
            serviceRoleInfo.setSortNum(2);
        }

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,Map<String, ClusterServiceRoleInstanceEntity> map) {
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + roleInstanceEntity.getClusterId());
        String feMaster = globalVariables.get("${feMaster}");
        if (roleInstanceEntity.getHostname().equals(feMaster) && roleInstanceEntity.getServiceRoleState() == ServiceRoleState.RUNNING) {
            try {
                List<ProcInfo> frontends = StarRocksUtils.showFrontends(feMaster);
                resolveProcInfoAlert("SRFE", frontends, map);
            } catch (Exception e) {

            }
            try {
                List<ProcInfo> backends = StarRocksUtils.showBackends(feMaster);
                resolveProcInfoAlert("SRBE", backends, map);
            } catch (Exception e) {

            }

        }
    }
    private void resolveProcInfoAlert(String serviceRoleName, List<ProcInfo> frontends, Map<String, ClusterServiceRoleInstanceEntity> map) {
        for (ProcInfo frontend : frontends) {
            ClusterServiceRoleInstanceEntity roleInstanceEntity = map.get(frontend.getHostName() + serviceRoleName);
//            ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceService.getServiceRoleInsByHostAndName(frontend.getHostName(), serviceRoleName);
            if (!frontend.getAlive()) {
                String alertTargetName = serviceRoleName + " Alive";
                logger.info("{} at host {} is not alive", serviceRoleName, frontend.getHostName());
                String alertAdvice = "the errmsg is " + frontend.getErrMsg();
                ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.WARN, alertAdvice);
            } else {
                ProcessUtils.recoverAlert(roleInstanceEntity);
            }
        }
    }
}
