package com.datasophon.api.master.handler.service;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceStopHandler extends ServiceHandler{
    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        //停止
        ServiceRoleOperateCommand serviceRoleOperateCommand = new ServiceRoleOperateCommand();
        serviceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());
        serviceRoleOperateCommand.setStopRunner(serviceRoleInfo.getStopRunner());
        serviceRoleOperateCommand.setStatusRunner(serviceRoleInfo.getStatusRunner());
        serviceRoleOperateCommand.setRunAs(serviceRoleInfo.getRunAs());
        serviceRoleOperateCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        if(serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT){
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if(Objects.nonNull(getNext())){
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }
        ActorSelection stopActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + serviceRoleInfo.getHostname() + ":2552/user/worker/stopServiceActor");
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(stopActor, serviceRoleOperateCommand, timeout);
        ExecResult execResult = (ExecResult) Await.result(startFuture, timeout.duration());
        if (Objects.nonNull(execResult) && execResult.getExecResult()) {
            //角色安装成功
            if(Objects.nonNull(getNext())){
                return getNext().handlerRequest(serviceRoleInfo);
            }
        }
        return execResult;
    }
}
