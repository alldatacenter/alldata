package com.datasophon.api.strategy;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;
import org.apache.commons.lang.StringUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KerberosHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handler(Integer clusterId, List<String> hosts) {

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        Integer clusterId = roleInstanceEntity.getClusterId();

        ClusterInfoEntity cluster = ProcessUtils.getClusterInfo(clusterId);
        String frameCode = cluster.getClusterFrame();

        String key = frameCode + Constants.UNDERLINE + roleInstanceEntity.getServiceName() + Constants.UNDERLINE + roleInstanceEntity.getServiceRoleName();
        ServiceRoleInfo serviceRoleInfo = ServiceRoleMap.get(key);
        ServiceInfo serviceInfo = ServiceInfoMap.get(frameCode + Constants.UNDERLINE + roleInstanceEntity.getServiceName());

        ActorSelection execCmdActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + roleInstanceEntity.getHostname() + ":2552/user/worker/executeCmdActor");
        ExecuteCmdCommand cmdCommand = new ExecuteCmdCommand();
        ArrayList<String> commandList = new ArrayList<>();
        commandList.add(serviceInfo.getDecompressPackageName() + Constants.SLASH + serviceRoleInfo.getStatusRunner().getProgram());
        commandList.addAll(serviceRoleInfo.getStatusRunner().getArgs());
        cmdCommand.setCommands(commandList);
        Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(execCmdActor, cmdCommand, timeout);
        try {
            ExecResult execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
                ProcessUtils.recoverAlert(roleInstanceEntity);
            } else {
                String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
                ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
            }
        } catch (Exception e) {
            //save alert
            String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
            ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }
    }
}
