package com.datasophon.api.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.ServiceCommandActor;
import com.datasophon.api.master.ServiceExecuteResultActor;
import com.datasophon.api.master.handler.service.*;
import com.datasophon.api.service.*;
import com.datasophon.api.master.MasterServiceActor;
import com.datasophon.common.model.*;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.enums.*;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.command.FileOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);


    public static void saveServiceInstallInfo(ServiceRoleInfo serviceRoleInfo) {
        ClusterServiceInstanceService serviceInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
        ClusterServiceInstanceConfigService serviceInstanceConfigService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceConfigService.class);
        ClusterServiceRoleInstanceService serviceRoleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterServiceRoleInstanceWebuisService webuisService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceWebuisService.class);
        ClusterServiceInstanceRoleGroupService roleGroupService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceRoleGroupService.class);

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(serviceRoleInfo.getClusterId());
        //判断当前服务实例是否存在，不存在则新增
        ClusterServiceInstanceEntity clusterServiceInstance = serviceInstanceService.getServiceInstanceByClusterIdAndServiceName(serviceRoleInfo.getClusterId(), serviceRoleInfo.getParentName());
        if (Objects.isNull(clusterServiceInstance)) {
            clusterServiceInstance = new ClusterServiceInstanceEntity();
            clusterServiceInstance.setClusterId(serviceRoleInfo.getClusterId());
            clusterServiceInstance.setServiceName(serviceRoleInfo.getParentName());
            clusterServiceInstance.setServiceState(ServiceState.RUNNING);
            clusterServiceInstance.setCreateTime(new Date());
            clusterServiceInstance.setUpdateTime(new Date());
            serviceInstanceService.save(clusterServiceInstance);
            //保存服务实例配置
            List<ServiceConfig> list =  ServiceConfigMap.get(clusterInfo.getClusterCode() + Constants.UNDERLINE + serviceRoleInfo.getParentName() + Constants.CONFIG);
            String config = JSON.toJSONString(list);
            ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig = new ClusterServiceInstanceConfigEntity();
            clusterServiceInstanceConfig.setClusterId(serviceRoleInfo.getClusterId());
            clusterServiceInstanceConfig.setServiceId(clusterServiceInstance.getId());
            clusterServiceInstanceConfig.setConfigJson(config);
            clusterServiceInstanceConfig.setConfigJsonMd5(SecureUtil.md5(config));
            clusterServiceInstanceConfig.setConfigVersion(1);
            clusterServiceInstanceConfig.setCreateTime(new Date());
            clusterServiceInstanceConfig.setUpdateTime(new Date());
            serviceInstanceConfigService.save(clusterServiceInstanceConfig);
        } else {
            clusterServiceInstance.setServiceState(ServiceState.RUNNING);
            clusterServiceInstance.setServiceStateCode(ServiceState.RUNNING.getValue());
            serviceInstanceService.updateById(clusterServiceInstance);
        }
        Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + clusterServiceInstance.getId());
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getById(roleGroupId);

        //保存服务角色实例
        ClusterServiceRoleInstanceEntity roleInstanceEntity = serviceRoleInstanceService.getOneServiceRole(serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), clusterInfo.getId());
        if (Objects.isNull(roleInstanceEntity)) {
            ClusterServiceRoleInstanceEntity roleInstance = new ClusterServiceRoleInstanceEntity();
            roleInstance.setServiceId(clusterServiceInstance.getId());
            roleInstance.setRoleType(CommonUtils.convertRoleType(serviceRoleInfo.getRoleType().getName()));
            roleInstance.setCreateTime(new Date());
            roleInstance.setHostname(serviceRoleInfo.getHostname());
            roleInstance.setClusterId(serviceRoleInfo.getClusterId());
            roleInstance.setServiceRoleName(serviceRoleInfo.getName());
            roleInstance.setServiceRoleState(ServiceRoleState.RUNNING);
            roleInstance.setUpdateTime(new Date());
            roleInstance.setServiceName(serviceRoleInfo.getParentName());
            roleInstance.setRoleGroupId(roleGroup.getId());
            roleInstance.setNeedRestart(NeedRestart.NO);
            serviceRoleInstanceService.save(roleInstance);
            if (Constants.ZKSERVER.equals(roleInstance.getServiceRoleName().toLowerCase())) {
                ClusterZkService clusterZkService = SpringTool.getApplicationContext().getBean(ClusterZkService.class);
                ClusterZk clusterZk = new ClusterZk();
                clusterZk.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
                clusterZk.setClusterId(serviceRoleInfo.getClusterId());
                clusterZk.setZkServer(roleInstance.getHostname());
                clusterZkService.save(clusterZk);
            }

            if (Objects.nonNull(serviceRoleInfo.getExternalLink())) {
                ExternalLink externalLink = serviceRoleInfo.getExternalLink();
                List<ClusterServiceRoleInstanceWebuis> list = webuisService.list(new QueryWrapper<ClusterServiceRoleInstanceWebuis>()
                        .eq(Constants.NAME, externalLink.getName() + "(" + serviceRoleInfo.getHostname() + ")"));
                if (Objects.nonNull(list) && list.size() > 0) {
                    logger.info("web ui already exists");
                } else {
                    HashMap<String, String> globalVariables = (HashMap<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterInfo.getId());
                    globalVariables.put("${host}", serviceRoleInfo.getHostname());
                    String url = PlaceholderUtils.replacePlaceholders(externalLink.getUrl(), globalVariables, Constants.REGEX_VARIABLE);
                    ClusterServiceRoleInstanceWebuis webuis = new ClusterServiceRoleInstanceWebuis();
                    webuis.setWebUrl(url);
                    webuis.setServiceInstanceId(clusterServiceInstance.getId());
                    webuis.setServiceRoleInstanceId(roleInstance.getId());
                    webuis.setName(externalLink.getName() + "(" + serviceRoleInfo.getHostname() + ")");
                    webuisService.save(webuis);
                    globalVariables.remove("${host}");
                }

            }
        }

    }

    public static void saveHostInstallInfo(StartWorkerMessage message, String clusterCode, ClusterHostService clusterHostService) {
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterHostEntity clusterHostEntity = new ClusterHostEntity();
        BeanUtil.copyProperties(message, clusterHostEntity);
        Map<String, String> hostIp = (Map<String, String>) CacheUtils.get(Constants.HOST_IP);

        ClusterInfoEntity cluster = clusterInfoService.getClusterByClusterCode(clusterCode);

        clusterHostEntity.setClusterId(cluster.getId());
        clusterHostEntity.setCheckTime(new Date());
        clusterHostEntity.setRack("/default-rack");
        clusterHostEntity.setNodeLabel("default");
        clusterHostEntity.setCreateTime(new Date());
        clusterHostEntity.setIp(hostIp.get(message.getHostname()));
        clusterHostEntity.setHostState(1);
        clusterHostEntity.setManaged(MANAGED.YES);
        clusterHostService.save(clusterHostEntity);
    }

    public static void updateCommandStateToFailed(List<String> commandIds) {
        for (String commandId : commandIds) {
            logger.info("command id is {}", commandId);
            //cancel worker and sub node
            ClusterServiceCommandHostCommandService service = SpringTool.getApplicationContext().getBean(ClusterServiceCommandHostCommandService.class);
            ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
            List<ClusterServiceCommandHostCommandEntity> hostCommandList = service.getHostCommandListByCommandId(commandId);
            for (ClusterServiceCommandHostCommandEntity hostCommandEntity : hostCommandList) {
                if (hostCommandEntity.getCommandState() == CommandState.RUNNING && hostCommandEntity.getServiceRoleType() != RoleType.MASTER) {
                    logger.info("{} host command  set to cancel", hostCommandEntity.getCommandName());
                    hostCommandEntity.setCommandState(CommandState.CANCEL);
                    hostCommandEntity.setCommandProgress(100);
                    service.updateByHostCommandId(hostCommandEntity);
                    UpdateCommandHostMessage message = new UpdateCommandHostMessage();
                    message.setCommandId(commandId);
                    message.setCommandHostId(hostCommandEntity.getCommandHostId());
                    message.setHostname(hostCommandEntity.getHostname());
                    if (hostCommandEntity.getServiceRoleType() == RoleType.MASTER) {
                        message.setServiceRoleType(ServiceRoleType.MASTER);
                    } else {
                        message.setServiceRoleType(ServiceRoleType.WORKER);
                    }
                    ActorUtils.actorSystem.scheduler().scheduleOnce(
                            FiniteDuration.apply(3L, TimeUnit.SECONDS),
                            commandActor,
                            message,
                            ActorUtils.actorSystem.dispatcher(),
                            ActorRef.noSender());
                }
            }
        }
    }

    public static void tellCommandActorResult(String serviceName, ExecuteServiceRoleCommand executeServiceRoleCommand, ServiceExecuteState state) {
        ActorRef serviceExecuteResultActor = ActorUtils.getLocalActor(ServiceExecuteResultActor.class, ActorUtils.getActorRefName(ServiceExecuteResultActor.class));

        ServiceExecuteResultMessage serviceExecuteResultMessage = new ServiceExecuteResultMessage();
        serviceExecuteResultMessage.setServiceExecuteState(state);
        serviceExecuteResultMessage.setDag(executeServiceRoleCommand.getDag());
        serviceExecuteResultMessage.setServiceName(serviceName);
        serviceExecuteResultMessage.setClusterCode(executeServiceRoleCommand.getClusterCode());
        serviceExecuteResultMessage.setServiceRoleType(executeServiceRoleCommand.getServiceRoleType());
        serviceExecuteResultMessage.setCommandType(executeServiceRoleCommand.getCommandType());
        serviceExecuteResultMessage.setDag(executeServiceRoleCommand.getDag());
        serviceExecuteResultMessage.setClusterId(executeServiceRoleCommand.getClusterId());
        serviceExecuteResultMessage.setActiveTaskList(executeServiceRoleCommand.getActiveTaskList());
        serviceExecuteResultMessage.setErrorTaskList(executeServiceRoleCommand.getErrorTaskList());
        serviceExecuteResultMessage.setReadyToSubmitTaskList(executeServiceRoleCommand.getReadyToSubmitTaskList());
        serviceExecuteResultMessage.setCompleteTaskList(executeServiceRoleCommand.getCompleteTaskList());

        serviceExecuteResultActor.tell(serviceExecuteResultMessage, ActorRef.noSender());
    }

    public static ClusterServiceCommandHostCommandEntity handleCommandResult(String hostCommandId, Boolean execResult, String execOut) {
        ClusterServiceCommandHostCommandService service = SpringTool.getApplicationContext().getBean(ClusterServiceCommandHostCommandService.class);

        ClusterServiceCommandHostCommandEntity hostCommand = service.getByHostCommandId(hostCommandId);
        hostCommand.setCommandProgress(100);
        if (execResult) {
            hostCommand.setCommandState(CommandState.SUCCESS);
            hostCommand.setResultMsg("success");
            logger.info("{} in {} success", hostCommand.getCommandName(), hostCommand.getHostname());
        } else {
            hostCommand.setCommandState(CommandState.FAILED);
            hostCommand.setResultMsg(execOut);
            logger.info("{} in {} failed", hostCommand.getCommandName(), hostCommand.getHostname());
        }
        service.updateByHostCommandId(hostCommand);
        //更新command host进度
        //更新command进度
        UpdateCommandHostMessage message = new UpdateCommandHostMessage();
        message.setExecResult(execResult);
        message.setCommandId(hostCommand.getCommandId());
        message.setCommandHostId(hostCommand.getCommandHostId());
        message.setHostname(hostCommand.getHostname());
        if (hostCommand.getServiceRoleType() == RoleType.MASTER) {
            message.setServiceRoleType(ServiceRoleType.MASTER);
        } else {
            message.setServiceRoleType(ServiceRoleType.WORKER);
        }

        ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
        ActorUtils.actorSystem.scheduler().scheduleOnce(FiniteDuration.apply(
                1L, TimeUnit.SECONDS),
                commandActor, message,
                ActorUtils.actorSystem.dispatcher(),
                ActorRef.noSender());

        return hostCommand;
    }

    public static void buildExecuteServiceRoleCommand(
            Integer clusterId,
            CommandType commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> masterRoles,
            ServiceRoleInfo workerRole,
            ActorRef serviceActor,
            ServiceRoleType serviceRoleType) {
        ExecuteServiceRoleCommand executeServiceRoleCommand = new ExecuteServiceRoleCommand(clusterId, node, masterRoles);
        executeServiceRoleCommand.setServiceRoleType(serviceRoleType);
        executeServiceRoleCommand.setCommandType(commandType);
        executeServiceRoleCommand.setDag(dag);
        executeServiceRoleCommand.setClusterCode(clusterCode);
        executeServiceRoleCommand.setClusterId(clusterId);
        executeServiceRoleCommand.setActiveTaskList(activeTaskList);
        executeServiceRoleCommand.setErrorTaskList(errorTaskList);
        executeServiceRoleCommand.setReadyToSubmitTaskList(readyToSubmitTaskList);
        executeServiceRoleCommand.setCompleteTaskList(completeTaskList);
        executeServiceRoleCommand.setWorkerRole(workerRole);
        serviceActor.tell(executeServiceRoleCommand, ActorRef.noSender());
    }

    public static ClusterServiceCommandEntity generateCommandEntity(Integer clusterId, CommandType commandType, String serviceName) {
        ClusterServiceCommandEntity commandEntity = new ClusterServiceCommandEntity();
        String commandId = IdUtil.simpleUUID();
        commandEntity.setCommandId(commandId);
        commandEntity.setClusterId(clusterId);
        commandEntity.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE)) + Constants.SPACE + serviceName);
        commandEntity.setCommandProgress(0);
        commandEntity.setCommandState(CommandState.RUNNING);
        commandEntity.setCommandType(commandType.getValue());
        commandEntity.setCreateTime(new Date());
        commandEntity.setCreateBy("admin");
        commandEntity.setServiceName(serviceName);
        return commandEntity;
    }

    public static ClusterServiceCommandHostEntity generateCommandHostEntity(String commandId, String hostname) {
        ClusterServiceCommandHostEntity commandHost = new ClusterServiceCommandHostEntity();
        String commandHostId = IdUtil.simpleUUID();
        commandHost.setCommandHostId(commandHostId);
        commandHost.setCommandId(commandId);
        commandHost.setHostname(hostname);
        commandHost.setCommandState(CommandState.RUNNING);
        commandHost.setCommandProgress(0);
        commandHost.setCreateTime(new Date());

        return commandHost;
    }

    public static ClusterServiceCommandHostCommandEntity generateCommandHostCommandEntity(CommandType commandType, String commandId, String serviceRoleName, RoleType serviceRoleType, ClusterServiceCommandHostEntity commandHost) {
        ClusterServiceCommandHostCommandEntity hostCommand = new ClusterServiceCommandHostCommandEntity();
        String hostCommandId = IdUtil.simpleUUID();
        hostCommand.setHostCommandId(hostCommandId);
        hostCommand.setServiceRoleName(serviceRoleName);
        hostCommand.setCommandHostId(commandHost.getCommandHostId());
        hostCommand.setCommandState(CommandState.RUNNING);
        hostCommand.setCommandProgress(0);
        hostCommand.setHostname(commandHost.getHostname());
        hostCommand.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE)) + Constants.SPACE + serviceRoleName);
        hostCommand.setCommandId(commandId);
        hostCommand.setCommandType(commandType.getValue());
        hostCommand.setServiceRoleType(serviceRoleType);
        hostCommand.setCreateTime(new Date());
        return hostCommand;
    }

    public static void updateServiceRoleState(CommandType commandType, String serviceRoleName, String hostname, Integer clusterId, ServiceRoleState serviceRoleState) {
        ClusterServiceRoleInstanceService serviceRoleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterServiceRoleInstanceEntity serviceRole = serviceRoleInstanceService.getOneServiceRole(serviceRoleName, hostname, clusterId);
        serviceRole.setServiceRoleState(serviceRoleState);
        serviceRole.setServiceRoleStateCode(serviceRoleState.getValue());
        if (commandType != CommandType.STOP_SERVICE) {
            serviceRole.setNeedRestart(NeedRestart.NO);
        }
        serviceRoleInstanceService.updateById(serviceRole);
    }

    public static void generateClusterVariable(Map<String, String> globalVariables, Integer clusterId, String variableName, String value) {
        ClusterVariableService variableService = SpringTool.getApplicationContext().getBean(ClusterVariableService.class);
        ClusterVariable clusterVariable = variableService.getVariableByVariableName(variableName, clusterId);
        if (Objects.nonNull(clusterVariable)) {
            logger.info("update variable {} value {} to {}", variableName, clusterVariable.getVariableValue(), value);
            clusterVariable.setVariableValue(value);
            variableService.updateById(clusterVariable);
        } else {
            ClusterVariable newClusterVariable = new ClusterVariable();
            newClusterVariable.setClusterId(clusterId);
            newClusterVariable.setVariableName(variableName);
            newClusterVariable.setVariableValue(value);
            variableService.save(newClusterVariable);
        }
        globalVariables.put(variableName, value);
    }

    public static void hdfsEcMethond(Integer serviceInstanceId, ClusterServiceRoleInstanceService roleInstanceService, TreeSet<String> list, String type, String roleName) throws Exception {

        List<ClusterServiceRoleInstanceEntity> namenodes = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                .eq(Constants.SERVICE_ID, serviceInstanceId)
                .eq(Constants.SERVICE_ROLE_NAME, roleName));

        //更新namenode节点的whitelist白名单
        for (ClusterServiceRoleInstanceEntity namenode : namenodes) {
            ActorSelection actorSelection = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + namenode.getHostname() + ":2552/user/worker/fileOperateActor");
            ActorSelection execCmdActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + namenode.getHostname() + ":2552/user/worker/executeCmdActor");
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            FileOperateCommand fileOperateCommand = new FileOperateCommand();
            fileOperateCommand.setLines(list);
            fileOperateCommand.setPath(Constants.INSTALL_PATH + "/hadoop-3.3.3/etc/hadoop/" + type);
            Future<Object> future = Patterns.ask(actorSelection, fileOperateCommand, timeout);
            ExecResult fileOperateResult = (ExecResult) Await.result(future, timeout.duration());
            if (Objects.nonNull(fileOperateResult) && fileOperateResult.getExecResult()) {
                logger.info("write {} success in namenode {}", type, namenode.getHostname());
                //刷新白名单
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                ArrayList<String> commands = new ArrayList<>();
                commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
                commands.add("dfsadmin");
                commands.add("-refreshNodes");
                command.setCommands(commands);
                Future<Object> execFuture = Patterns.ask(execCmdActor, command, timeout);
                ExecResult execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("hdfs dfsadmin -refreshNodes success at {}", namenode.getHostname());
                }
            }
        }
    }

    public static void createServiceActor(ClusterInfoEntity clusterInfo) {
        FrameServiceService frameServiceService = SpringTool.getApplicationContext().getBean(FrameServiceService.class);

        List<FrameServiceEntity> frameServiceList = frameServiceService.getAllFrameServiceByFrameCode(clusterInfo.getClusterFrame());
        for (FrameServiceEntity frameServiceEntity : frameServiceList) {
            //create service actor
            logger.info("create {} actor", clusterInfo.getClusterCode() + "-serviceActor-" + frameServiceEntity.getServiceName());
            ActorUtils.actorSystem.actorOf(Props.create(MasterServiceActor.class)
                    .withDispatcher("my-forkjoin-dispatcher"), clusterInfo.getClusterCode() + "-serviceActor-" + frameServiceEntity.getServiceName());
        }
    }

    public static String getExceptionMessage(Exception ex) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(out);
        ex.printStackTrace(pout);
        String ret = new String(out.toByteArray());
        pout.close();
        try {
            out.close();
        } catch (Exception e) {
        }
        return ret;
    }

    public static ExecResult restartService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception {
        ServiceHandler serviceStartHandler = new ServiceStartHandler();
        ServiceHandler serviceStopHandler = new ServiceStopHandler();
        if (needReConfig) {
            ServiceConfigureHandler serviceConfigureHandler = new ServiceConfigureHandler();
            serviceStopHandler.setNext(serviceConfigureHandler);
            serviceConfigureHandler.setNext(serviceStartHandler);
        } else {
            serviceStopHandler.setNext(serviceStartHandler);
        }
        return serviceStopHandler.handlerRequest(serviceRoleInfo);
    }

    public static ExecResult startService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception {
        ExecResult execResult = new ExecResult();
        if (needReConfig) {
            ServiceConfigureHandler serviceHandler = new ServiceConfigureHandler();
            ServiceHandler serviceStartHandler = new ServiceStartHandler();
            serviceHandler.setNext(serviceStartHandler);
            execResult = serviceHandler.handlerRequest(serviceRoleInfo);
        } else {
            ServiceHandler serviceStartHandler = new ServiceStartHandler();
            execResult = serviceStartHandler.handlerRequest(serviceRoleInfo);
        }
        return execResult;
    }

    public static ExecResult startInstallService(ServiceRoleInfo serviceRoleInfo) throws Exception {
        ServiceHandler serviceInstallHandler = new ServiceInstallHandler();
        ServiceHandler serviceConfigureHandler = new ServiceConfigureHandler();
        ServiceHandler serviceStartHandler = new ServiceStartHandler();
        serviceInstallHandler.setNext(serviceConfigureHandler);
        serviceConfigureHandler.setNext(serviceStartHandler);
        ExecResult execResult = serviceInstallHandler.handlerRequest(serviceRoleInfo);
        return execResult;
    }



    /**
     *@Description: 生成configFileMap
     * @param configFileMap
     * @param config
     */
    public static void generateConfigFileMap(HashMap<Generators, List<ServiceConfig>> configFileMap, ClusterServiceRoleGroupConfig config) {
        Map<JSONObject, JSONArray> map = JSONObject.parseObject(config.getConfigFileJson(), Map.class);
        for (JSONObject fileJson : map.keySet()) {
            Generators generators = fileJson.toJavaObject(Generators.class);
            List<ServiceConfig> serviceConfigs = map.get(fileJson).toJavaList(ServiceConfig.class);
            configFileMap.put(generators, serviceConfigs);
        }
    }

    public static ServiceConfig createServiceConfig(String configName,Object configValue,String type) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(configName);
        serviceConfig.setLabel(configName);
        serviceConfig.setValue(configValue);
        serviceConfig.setRequired(true);
        serviceConfig.setHidden(false);
        serviceConfig.setType(type);
        return serviceConfig;
    }

    public static ClusterInfoEntity getClusterInfo(Integer clusterId) {
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        return clusterInfo;
    }

    /**
     * 并集：左边集合与右边集合合并
     *
     * @param left
     * @param right
     * @return
     */
    public static List<ServiceConfig> addAll(List<ServiceConfig> left, List<ServiceConfig> right) {
        if (left == null) {
            return null;
        }
        if (right == null) {
            return left;
        }
        //使用LinkedList方便插入和删除
        List<ServiceConfig> res = new LinkedList<>(right);
        Set<String> set = new HashSet<>();
        //
        for (ServiceConfig item : left) {
            set.add(item.getName());
        }
        //迭代器遍历listA
        Iterator<ServiceConfig> iter = res.iterator();
        while (iter.hasNext()) {
            ServiceConfig item = iter.next();
            //如果set中包含id则remove
            if (!set.contains(item.getName())) {
                left.add(item);
            }
        }
        return left;
    }

    public static void syncUserGroupToHosts(List<ClusterHostEntity> hostList, String groupName,String operate) {
        for (ClusterHostEntity hostEntity : hostList) {
            ActorRef execCmdActor = ActorUtils.getRemoteActor(hostEntity.getHostname(), "unixGroupActor");
            ExecuteCmdCommand command = new ExecuteCmdCommand();
            ArrayList<String> commands = new ArrayList<>();
            commands.add(operate);
            commands.add(groupName);
            command.setCommands(commands);
            execCmdActor.tell(command,ActorRef.noSender());
        }
    }

    public static Map<String, ServiceConfig> translateToMap(List<ServiceConfig> list) {
        return  list.stream().collect(Collectors.toMap(ServiceConfig::getName, serviceConfig -> serviceConfig, (v1, v2) -> v1));
    }

    public static void syncUserToHosts(List<ClusterHostEntity> hostList, String username,String mainGroup,String otherGroup,String operate) {
        for (ClusterHostEntity hostEntity : hostList) {
            ActorRef execCmdActor = ActorUtils.getRemoteActor(hostEntity.getHostname(), "executeCmdActor");
            ExecuteCmdCommand command = new ExecuteCmdCommand();
            ArrayList<String> commands = new ArrayList<>();
            commands.add(operate);
            commands.add(username);
            if(StringUtils.isNotBlank(mainGroup)){
                commands.add("-g");
                commands.add(mainGroup);
            }
            if(StringUtils.isNotBlank(otherGroup)){
                commands.add("-G");
                commands.add(otherGroup);
            }
            command.setCommands(commands);
            execCmdActor.tell(command,ActorRef.noSender());
        }
    }

    public static void recoverAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterAlertHistoryService alertHistoryService = SpringTool.getApplicationContext().getBean(ClusterAlertHistoryService.class);
        ClusterAlertHistory clusterAlertHistory = alertHistoryService.getOne(new QueryWrapper<ClusterAlertHistory>()
                .eq(Constants.ALERT_TARGET_NAME, roleInstanceEntity.getServiceRoleName() + " Survive")
                .eq(Constants.CLUSTER_ID, roleInstanceEntity.getClusterId())
                .eq(Constants.HOSTNAME, roleInstanceEntity.getHostname())
                .eq(Constants.IS_ENABLED, 1));
        if (Objects.nonNull(clusterAlertHistory)) {
            clusterAlertHistory.setIsEnabled(2);
            alertHistoryService.updateById(clusterAlertHistory);
        }
        //update service role instance state
        if (roleInstanceEntity.getServiceRoleState() != ServiceRoleState.RUNNING) {
            roleInstanceEntity.setServiceRoleState(ServiceRoleState.RUNNING);
            roleInstanceService.updateById(roleInstanceEntity);
        }
    }

    public static void saveAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity, String
            alertTargetName, AlertLevel alertLevel, String alertAdvice) {
        ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterAlertHistoryService alertHistoryService = SpringTool.getApplicationContext().getBean(ClusterAlertHistoryService.class);
        ClusterServiceInstanceService serviceInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
        ClusterAlertHistory clusterAlertHistory = alertHistoryService.getOne(new QueryWrapper<ClusterAlertHistory>()
                .eq(Constants.ALERT_TARGET_NAME, alertTargetName)
                .eq(Constants.CLUSTER_ID, roleInstanceEntity.getClusterId())
                .eq(Constants.HOSTNAME, roleInstanceEntity.getHostname())
                .eq(Constants.IS_ENABLED, 1));

        ClusterServiceInstanceEntity serviceInstanceEntity = serviceInstanceService.getById(roleInstanceEntity.getServiceId());
        if (Objects.isNull(clusterAlertHistory)) {
            clusterAlertHistory = new ClusterAlertHistory();
            clusterAlertHistory.setClusterId(roleInstanceEntity.getClusterId());

            clusterAlertHistory.setAlertGroupName(roleInstanceEntity.getServiceName().toLowerCase());
            clusterAlertHistory.setAlertTargetName(alertTargetName);
            clusterAlertHistory.setCreateTime(new Date());
            clusterAlertHistory.setUpdateTime(new Date());
            clusterAlertHistory.setAlertLevel(alertLevel);
            clusterAlertHistory.setAlertInfo("");
            clusterAlertHistory.setAlertAdvice(alertAdvice);
            clusterAlertHistory.setHostname(roleInstanceEntity.getHostname());
            clusterAlertHistory.setServiceRoleInstanceId(roleInstanceEntity.getId());
            clusterAlertHistory.setServiceInstanceId(roleInstanceEntity.getServiceId());
            clusterAlertHistory.setIsEnabled(1);

            clusterAlertHistory.setServiceInstanceId(roleInstanceEntity.getServiceId());

            alertHistoryService.save(clusterAlertHistory);
        }
        //update service role instance state
        serviceInstanceEntity.setServiceState(ServiceState.EXISTS_EXCEPTION);
        roleInstanceEntity.setServiceRoleState(ServiceRoleState.STOP);
        if (alertLevel == AlertLevel.WARN) {
            serviceInstanceEntity.setServiceState(ServiceState.EXISTS_ALARM);
            roleInstanceEntity.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
        }
        serviceInstanceService.updateById(serviceInstanceEntity);
        roleInstanceService.updateById(roleInstanceEntity);

    }
}
