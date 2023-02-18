package com.datasophon.api.strategy;

import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.*;

public class ZkServerHandlerStrategy implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ZkServerHandlerStrategy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        //保存zkUrls到全局变量
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        String join = String.join(":2181,", hosts);
        String zkUrls = join + ":2181";
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${zkUrls}", zkUrls);

    }



    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

        for (ServiceConfig config : list) {
            if("enableKerberos".equals(config.getName())){
                if( (Boolean)config.getValue()){
                    enableKerberos = true;
                    ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableZOOKEEPERKerberos}", "true");
                }else {
                    ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableZOOKEEPERKerberos}", "false");
                }
            }
        }

        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "ZOOKEEPER" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if(enableKerberos){
            for (ServiceConfig serviceConfig : configs) {
                if(serviceConfig.isConfigWithKerberos()){
                    if(map.containsKey(serviceConfig.getName())){
                        ServiceConfig config = map.get(serviceConfig.getName());
                        config.setRequired(true);
                        config.setHidden(false);
                        String value = PlaceholderUtils.replacePlaceholders((String) serviceConfig.getValue(), globalVariables, Constants.REGEX_VARIABLE);
                        logger.info("the value is {}" , value);
                        config.setValue(value);
                    }else{
                        serviceConfig.setRequired(true);
                        serviceConfig.setHidden(false);
                        String value = PlaceholderUtils.replacePlaceholders((String) serviceConfig.getValue(), globalVariables, Constants.REGEX_VARIABLE);
                        serviceConfig.setValue(value);
                        kbConfigs.add(serviceConfig);
                    }
                }
            }
        }else{
            for (ServiceConfig serviceConfig : configs) {
                if(serviceConfig.isConfigWithKerberos()){
                    if(map.containsKey(serviceConfig.getName())){
                        list.remove(map.get(serviceConfig.getName()));
                    }
                }
            }
        }
        list.addAll(kbConfigs);
    }
    /**
     *
     * @param clusterId
     * @param list
     */
    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        //add server.x config
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = (HashMap<String, List<String>>) CacheUtils.get(hostMapKey);

        if (Objects.nonNull(hostMap)) {
            List<String> zkServers = hostMap.get("ZkServer");
            HashMap<String, String> hostIpMap = (HashMap<String, String>) CacheUtils.get(Constants.HOST_IP);

            Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

            Integer myid =  1;
            for (String server : zkServers) {
                ServiceConfig serviceConfig = new ServiceConfig();
                serviceConfig.setName("server." + myid);
                serviceConfig.setLabel("server." + myid);
                serviceConfig.setValue(hostIpMap.get(server) + ":2888:3888");
                serviceConfig.setHidden(false);
                serviceConfig.setRequired(true);
                serviceConfig.setType("input");
                serviceConfig.setDefaultValue("");
                serviceConfig.setConfigType("zkserver");
                if (map.containsKey("server." + myid)) {
                    logger.info("set zk server {}", myid);
                    ServiceConfig config = map.get("server." + myid);
                    BeanUtils.copyProperties(serviceConfig,config);
                } else {
                    logger.info("add zk server.x config");
                    list.add(serviceConfig);
                }
                CacheUtils.put("zkserver_" + server, myid);
                myid++;
            }
        }
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {

    }
}
