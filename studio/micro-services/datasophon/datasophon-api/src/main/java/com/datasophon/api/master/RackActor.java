package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.common.command.GenerateRackPropCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RackActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(RackActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GenerateRackPropCommand) {
            GenerateRackPropCommand command = (GenerateRackPropCommand) msg;

            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            ClusterHostService hostService = SpringTool.getApplicationContext().getBean(ClusterHostService.class);
            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
            //update rack table
            List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService.getServiceRoleInstanceListByClusterIdAndRoleName(command.getClusterId(), "NameNode");
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(command.getClusterId());
            //build config file map
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
            Generators generators = new Generators();
            generators.setFilename("rack.properties");
            generators.setOutputDirectory("etc/hadoop");
            generators.setConfigFormat("properties2");

            ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
            List<ClusterHostEntity> hostList = hostService.list();
            for (ClusterHostEntity clusterHostEntity : hostList) {
                ServiceConfig serviceConfig = ProcessUtils.createServiceConfig(clusterHostEntity.getIp(), Constants.SLASH + clusterHostEntity.getRack(), "input");
                serviceConfigs.add(serviceConfig);
            }
            configFileMap.put(generators, serviceConfigs);
            for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleList) {
                //generate rack.properties
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setName("NameNode");
                serviceRoleInfo.setParentName("HDFS");
                serviceRoleInfo.setConfigFileMap(configFileMap);
                serviceRoleInfo.setDecompressPackageName(PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(), "HDFS"));
                serviceRoleInfo.setHostname(roleInstanceEntity.getHostname());
                ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
                ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);
                if (!execResult.getExecResult()) {
                    logger.error("generate rack.properties failed");
                }
            }
        }
    }
}
