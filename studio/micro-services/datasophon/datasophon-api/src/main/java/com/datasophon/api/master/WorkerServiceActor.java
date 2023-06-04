package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.master.handler.service.*;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WorkerServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof ExecuteServiceRoleCommand) {
            ExecuteServiceRoleCommand executeServiceRoleCommand = (ExecuteServiceRoleCommand) message;

            ClusterServiceRoleGroupConfigService roleGroupConfigService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleGroupConfigService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);

            ServiceRoleInfo serviceRoleInfo = executeServiceRoleCommand.getWorkerRole();
            ExecResult execResult = new ExecResult();
            Integer serviceInstanceId = serviceRoleInfo.getServiceInstanceId();
            ClusterServiceRoleInstanceEntity serviceRoleInstance = roleInstanceService.getOneServiceRole(
                    serviceRoleInfo.getName(),
                    serviceRoleInfo.getHostname(),
                    serviceRoleInfo.getClusterId());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
            boolean needReConfig = false;
            if(executeServiceRoleCommand.getCommandType() == CommandType.INSTALL_SERVICE){
                Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + serviceInstanceId);
                ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
                ProcessUtils.generateConfigFileMap(configFileMap, config);
            }else if(serviceRoleInstance.getNeedRestart() == NeedRestart.YES){
                ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(serviceRoleInstance.getRoleGroupId());
                ProcessUtils.generateConfigFileMap(configFileMap, config);
                needReConfig = true;
            }
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setEnableRangerPlugin(false);
            switch (executeServiceRoleCommand.getCommandType()) {
                case INSTALL_SERVICE:
                    try {
                        logger.info("start to install {} int host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                        execResult = ProcessUtils.startInstallService(serviceRoleInfo);
                        if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                            //install success
                            ProcessUtils.saveServiceInstallInfo(serviceRoleInfo);
                            logger.info("{} install success in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                        }
                    } catch (Exception e) {
                        logger.info("{} install failed in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                        logger.error(ProcessUtils.getExceptionMessage(e));
                    }
                    break;
                case START_SERVICE:
                    try {
                        logger.info("start  {} in host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                        execResult = ProcessUtils.startService(serviceRoleInfo, needReConfig);
                        if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                            //更新角色实例状态为正在运行
                            ProcessUtils.updateServiceRoleState(CommandType.START_SERVICE,
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname(),
                                    executeServiceRoleCommand.getClusterId(),
                                    ServiceRoleState.RUNNING);
                        }
                    } catch (Exception e) {
                        logger.error(ProcessUtils.getExceptionMessage(e));
                    }
                    break;
                case STOP_SERVICE:
                    try {
                        logger.info("stop {} in host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                        ServiceHandler serviceStopHandler = new ServiceStopHandler();
                        execResult = serviceStopHandler.handlerRequest(serviceRoleInfo);
                        if (Objects.nonNull(execResult) && execResult.getExecResult()) {//执行成功
                            //更新角色实例状态为停止
                            ProcessUtils.updateServiceRoleState(CommandType.STOP_SERVICE,
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname(),
                                    executeServiceRoleCommand.getClusterId(),
                                    ServiceRoleState.STOP);
                        }
                    } catch (Exception e) {
                        logger.error(ProcessUtils.getExceptionMessage(e));
                    }
                    break;
                case RESTART_SERVICE:
                    try {
                        logger.info("restart {} in host {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                        execResult = ProcessUtils.restartService(serviceRoleInfo,needReConfig);
                        if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                            //更新角色实例状态为正在运行
                            ProcessUtils.updateServiceRoleState(CommandType.RESTART_SERVICE,serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), executeServiceRoleCommand.getClusterId(), ServiceRoleState.RUNNING);
                        }
                    } catch (Exception e) {
                        logger.error(ProcessUtils.getExceptionMessage(e));
                    }
                    break;
                default:
                    break;
            }
            ProcessUtils.handleCommandResult(serviceRoleInfo.getHostCommandId(), execResult.getExecResult(), execResult.getExecOut());
        } else {
            unhandled(message);
        }
    }


}
