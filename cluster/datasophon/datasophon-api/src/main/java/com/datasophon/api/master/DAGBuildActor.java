package com.datasophon.api.master;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.service.*;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.dao.entity.*;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.StartExecuteCommandCommand;
import com.datasophon.common.command.SubmitActiveTaskNodeCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DAGBuildActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(DAGBuildActor.class);


    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof StartExecuteCommandCommand) {
            DAGGraph<String, ServiceNode, String> dag = new DAGGraph<>();

            StartExecuteCommandCommand executeCommandCommand = (StartExecuteCommandCommand) message;
            CommandType commandType = executeCommandCommand.getCommandType();
            logger.info("start execute command");

            ClusterServiceCommandService commandService = SpringTool.getApplicationContext().getBean(ClusterServiceCommandService.class);
            ClusterServiceCommandHostCommandService hostCommandService = SpringTool.getApplicationContext().getBean(ClusterServiceCommandHostCommandService.class);
            FrameServiceRoleService frameServiceRoleService = SpringTool.getApplicationContext().getBean(FrameServiceRoleService.class);
            FrameServiceService frameService = SpringTool.getApplicationContext().getBean(FrameServiceService.class);
            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);

            ClusterInfoEntity clusterInfo = clusterInfoService.getById(executeCommandCommand.getClusterId());
            List<ClusterServiceCommandEntity> commandList = commandService.list(new QueryWrapper<ClusterServiceCommandEntity>().in(Constants.COMMAND_ID, executeCommandCommand.getCommandIds()));

            ArrayList<FrameServiceEntity> frameServiceList = new ArrayList<>();
            if (ArrayUtil.isNotEmpty(commandList)) {
                for (ClusterServiceCommandEntity command : commandList) {
                    //build dag
                    List<ServiceRoleInfo> masterRoles = new ArrayList<>();
                    List<ServiceRoleInfo> elseRoles = new ArrayList<>();
                    ServiceNode serviceNode = new ServiceNode();

                    List<ClusterServiceCommandHostCommandEntity> hostCommandList = hostCommandService.getHostCommandListByCommandId(command.getCommandId());

                    FrameServiceEntity serviceEntity = frameService.getServiceByFrameCodeAndServiceName(clusterInfo.getClusterFrame(), command.getServiceName());
                    frameServiceList.add(serviceEntity);

                    serviceNode.setCommandId(command.getCommandId());
                    for (ClusterServiceCommandHostCommandEntity hostCommand : hostCommandList) {
                        logger.info("service role is {}",hostCommand.getServiceRoleName());
                        FrameServiceRoleEntity frameServiceRoleEntity = frameServiceRoleService.getServiceRoleByFrameCodeAndServiceRoleName(clusterInfo.getClusterFrame(), hostCommand.getServiceRoleName());

                        ServiceRoleInfo serviceRoleInfo = JSONObject.parseObject(frameServiceRoleEntity.getServiceRoleJson(), ServiceRoleInfo.class);
                        serviceRoleInfo.setHostname(hostCommand.getHostname());
                        serviceRoleInfo.setHostCommandId(hostCommand.getHostCommandId());
                        serviceRoleInfo.setClusterId(clusterInfo.getId());
                        serviceRoleInfo.setParentName(command.getServiceName());
                        serviceRoleInfo.setPackageName(serviceEntity.getPackageName());
                        serviceRoleInfo.setDecompressPackageName(serviceEntity.getDecompressPackageName());
                        serviceRoleInfo.setCommandType(commandType);
                        serviceRoleInfo.setServiceInstanceId(command.getServiceInstanceId());

                        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceRoleInfo.getName());
                        if (Objects.nonNull(serviceRoleHandler)) {
                            serviceRoleHandler.handlerServiceRoleInfo(serviceRoleInfo,hostCommand.getHostname());
                        }

                        if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                            masterRoles.add(serviceRoleInfo);
                        } else {
                            elseRoles.add(serviceRoleInfo);
                        }
                    }
                    serviceNode.setMasterRoles(masterRoles);
                    serviceNode.setElseRoles(elseRoles);
                    dag.addNode(command.getServiceName(), serviceNode);
                }
                //build edge
                for (FrameServiceEntity serviceEntity : frameServiceList) {
                    if (StringUtils.isNotBlank(serviceEntity.getDependencies())) {
                        for (String dependency : serviceEntity.getDependencies().split(Constants.COMMA)) {
                            if(dag.containsNode(dependency)){
                                dag.addEdge(dependency, serviceEntity.getServiceName(), false);
                            }
                        }
                    }
                }
            }

            if(commandType == CommandType.STOP_SERVICE){
                logger.info("reverse dag");
                DAGGraph<String, ServiceNode, String> reverseDagGraph = dag.getReverseDagGraph(dag);
                dag = reverseDagGraph;
            }

            Map<String, String> errorTaskList = new ConcurrentHashMap<>();
            Map<String, ServiceExecuteState> activeTaskList = new ConcurrentHashMap<>();
            Map<String, String>  readyToSubmitTaskList = new ConcurrentHashMap<>();
            Map<String, String>  completeTaskList = new ConcurrentHashMap<>();

            Collection<String> beginNode = dag.getBeginNode();
            logger.info("beginNode is {}",beginNode.toString());
            for (String node : beginNode) {
                readyToSubmitTaskList.put(node,"");
            }

            SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand = new SubmitActiveTaskNodeCommand();
            submitActiveTaskNodeCommand.setCommandType(executeCommandCommand.getCommandType());
            submitActiveTaskNodeCommand.setDag(dag);
            submitActiveTaskNodeCommand.setClusterId(clusterInfo.getId());
            submitActiveTaskNodeCommand.setActiveTaskList(activeTaskList);
            submitActiveTaskNodeCommand.setErrorTaskList(errorTaskList);
            submitActiveTaskNodeCommand.setReadyToSubmitTaskList(readyToSubmitTaskList);
            submitActiveTaskNodeCommand.setCompleteTaskList(completeTaskList);
            submitActiveTaskNodeCommand.setClusterCode(clusterInfo.getClusterCode());

            ActorRef submitTaskNodeActor = ActorUtils.getLocalActor(SubmitTaskNodeActor.class,ActorUtils.getActorRefName(SubmitTaskNodeActor.class));
            submitTaskNodeActor.tell(submitActiveTaskNodeCommand, getSelf());
        }
    }
}
