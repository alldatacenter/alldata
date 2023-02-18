package com.datasophon.api.load;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.configuration.ConfigBean;
import com.datasophon.api.service.*;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.common.model.*;
import com.datasophon.dao.entity.*;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.utils.HostUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LoadServiceMeta implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LoadServiceMeta.class);

    private static final String PATH = "meta";

    @Autowired
    private FrameServiceService frameServiceService;

    @Autowired
    private FrameInfoService frameInfoService;

    @Autowired
    private FrameServiceRoleService roleService;

    @Autowired
    private ClusterVariableService variableService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ConfigBean configBean;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;


    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    private static final String HDFS = "HDFS";

    private static final String HADOOP = "HADOOP";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        //load hosts
        loadHostsFileInEtc();
        logger.info("put ip host map into cache");

        File[] ddps = FileUtil.ls(PATH);
        //load global variable
        List<ClusterInfoEntity> clusters = clusterInfoService.list();
        loadGlobalVariables(clusters);

        for (File path : ddps) {
            List<File> files = FileUtil.loopFiles(path);
            String frameCode = path.getName();
            FrameInfoEntity frameInfo = saveClusterFrame(frameCode);
            //analysis file
            for (File file : files) {
                if (file.getName().endsWith(Constants.JSON)) {
                    String serviceName = file.getParentFile().getName();
                    String serviceDdl = FileReader.create(file).readString();
                    ServiceInfo serviceInfo = JSONObject.parseObject(serviceDdl, ServiceInfo.class);
                    String serviceInfoMd5 = SecureUtil.md5(serviceDdl);

                    //save service config
                    List<ServiceConfig> allParameters = serviceInfo.getParameters();
                    Map<String, ServiceConfig> map = allParameters.stream().collect(Collectors.toMap(ServiceConfig::getName, serviceConfig -> serviceConfig, (v1, v2) -> v1));
                    Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

                    buildConfigFileMap(serviceInfo, map, configFileMap);

                    PackageUtils.putServicePackageName(frameCode, serviceName, serviceInfo.getDecompressPackageName());

                    putServiceHomeToVariable(clusters, serviceName, serviceInfo.getDecompressPackageName());
                    //save service and service config
                    FrameServiceEntity serviceEntity = saveFrameService(frameCode, frameInfo, serviceName, serviceDdl, serviceInfo, serviceInfoMd5, allParameters, configFileMap);
                    //save frame service role
                    saveFrameServiceRole(frameCode, serviceName, serviceInfo, serviceEntity);
                }
            }
        }
    }

    private void putServiceHomeToVariable(List<ClusterInfoEntity> clusters, String serviceName, String decompressPackageName) {
        for (ClusterInfoEntity cluster : clusters) {
            Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + cluster.getId());
            if (HDFS.equals(serviceName)) {
                serviceName = HADOOP;
            }
            globalVariables.put("${" + serviceName + "_HOME}", Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        }
    }


    private void saveFrameServiceRole(String frameCode, String serviceName, ServiceInfo serviceInfo, FrameServiceEntity serviceEntity) {
        List<ServiceRoleInfo> serviceRoles = serviceInfo.getRoles();

        for (ServiceRoleInfo serviceRole : serviceRoles) {
            String key = frameCode + Constants.UNDERLINE + serviceInfo.getName() + Constants.UNDERLINE + serviceRole.getName();
            logger.info("put {} {} {} service role info into cache", frameCode, serviceName, serviceRole.getName());
            if (StringUtils.isNotBlank(serviceRole.getJmxPort())) {
                logger.info("{} jmx port is :{} and the jmx key is: {}", serviceRole.getName(), serviceRole.getJmxPort(), key);
                ServiceRoleJmxMap.put(key, serviceRole.getJmxPort());
            }
            ServiceRoleMap.put(key, serviceRole);
            String serviceRoleJson = JSONObject.toJSONString(serviceRole);
            String serviceRoleJsonMd5 = SecureUtil.md5(serviceRoleJson);
            //持久化服务角色元信息至数据库
            FrameServiceRoleEntity role = roleService.getServiceRoleByServiceIdAndServiceRoleName(serviceEntity.getId(), serviceRole.getName());
            if (Objects.isNull(role)) {
                role = new FrameServiceRoleEntity();
                generateFrameServiceRole(frameCode, serviceEntity, serviceRole, serviceRoleJson, serviceRoleJsonMd5, role);
                roleService.save(role);
            } else if (!role.getServiceRoleJsonMd5().equals(serviceRoleJsonMd5)) {
                generateFrameServiceRole(frameCode, serviceEntity, serviceRole, serviceRoleJson, serviceRoleJsonMd5, role);
                roleService.updateById(role);
            }
        }
        logger.info("put {} {} service info into cache", frameCode, serviceName);
        ServiceInfoMap.put(frameCode + Constants.UNDERLINE + serviceName, serviceInfo);
    }

    private FrameServiceEntity saveFrameService(String frameCode, FrameInfoEntity frameInfo, String serviceName, String serviceDdl, ServiceInfo serviceInfo, String serviceInfoMd5, List<ServiceConfig> allParameters, Map<Generators, List<ServiceConfig>> configFileMap) {
        FrameServiceEntity serviceEntity = frameServiceService.getServiceByFrameIdAndServiceName(frameInfo.getId(), serviceName);
        if (Objects.isNull(serviceEntity)) {
            serviceEntity = new FrameServiceEntity();
            buildServiceEntity(frameCode, frameInfo.getId(), serviceName, serviceDdl, serviceInfo, serviceInfoMd5, serviceEntity, configFileMap, serviceInfo.getDecompressPackageName());

            frameServiceService.save(serviceEntity);
        } else if (!serviceEntity.getServiceJsonMd5().equals(serviceInfoMd5)) {
            String configMapStr = JSONObject.toJSONString(configFileMap);
            String configFileMapStrMd5 = SecureUtil.md5(configMapStr);
            if (!configFileMapStrMd5.equals(serviceEntity.getConfigFileJsonMd5())) {
                //update config
                updateServiceInstanceConfig(frameCode, serviceInfo.getName(), serviceInfo.getParameters(), configFileMap);
            }
            buildServiceEntity(frameCode, frameInfo.getId(), serviceName, serviceDdl, serviceInfo, serviceInfoMd5, serviceEntity, configFileMap, serviceInfo.getDecompressPackageName());
            frameServiceService.updateById(serviceEntity);
        }

        ServiceConfigMap.put(frameCode + Constants.UNDERLINE + serviceInfo.getName() + Constants.CONFIG, allParameters);
        ServiceConfigFileMap.put(frameCode + Constants.UNDERLINE + serviceInfo.getName() + Constants.CONFIG_FILE, configFileMap);

        return serviceEntity;
    }

    private void buildConfigFileMap(ServiceInfo serviceInfo, Map<String, ServiceConfig> map, Map<Generators, List<ServiceConfig>> configFileMap) {
        ConfigWriter configWriter = serviceInfo.getConfigWriter();
        List<Generators> generators = configWriter.getGenerators();
        for (Generators generator : generators) {
            List<ServiceConfig> list = new ArrayList<>();
            List<String> includeParams = generator.getIncludeParams();
            for (String includeParam : includeParams) {
                if (map.containsKey(includeParam)) {
                    ServiceConfig serviceConfig = map.get(includeParam);
                    ServiceConfig newConfig = new ServiceConfig();
                    BeanUtils.copyProperties(serviceConfig, newConfig);
                    list.add(newConfig);
                }
            }
            if (configFileMap.containsKey(generator)) {
                configFileMap.get(generator).addAll(list);
            } else {
                configFileMap.put(generator, list);
            }
        }
    }

    private FrameInfoEntity saveClusterFrame(String frameCode) {
        FrameInfoEntity frameInfo = frameInfoService.getOne(new QueryWrapper<FrameInfoEntity>().eq("frame_code", frameCode));
        if (Objects.isNull(frameInfo)) {
            frameInfo = new FrameInfoEntity();
            frameInfo.setFrameCode(frameCode);
            frameInfoService.save(frameInfo);
        }
        return frameInfo;
    }


    private void loadGlobalVariables(List<ClusterInfoEntity> clusters) throws UnknownHostException {
        HashMap<String, String> globalVariables = new HashMap<>();
        if (Objects.nonNull(clusters) && clusters.size() > 0) {
            for (ClusterInfoEntity cluster : clusters) {
                List<ClusterVariable> variables = variableService.list(new QueryWrapper<ClusterVariable>().eq(Constants.CLUSTER_ID, cluster.getId()));
                for (ClusterVariable variable : variables) {
                    globalVariables.put(variable.getVariableName(), variable.getVariableValue());
                }
                globalVariables.put("${apiHost}", InetAddress.getLocalHost().getHostName());
                globalVariables.put("${apiPort}", configBean.getServerPort());
                globalVariables.put("${INSTALL_PATH}", Constants.INSTALL_PATH);
                CacheUtils.put("globalVariables" + Constants.UNDERLINE + cluster.getId(), globalVariables);
                ProcessUtils.createServiceActor(cluster);
            }
        }
    }

    private void loadHostsFileInEtc() {
        HostUtils.read();
    }

    private void updateServiceInstanceConfig(String frameCode, String serviceName, List<ServiceConfig> parameters, Map<Generators, List<ServiceConfig>> configFileMap) {
        //查询frameCode相同的集群
        List<ClusterInfoEntity> clusters = clusterInfoService.getClusterByFrameCode(frameCode);
        //查询集群的服务实例
        for (ClusterInfoEntity cluster : clusters) {
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getServiceInstanceByClusterIdAndServiceName(cluster.getId(), serviceName);
            if (Objects.nonNull(serviceInstance)) {
                ClusterServiceRoleGroupConfig config = roleGroupService.getRoleGroupConfigByServiceId(serviceInstance.getId());
                String configJson = config.getConfigJson();
                List<ServiceConfig> serviceConfigs = JSONArray.parseArray(configJson, ServiceConfig.class);
                ProcessUtils.addAll(serviceConfigs, parameters);
                //更新服务实例的配置
                config.setConfigJson(JSONObject.toJSONString(serviceConfigs));
                roleGroupConfigService.updateById(config);
            }
        }

    }

    private void generateFrameServiceRole(String frameCode, FrameServiceEntity serviceEntity, ServiceRoleInfo serviceRole, String serviceRoleJson, String serviceRoleJsonMd5, FrameServiceRoleEntity role) {
        role.setServiceId(serviceEntity.getId());
        role.setServiceRoleName(serviceRole.getName());
        role.setCardinality(serviceRole.getCardinality());
        role.setFrameCode(frameCode);
        role.setServiceRoleJson(serviceRoleJson);
        role.setServiceRoleType(CommonUtils.convertRoleType(serviceRole.getRoleType().getName()));
        role.setJmxPort(serviceRole.getJmxPort());
        role.setServiceRoleJsonMd5(serviceRoleJsonMd5);
        role.setLogFile(serviceRole.getLogFile());
    }

    private void buildServiceEntity(String frameCode, Integer frameInfoId, String serviceName, String serviceDdl, ServiceInfo serviceInfo, String serviceInfoMd5, FrameServiceEntity serviceEntity, Map<Generators, List<ServiceConfig>> configFileMap, String decompressPackageName) {
        serviceEntity.setServiceName(serviceName);
        serviceEntity.setLabel(serviceInfo.getLabel());
        serviceEntity.setFrameId(frameInfoId);
        serviceEntity.setServiceDesc(serviceInfo.getDescription());
        serviceEntity.setServiceVersion(serviceInfo.getVersion());
        serviceEntity.setPackageName(serviceInfo.getPackageName());
        serviceEntity.setDependencies(StringUtils.join(serviceInfo.getDependencies(), ","));
        serviceEntity.setFrameCode(frameCode);
        serviceEntity.setServiceConfig(JSON.toJSONString(serviceInfo.getParameters()));
        serviceEntity.setServiceJson(serviceDdl);
        serviceEntity.setServiceJsonMd5(serviceInfoMd5);
        serviceEntity.setDecompressPackageName(decompressPackageName);
        serviceEntity.setConfigFileJson(JSONObject.toJSONString(configFileMap));
        serviceEntity.setConfigFileJsonMd5(SecureUtil.md5(serviceEntity.getConfigFileJson()));
        serviceEntity.setSortNum(serviceInfo.getSortNum());
    }

}
