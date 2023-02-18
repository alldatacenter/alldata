package com.datasophon.api.master;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.load.ServiceRoleJmxMap;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateAlertConfigCommand;
import com.datasophon.common.command.GenerateHostPrometheusConfig;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.command.GenerateSRPromConfigCommand;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PrometheusActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GeneratePrometheusConfigCommand) {

            GeneratePrometheusConfigCommand command = (GeneratePrometheusConfigCommand) msg;
            ClusterServiceInstanceService serviceInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(command.getServiceInstanceId());
            List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService.getServiceRoleInstanceListByServiceId(serviceInstance.getId());

            ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole("Prometheus", null, command.getClusterId());

            logger.info("start to genetate {} prometheus config",serviceInstance.getServiceName());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

            HashMap<String, List<String>> roleMap = new HashMap<>();
            for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleInstanceList) {
                if (roleMap.containsKey(roleInstanceEntity.getServiceRoleName())) {
                    List<String> list = roleMap.get(roleInstanceEntity.getServiceRoleName());
                    list.add(roleInstanceEntity.getHostname());
                } else {
                    List<String> list = new ArrayList<>();
                    list.add(roleInstanceEntity.getHostname());
                    roleMap.put(roleInstanceEntity.getServiceRoleName(), list);
                }
            }
            for (Map.Entry<String, List<String>> roleEntry : roleMap.entrySet()) {
                Generators generators = new Generators();
                generators.setFilename(roleEntry.getKey().toLowerCase() + ".json");
                generators.setOutputDirectory("configs");
                generators.setConfigFormat("custom");
                generators.setTemplateName("scrape.ftl");
                List<String> value = roleEntry.getValue();
                ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
                for (String hostname : value) {
                    String jmxKey = command.getClusterFrame() + Constants.UNDERLINE + serviceInstance.getServiceName() + Constants.UNDERLINE + roleEntry.getKey();
                    if (ServiceRoleJmxMap.exists(jmxKey)) {
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName(roleEntry.getKey() + Constants.UNDERLINE + hostname);
                        serviceConfig.setValue(hostname + ":" + ServiceRoleJmxMap.get(jmxKey));
                        serviceConfig.setRequired(true);
                        serviceConfigs.add(serviceConfig);
                    }
                }
                configFileMap.put(generators, serviceConfigs);
            }
            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setName("Prometheus");
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
            serviceRoleInfo.setHostname(prometheusInstance.getHostname());
            ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
            ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);
            if(execResult.getExecResult()){
                //重新加载prometheus配置
                HttpUtil.post("http://"+prometheusInstance.getHostname()+":9090/-/reload","");
            }
        }else if(msg instanceof GenerateHostPrometheusConfig){
            GenerateHostPrometheusConfig command = (GenerateHostPrometheusConfig) msg;
            Integer clusterId = command.getClusterId();
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
            ClusterHostService hostService = SpringTool.getApplicationContext().getBean(ClusterHostService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            List<ClusterHostEntity> hostList = hostService.list(new QueryWrapper<ClusterHostEntity>()
                    .eq(Constants.MANAGED, 1)
                    .eq(Constants.CLUSTER_ID,clusterId));
            ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole("Prometheus", null, command.getClusterId());
            if(Objects.nonNull(prometheusInstance)){
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
                    serviceConfig.setName("worker_"+clusterHostEntity.getHostname());
                    serviceConfig.setValue(clusterHostEntity.getHostname()+":8585");
                    serviceConfig.setRequired(true);
                    workerServiceConfigs.add(serviceConfig);

                    ServiceConfig nodeServiceConfig = new ServiceConfig();
                    nodeServiceConfig.setName("node_"+clusterHostEntity.getHostname());
                    nodeServiceConfig.setValue(clusterHostEntity.getHostname()+":9100");
                    nodeServiceConfig.setRequired(true);
                    nodeServiceConfigs.add(nodeServiceConfig);
                }
                configFileMap.put(workerGenerators,workerServiceConfigs);
                configFileMap.put(nodeGenerators,nodeServiceConfigs);
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setName("Prometheus");
                serviceRoleInfo.setParentName("PROMETHEUS");
                serviceRoleInfo.setConfigFileMap(configFileMap);
                serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
                serviceRoleInfo.setHostname(prometheusInstance.getHostname());
                ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
                ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);
                if(execResult.getExecResult()){
                    //reload prometheus config
                    HttpUtil.post("http://"+prometheusInstance.getHostname()+":9090/-/reload","");
                }
            }

        }else if(msg instanceof GenerateAlertConfigCommand){

            GenerateAlertConfigCommand command = (GenerateAlertConfigCommand) msg;
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole("Prometheus", null, command.getClusterId());
            if(Objects.nonNull(prometheusInstance)){
                ActorSelection alertConfigActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + prometheusInstance.getHostname() + ":2552/user/worker/alertConfigActor");
                Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
                Future<Object> configureFuture = Patterns.ask(alertConfigActor, command, timeout);
                ExecResult configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
                if(configResult.getExecResult()){
                    //reload prometheus config
                    HttpUtil.post("http://"+prometheusInstance.getHostname()+":9090/-/reload","");
                }
            }

        }else if(msg instanceof GenerateSRPromConfigCommand){
            GenerateSRPromConfigCommand command = (GenerateSRPromConfigCommand) msg;
            ClusterServiceInstanceService serviceInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(command.getServiceInstanceId());
            List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService.getServiceRoleInstanceListByServiceId(serviceInstance.getId());

            ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole("Prometheus", null, command.getClusterId());

            logger.info("start to genetate {} prometheus config",serviceInstance.getServiceName());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();


            ArrayList<String> feList = new ArrayList<>();
            ArrayList<String> beList = new ArrayList<>();

            for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleInstanceList) {
                String jmxKey = command.getClusterFrame() + Constants.UNDERLINE + serviceInstance.getServiceName()+ Constants.UNDERLINE + roleInstanceEntity.getServiceRoleName();
                logger.info("jmxKey is {}",jmxKey);
                if ("FE".equals(roleInstanceEntity.getServiceRoleName())) {
                    logger.info(ServiceRoleJmxMap.get(jmxKey));
                    feList.add(roleInstanceEntity.getHostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                } else {
                    beList.add(roleInstanceEntity.getHostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                }
            }
            ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
            Generators generators = new Generators();
            generators.setFilename(command.getFilename());
            generators.setOutputDirectory("configs");
            generators.setConfigFormat("custom");
            generators.setTemplateName("starrocks-prom.ftl");

            ServiceConfig feServiceConfig = new ServiceConfig();
            feServiceConfig.setName("feList");
            feServiceConfig.setValue(feList);
            feServiceConfig.setRequired(true);
            feServiceConfig.setConfigType("map");

            ServiceConfig beServiceConfig = new ServiceConfig();
            beServiceConfig.setName("beList");
            beServiceConfig.setValue(beList);
            beServiceConfig.setConfigType("map");
            beServiceConfig.setRequired(true);
            serviceConfigs.add(feServiceConfig);
            serviceConfigs.add(beServiceConfig);
            configFileMap.put(generators,serviceConfigs);

            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setName("Prometheus");
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
            serviceRoleInfo.setHostname(prometheusInstance.getHostname());
            ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
            ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);
            if(execResult.getExecResult()){
                //reload prometheus
                HttpUtil.post("http://"+prometheusInstance.getHostname()+":9090/-/reload","");
            }
        }

        else {
            unhandled(msg);
        }

    }
}
