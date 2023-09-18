package com.datasophon.api.master;


import akka.actor.UntypedActor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.master.handler.service.*;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class MasterServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(MasterServiceActor.class);

    @Override
    public void postStop() {

        logger.info("{} service actor stopped ", getSelf().path().toString());
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof ExecuteServiceRoleCommand) {
            ExecuteServiceRoleCommand executeServiceRoleCommand = (ExecuteServiceRoleCommand) message;

            ClusterServiceRoleGroupConfigService roleGroupConfigService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleGroupConfigService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);

            List<ServiceRoleInfo> serviceRoleInfoList = executeServiceRoleCommand.getMasterRoles();
            Collections.sort(serviceRoleInfoList);
            Integer successNum = 0;
            for (ServiceRoleInfo serviceRoleInfo : serviceRoleInfoList) {
                logger.info("{} service role size is {}", serviceRoleInfo.getName(), serviceRoleInfoList.size());
                ExecResult execResult = new ExecResult();
                Integer serviceInstanceId = serviceRoleInfo.getServiceInstanceId();
                ClusterServiceRoleInstanceEntity serviceRoleInstance = roleInstanceService.getOneServiceRole(serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), serviceRoleInfo.getClusterId());
                HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
                boolean enableRangerPlugin = isEnableRangerPlugin(serviceRoleInfo.getClusterId(),serviceRoleInfo.getParentName());
                boolean needReConfig = false;
                if (executeServiceRoleCommand.getCommandType() == CommandType.INSTALL_SERVICE) {
                    Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + serviceInstanceId);
                    ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
                    generateConfigFileMap(configFileMap, config);
                } else if (serviceRoleInstance.getNeedRestart() == NeedRestart.YES) {
                    ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(serviceRoleInstance.getRoleGroupId());
                    generateConfigFileMap(configFileMap, config);
                    needReConfig = true;
                }
                logger.info("enable ranger plugin is {}", enableRangerPlugin);
                serviceRoleInfo.setConfigFileMap(configFileMap);
                serviceRoleInfo.setEnableRangerPlugin(enableRangerPlugin);
                switch (executeServiceRoleCommand.getCommandType()) {
                    case INSTALL_SERVICE:
                        try {
                            logger.info("start to install {} int host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());

                            execResult = ProcessUtils.startInstallService(serviceRoleInfo);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                ProcessUtils.saveServiceInstallInfo(serviceRoleInfo);
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType()) && successNum == serviceRoleInfoList.size()) {
                                    logger.info("all master role has installed");
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.SUCCESS);
                                }
                                logger.info("{} install success in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                            } else {
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info("{} install failed in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                                }
                            }

                        } catch (Exception e) {
                            logger.info("{} install failed in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                            ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    case START_SERVICE:
                        try {
                            logger.info("start  {} in host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                            execResult = ProcessUtils.startService(serviceRoleInfo, needReConfig);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType()) && successNum == serviceRoleInfoList.size()) {
                                    logger.info("{} start success", serviceRoleInfo.getParentName());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.SUCCESS);
                                }
                                //update service role state is running
                                ProcessUtils.updateServiceRoleState(CommandType.START_SERVICE,
                                        serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname(),
                                        executeServiceRoleCommand.getClusterId(),
                                        ServiceRoleState.RUNNING);
                            } else {
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info("{} start failed", serviceRoleInfo.getParentName());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                                }
                            }
                        } catch (Exception e) {
                            ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    case STOP_SERVICE:
                        try {
                            logger.info("stop {} in host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                            ServiceHandler serviceStopHandler = new ServiceStopHandler();
                            execResult = serviceStopHandler.handlerRequest(serviceRoleInfo);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {//执行成功
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType()) && successNum == serviceRoleInfoList.size()) {
                                    logger.info("{} stop success", serviceRoleInfo.getParentName());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.SUCCESS);
                                }
                                //update service role state is stopped
                                ProcessUtils.updateServiceRoleState(CommandType.STOP_SERVICE,
                                        serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname(),
                                        executeServiceRoleCommand.getClusterId(),
                                        ServiceRoleState.STOP);
                            } else {

                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info("{} stop failed", serviceRoleInfo.getParentName());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                                }
                            }
                        } catch (Exception e) {
                            ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    case RESTART_SERVICE:
                        try {
                            logger.info("restart {} in host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                            execResult = ProcessUtils.restartService(serviceRoleInfo, needReConfig);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType()) && successNum == serviceRoleInfoList.size()) {
                                    logger.info("{} restart success", serviceRoleInfo.getParentName());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.SUCCESS);
                                }
                                //update service role state is running
                                ProcessUtils.updateServiceRoleState(CommandType.RESTART_SERVICE, serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), executeServiceRoleCommand.getClusterId(), ServiceRoleState.RUNNING);
                            } else {
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info("{} restart failed", serviceRoleInfo.getParentName());
                                    ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                                }
                            }
                        } catch (Exception e) {
                            ProcessUtils.tellCommandActorResult(serviceRoleInfo.getParentName(), executeServiceRoleCommand, ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    default:
                        break;
                }
                ProcessUtils.handleCommandResult(serviceRoleInfo.getHostCommandId(), execResult.getExecResult(), execResult.getExecOut());
            }
        } else {
            unhandled(message);
        }
    }

    private boolean isEnableRangerPlugin(Integer clusterId,String serviceName) {
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        if(globalVariables.containsKey("${enable"+serviceName+"Plugin}") && "true".equals(globalVariables.get("${enable"+serviceName+"Plugin}"))){
            return true;
        }
        return false;
    }

    //生成configFileMap
    private void generateConfigFileMap(HashMap<Generators, List<ServiceConfig>> configFileMap, ClusterServiceRoleGroupConfig config) {
        Map<JSONObject, JSONArray> map = JSONObject.parseObject(config.getConfigFileJson(), Map.class);
        for (JSONObject fileJson : map.keySet()) {
            Generators generators = fileJson.toJavaObject(Generators.class);
            List<ServiceConfig> serviceConfigs = map.get(fileJson).toJavaList(ServiceConfig.class);
            configFileMap.put(generators, serviceConfigs);
        }
    }

}
