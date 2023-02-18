package com.datasophon.api.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.service.*;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.common.model.*;
import com.datasophon.dao.entity.*;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service("serviceInstallService")
@Transactional
public class ServiceInstallServiceImpl implements ServiceInstallService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallServiceImpl.class);

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    FrameInfoService frameInfoService;

    @Autowired
    FrameServiceService frameService;

    @Autowired
    ClusterServiceCommandService commandService;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterServiceInstanceConfigService serviceInstanceConfigService;

    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;

    @Autowired
    private ClusterVariableService variableService;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private ClusterServiceRoleGroupConfigService groupConfigService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;


    @Override
    public Result getServiceConfigOption(Integer clusterId, String serviceName) {
        List<ServiceConfig> list = null;
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        //查询服务实例是否存在
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getServiceInstanceByClusterIdAndServiceName(clusterId, serviceName);
        if (Objects.nonNull(serviceInstance)) {
            ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getRoleGroupByServiceInstanceId(serviceInstance.getId());
            ClusterServiceRoleGroupConfig config = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
            String serviceConfig = PlaceholderUtils.replacePlaceholders(config.getConfigJson(), globalVariables, Constants.REGEX_VARIABLE);
            list = JSONArray.parseArray(serviceConfig, ServiceConfig.class);
        } else {
            FrameServiceEntity frameService = this.frameService.getServiceByFrameCodeAndServiceName(clusterInfo.getClusterFrame(), serviceName);
            String serviceConfig = frameService.getServiceConfig();
            serviceConfig = PlaceholderUtils.replacePlaceholders(serviceConfig, globalVariables, Constants.REGEX_VARIABLE);

            list = JSONArray.parseArray(serviceConfig, ServiceConfig.class);
        }

        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (Objects.nonNull(serviceRoleHandler)) {
            serviceRoleHandler.getConfig(clusterId, list);
        }

        return Result.success(list);
    }

    @Override
    public Result saveServiceConfig(Integer clusterId, String serviceName, List<ServiceConfig> list, Integer roleGroupId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        ServiceConfigMap.put(clusterInfo.getClusterCode() + Constants.UNDERLINE + serviceName + Constants.CONFIG, list);
        HashMap<String, ServiceConfig> map = new HashMap<>();
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);

        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (Objects.nonNull(serviceRoleHandler)) {
            serviceRoleHandler.handlerConfig(clusterId, list);
        }

        FrameServiceEntity frameServiceEntity = frameService.getServiceByFrameCodeAndServiceName(clusterInfo.getClusterFrame(), serviceName);
        for (ServiceConfig serviceConfig : list) {
//            //add to global variable
            if (Constants.INPUT.equals(serviceConfig.getType())) {
                String variableName = "${" + serviceConfig.getName() + "}";
                String value = String.valueOf(serviceConfig.getValue());
                if (globalVariables.containsKey(variableName)) {
                    ClusterVariable clusterVariable = variableService.getVariableByVariableName(variableName, clusterId);
                    if (Objects.nonNull(clusterVariable) && !value.equals(clusterVariable.getVariableValue())) {
                        clusterVariable.setVariableValue(value);
                        variableService.updateById(clusterVariable);
                    }
                } else {
                    ClusterVariable clusterVariable = new ClusterVariable();
                    clusterVariable.setClusterId(clusterId);
                    clusterVariable.setVariableName(variableName);
                    clusterVariable.setVariableValue(value);
                    variableService.save(clusterVariable);
                }
                globalVariables.put(variableName, value);
            }
            map.put(serviceConfig.getName(), serviceConfig);
        }

        //update config-file
        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        FrameServiceEntity frameService = this.frameService.getServiceByFrameCodeAndServiceName(clusterInfo.getClusterFrame(), serviceName);
        if (StringUtils.isNotBlank(frameService.getConfigFileJson())) {
            Map<JSONObject, JSONArray> configMap = JSONObject.parseObject(frameService.getConfigFileJson(), Map.class);
            for (JSONObject fileJson : configMap.keySet()) {
                Generators generators = fileJson.toJavaObject(Generators.class);
                List<ServiceConfig> serviceConfigs = configMap.get(fileJson).toJavaList(ServiceConfig.class);
                for (ServiceConfig config : serviceConfigs) {
                    logger.info(config.getName());
                    if (map.containsKey(config.getName())) {
                        ServiceConfig newConfig = map.get(config.getName());
                        config.setValue(map.get(config.getName()).getValue());
                        config.setHidden(newConfig.isHidden());
                        config.setRequired(newConfig.isRequired());
                    }
                }
                configFileMap.put(generators, serviceConfigs);
            }
        }
        if ("prometheus".equals(serviceName.toLowerCase())) {
            logger.info("add worker and node to prometheus");
            //添加worker和node到监控列表
            List<ClusterHostEntity> hostList = hostService.list(new QueryWrapper<ClusterHostEntity>()
                    .eq(Constants.MANAGED, 1)
                    .eq(Constants.CLUSTER_ID, clusterId));
            Generators workerGenerators = new Generators();
            workerGenerators.setFilename("worker.json");
            workerGenerators.setOutputDirectory("configs");
            workerGenerators.setConfigFormat("custom");
            workerGenerators.setTemplateName("scrape.ftl");

            Generators nodeGenerators = new Generators();
            nodeGenerators.setFilename("linux.json");
            nodeGenerators.setOutputDirectory("configs");
            nodeGenerators.setConfigFormat("custom");
            nodeGenerators.setTemplateName("scrape.ftl");
            ArrayList<ServiceConfig> workerServiceConfigs = new ArrayList<>();
            ArrayList<ServiceConfig> nodeServiceConfigs = new ArrayList<>();
            for (ClusterHostEntity clusterHostEntity : hostList) {
                ServiceConfig serviceConfig = new ServiceConfig();
                serviceConfig.setName("worker_" + clusterHostEntity.getHostname());
                serviceConfig.setValue(clusterHostEntity.getHostname() + ":8585");
                serviceConfig.setRequired(true);
                workerServiceConfigs.add(serviceConfig);

                ServiceConfig nodeServiceConfig = new ServiceConfig();
                nodeServiceConfig.setName("node_" + clusterHostEntity.getHostname());
                nodeServiceConfig.setValue(clusterHostEntity.getHostname() + ":9100");
                nodeServiceConfig.setRequired(true);
                nodeServiceConfigs.add(nodeServiceConfig);
            }
            configFileMap.put(workerGenerators, workerServiceConfigs);
            configFileMap.put(nodeGenerators, nodeServiceConfigs);
        }
        //查询服务实例是否存在
        ClusterServiceInstanceEntity serviceInstanceEntity = serviceInstanceService.getServiceInstanceByClusterIdAndServiceName(clusterId, serviceName);
        if (Objects.isNull(serviceInstanceEntity)) {
            serviceInstanceEntity = new ClusterServiceInstanceEntity();
            serviceInstanceEntity.setClusterId(clusterId);
            serviceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
            serviceInstanceEntity.setServiceName(serviceName);
            serviceInstanceEntity.setLabel(frameServiceEntity.getLabel());
            serviceInstanceEntity.setCreateTime(new Date());
            serviceInstanceEntity.setUpdateTime(new Date());
            serviceInstanceEntity.setNeedRestart(NeedRestart.NO);
            serviceInstanceEntity.setFrameServiceId(frameServiceEntity.getId());
            serviceInstanceEntity.setSortNum(frameServiceEntity.getSortNum());
            serviceInstanceService.save(serviceInstanceEntity);

            ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = new ClusterServiceInstanceRoleGroup();
            clusterServiceInstanceRoleGroup.setServiceInstanceId(serviceInstanceEntity.getId());
            clusterServiceInstanceRoleGroup.setClusterId(clusterId);
            clusterServiceInstanceRoleGroup.setRoleGroupName("默认角色组");
            clusterServiceInstanceRoleGroup.setServiceName(serviceName);
            clusterServiceInstanceRoleGroup.setRoleGroupType("default");
            roleGroupService.save(clusterServiceInstanceRoleGroup);

            ClusterServiceRoleGroupConfig roleGroupConfig = new ClusterServiceRoleGroupConfig();
            roleGroupConfig.setRoleGroupId(clusterServiceInstanceRoleGroup.getId());
            roleGroupConfig.setClusterId(clusterId);
            roleGroupConfig.setCreateTime(new Date());
            roleGroupConfig.setUpdateTime(new Date());
            roleGroupConfig.setServiceName(serviceName);
            buildConfig(list, configFileMap, roleGroupConfig);
            roleGroupConfig.setConfigVersion(1);
            groupConfigService.save(roleGroupConfig);
            CacheUtils.put("UseRoleGroup_" + serviceInstanceEntity.getId(), clusterServiceInstanceRoleGroup.getId());
        } else {
            serviceInstanceEntity.setUpdateTime(new Date());
            serviceInstanceEntity.setLabel(frameServiceEntity.getLabel());
            ClusterServiceRoleGroupConfig roleGroupConfig;
            if (Objects.isNull(roleGroupId)) {
                ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getRoleGroupByServiceInstanceId(serviceInstanceEntity.getId());
                roleGroupConfig = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
            } else {
                roleGroupConfig = groupConfigService.getConfigByRoleGroupId(roleGroupId);
            }
            String configJson = JSONObject.toJSONString(list);
            String newMd5 = SecureUtil.md5(configJson);
            String configJsonMd5 = roleGroupConfig.getConfigJsonMd5();
            CacheUtils.put("UseRoleGroup_" + serviceInstanceEntity.getId(), roleGroupConfig.getRoleGroupId());
            if (!configJsonMd5.equals(newMd5)) {
                ClusterServiceRoleGroupConfig newRoleGroupConfig = new ClusterServiceRoleGroupConfig();
                if (Objects.isNull(roleGroupId)) {
                    int count = roleGroupService.count(new QueryWrapper<ClusterServiceInstanceRoleGroup>()
                            .eq(Constants.ROLE_GROUP_TYPE, "auto")
                            .eq(Constants.SERVICE_INSTANCE_ID, serviceInstanceEntity.getId()));
                    ClusterServiceInstanceRoleGroup roleGroup = new ClusterServiceInstanceRoleGroup();
                    int num = count + 1;
                    roleGroup.setRoleGroupName("RoleGroup" + num);
                    roleGroup.setServiceInstanceId(serviceInstanceEntity.getId());
                    roleGroup.setServiceName(serviceInstanceEntity.getServiceName());
                    roleGroup.setClusterId(serviceInstanceEntity.getClusterId());
                    roleGroup.setRoleGroupType("auto");
                    roleGroupService.save(roleGroup);
                    newRoleGroupConfig.setConfigVersion(1);
                    newRoleGroupConfig.setRoleGroupId(roleGroup.getId());
                    CacheUtils.put("UseRoleGroup_" + serviceInstanceEntity.getId(), roleGroup.getId());
                } else {
                    newRoleGroupConfig.setConfigVersion(roleGroupConfig.getConfigVersion() + 1);
                    newRoleGroupConfig.setRoleGroupId(roleGroupConfig.getRoleGroupId());
                }
                newRoleGroupConfig.setClusterId(clusterId);
                newRoleGroupConfig.setCreateTime(new Date());
                newRoleGroupConfig.setUpdateTime(new Date());
                newRoleGroupConfig.setServiceName(serviceInstanceEntity.getServiceName());
                buildConfig(list, configFileMap, newRoleGroupConfig);
                groupConfigService.save(newRoleGroupConfig);
                serviceInstanceEntity.setNeedRestart(NeedRestart.YES);
                roleInstanceService.updateToNeedRestart(roleGroupId);

            }
            serviceInstanceService.updateById(serviceInstanceEntity);
        }
        return Result.success();
    }


    private void buildConfig(List<ServiceConfig> list, HashMap<Generators, List<ServiceConfig>> configFileMap, ClusterServiceRoleGroupConfig roleGroupConfig) {
        String configJson = JSONObject.toJSONString(list);
        String configFileJson = JSONObject.toJSONString(configFileMap);
        roleGroupConfig.setConfigJson(configJson);
        roleGroupConfig.setConfigJsonMd5(SecureUtil.md5(configJson));
        roleGroupConfig.setConfigFileJson(configFileJson);
        roleGroupConfig.setConfigFileJsonMd5(SecureUtil.md5(configFileJson));
    }

    @Override
    public Result saveServiceRoleHostMapping(Integer clusterId, List<ServiceRoleHostMapping> list) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> map = new HashMap<>();
        if (CacheUtils.constainsKey(hostMapKey)) {
            map = (HashMap<String, List<String>>) CacheUtils.get(hostMapKey);
        }

        for (ServiceRoleHostMapping serviceRoleHostMapping : list) {
            if ("JournalNode".equals(serviceRoleHostMapping.getServiceRole())) {
                if (serviceRoleHostMapping.getHosts().size() != 3) {
                    return Result.error(Status.THREE_JOURNALNODE_DEPLOYMENTS_REQUIRED.getMsg());
                }
            }
            if ("NameNode".equals(serviceRoleHostMapping.getServiceRole())) {
                if (serviceRoleHostMapping.getHosts().size() != 2) {
                    return Result.error(Status.TWO_NAMENODES_NEED_TO_BE_DEPLOYED.getMsg());
                }
            }
            if ("ZKFC".equals(serviceRoleHostMapping.getServiceRole())) {
                if (serviceRoleHostMapping.getHosts().size() != 2) {
                    return Result.error(Status.TWO_ZKFC_DEVICES_ARE_REQUIRED.getMsg());
                }
            }
            if ("ResourceManager".equals(serviceRoleHostMapping.getServiceRole())) {
                if (serviceRoleHostMapping.getHosts().size() != 2) {
                    return Result.error(Status.TWO_RESOURCEMANAGER_ARE_DEPLOYED.getMsg());
                }
            }
            map.put(serviceRoleHostMapping.getServiceRole(), serviceRoleHostMapping.getHosts());

            ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceRoleHostMapping.getServiceRole());
            if (Objects.nonNull(serviceRoleHandler)) {
                serviceRoleHandler.handler(clusterId, serviceRoleHostMapping.getHosts());
            }

        }
        CacheUtils.put(clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING, map);
        return Result.success();
    }


    @Override
    public Result saveHostServiceRoleMapping(Integer clusterId, List<HostServiceRoleMapping> list) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        HashMap<String, List<String>> map = new HashMap<>();
        for (HostServiceRoleMapping hostServiceRoleMapping : list) {
            map.put(hostServiceRoleMapping.getHost(), hostServiceRoleMapping.getServiceRoles());
        }
        CacheUtils.put(clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.HOST_SERVICE_ROLE_MAPPING, map);
        return Result.success();
    }

    @Override
    public Result getServiceRoleDeployOverview(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        HashMap<String, List<String>> map = (HashMap<String, List<String>>) CacheUtils.get(clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING);
        return Result.success(map);
    }

    /**
     * 1、查询所有需要安装的服务
     * 2、根据服务依赖关系决定服务安装顺序 --DAG
     * 3、安装master服务角色
     * 4、安装worker与client服务角色
     *
     * @param clusterId
     * @param commandIds
     * @return
     */
    @Override
    public Result startInstallService(Integer clusterId, List<String> commandIds) {
        Collection<ClusterServiceCommandEntity> commands = commandService.listByIds(commandIds);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        DAG<String, ServiceNode, ServiceNodeEdge> dag = new DAG<>();
        for (ClusterServiceCommandEntity command : commands) {
            List<ClusterServiceCommandHostCommandEntity> commandHostList = hostCommandService.getHostCommandListByCommandId(command.getCommandId());
            List<ServiceRoleInfo> masterRoles = new ArrayList<>();
            List<ServiceRoleInfo> elseRoles = new ArrayList<>();
            ServiceNode serviceNode = new ServiceNode();
            String serviceKey = clusterInfo.getClusterFrame() + Constants.UNDERLINE + command.getServiceName();
            ServiceInfo serviceInfo = ServiceInfoMap.get(serviceKey);
            for (ClusterServiceCommandHostCommandEntity hostCommand : commandHostList) {
                String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + command.getServiceName() + Constants.UNDERLINE + hostCommand.getServiceRoleName();
                ServiceRoleInfo serviceRoleInfo = ServiceRoleMap.get(key);
                serviceRoleInfo.setHostname(hostCommand.getHostname());
                serviceRoleInfo.setHostCommandId(hostCommand.getHostCommandId());
                serviceRoleInfo.setClusterId(clusterId);
                serviceRoleInfo.setParentName(command.getServiceName());
                if (Constants.MASTER.equals(serviceRoleInfo.getRoleType())) {
                    masterRoles.add(serviceRoleInfo);
                } else {
                    elseRoles.add(serviceRoleInfo);
                }
            }
            serviceNode.setMasterRoles(masterRoles);
            serviceNode.setElseRoles(elseRoles);
            dag.addNode(command.getServiceName(), serviceNode);
            if (serviceInfo.getDependencies().size() > 0) {
                for (String dependency : serviceInfo.getDependencies()) {
                    dag.addEdge(dependency, command.getServiceName());
                }
            }
        }
        return Result.success();
    }

    @Override
    public void downloadPackage(String packageName, HttpServletResponse response) throws IOException {
        FileInputStream inputStream = null;
        OutputStream out = null;
        //通过文件路径获得File对象
        File file = new File(Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + packageName);

        inputStream = new FileInputStream(file);

        response.reset();
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Length", "" + file.length());
        // 支持中文名称文件,需要对header进行单独设置，不然下载的文件名会出现乱码或者无法显示的情况
        // 设置响应头，控制浏览器下载该文件
        response.setHeader("Content-Disposition", "attachment;filename=" + packageName);
        //通过response获取ServletOutputStream对象(out)
        out = response.getOutputStream();
        int length = 0;
        byte[] buffer = new byte[1024];
        while ((length = inputStream.read(buffer)) != -1) {
            //4.写到输出流(out)中
            out.write(buffer, 0, length);
        }
        inputStream.close();
        out.flush();
        out.close();

    }

    @Override
    public Result getServiceRoleHostMapping(Integer clusterId) {
        return null;
    }

    @Override
    public Result checkServiceDependency(Integer clusterId, String serviceIds) {
        //
        List<ClusterServiceInstanceEntity> serviceInstanceList = serviceInstanceService.listRunningServiceInstance(clusterId);
        Map<String, ClusterServiceInstanceEntity> instanceMap = serviceInstanceList.stream().collect(Collectors.toMap(ClusterServiceInstanceEntity::getServiceName, e -> e, (v1, v2) -> v1));

        List<FrameServiceEntity> list = frameService.listServices(serviceIds);
        Map<String, FrameServiceEntity> serviceMap = list.stream().collect(Collectors.toMap(FrameServiceEntity::getServiceName, e -> e, (v1, v2) -> v1));
        if (!instanceMap.containsKey("ALERTMANAGER") && !serviceMap.containsKey("ALERTMANAGER")) {
            return Result.error("service install depends on alertmanager ,please make sure you have selected it or that alertmanager is normal and running");
        }
        if (!instanceMap.containsKey("GRAFANA") && !serviceMap.containsKey("GRAFANA")) {
            return Result.error("service install depends on grafana ,please make sure you have selected it or that grafana is normal and running");
        }
        if (!instanceMap.containsKey("PROMETHEUS") && !serviceMap.containsKey("PROMETHEUS")) {
            return Result.error("service install depends on prometheus ,please make sure you have selected it or that prometheus is normal and running");
        }

        for (FrameServiceEntity frameServiceEntity : list) {
            for (String dependService : frameServiceEntity.getDependencies().split(",")) {
                if (StringUtils.isNotBlank(dependService) && !instanceMap.containsKey(dependService) && !serviceMap.containsKey(dependService)) {
                    return Result.error("" + frameServiceEntity.getServiceName() + " install depends on " + dependService + ",please make sure that you have selected it or that " + dependService + " is normal and running");
                }
            }

        }
        return Result.success();
    }


}
