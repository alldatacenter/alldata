package com.datasophon.api.strategy;

import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.*;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RangerAdminHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RangerAdminHandlerStrategy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        if (hosts.size() == 1) {
            String rangerAdminUrl = "http://" + hosts.get(0) + ":6080";
            logger.info("rangerAdminUrl is {}", rangerAdminUrl);
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${rangerAdminUrl}", rangerAdminUrl);
        }
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
        //enable ranger plugin
        for (ServiceConfig config : list) {
            if ("enableHDFSPlugin".equals(config.getName()) && ((Boolean) config.getValue()).booleanValue()) {
                logger.info("enableHdfsPlugin");
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableHDFSPlugin}", "true");
                enableRangerPlugin(clusterId, "HDFS", "NameNode");
            }
            if ("enableHIVEPlugin".equals(config.getName()) && ((Boolean) config.getValue()).booleanValue()) {
                logger.info("enableHivePlugin");
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableHIVEPlugin}", "true");
                enableRangerPlugin(clusterId, "HIVE", "HiveServer2");
            }
            if ("enableHBASEPlugin".equals(config.getName()) && ((Boolean) config.getValue()).booleanValue()) {
                logger.info("enableHbasePlugin");
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableHBASEPlugin}", "true");
                enableRangerPlugin(clusterId, "HBASE", "HbaseMaster");
            }
            if (config.getName().contains("Plugin") && !(Boolean) config.getValue()) {
                String configName = config.getName();
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${" + configName + "}", "false");
            }
            if ("enableKerberos".equals(config.getName())) {
                enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config, "RANGER");
            }
        }
        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "RANGER" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableKerberos) {
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
        } else {
            removeConfigWithKerberos(list, map, configs);
        }
        list.addAll(kbConfigs);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {

    }

    private void enableRangerPlugin(Integer clusterId, String serviceName, String serviceRoleName) {
        ClusterServiceInstanceService serviceInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
        ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterServiceRoleGroupConfigService roleGroupConfigService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleGroupConfigService.class);
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ServiceInstallService serviceInstallService = SpringTool.getApplicationContext().getBean(ServiceInstallService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getServiceInstanceByClusterIdAndServiceName(clusterId, serviceName);
        //查询角色组id
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService.getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, serviceRoleName);

        if (Objects.nonNull(roleList) && roleList.size() > 0) {
            Integer roleGroupId = roleList.get(0).getRoleGroupId();

            ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
            List<ServiceConfig> serviceConfigs = JSONObject.parseArray(config.getConfigJson(), ServiceConfig.class);
            Map<String, ServiceConfig> map = serviceConfigs.stream().collect(Collectors.toMap(ServiceConfig::getName, serviceConfig -> serviceConfig, (v1, v2) -> v1));

            String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + serviceName + Constants.CONFIG;
            List<ServiceConfig> configs = ServiceConfigMap.get(key);
            for (ServiceConfig parameter : configs) {
                String name = parameter.getName();
                if (map.containsKey(name)) {
                    parameter = map.get(name);
                }

                if ("permission".equals(parameter.getConfigType())) {
                    parameter.setHidden(false);
                    parameter.setRequired(true);
                }
                if ("dfs.permissions.enabled".equals(parameter.getName())) {
                    parameter.setHidden(false);
                    parameter.setRequired(true);
                    parameter.setValue(true);

                }
                if ("rangerAdminUrl".equals(parameter.getName())) {
                    parameter.setHidden(false);
                    parameter.setRequired(true);
                    parameter.setValue(globalVariables.get("${rangerAdminUrl}"));
                }
                if (!map.containsKey(name)) {
                    logger.info("put config {} into service {}", name, serviceRoleName);
                    serviceConfigs.add(parameter);
                }
            }
            serviceInstallService.saveServiceConfig(clusterId, serviceInstance.getServiceName(), serviceConfigs, roleGroupId);
        }
    }
}
