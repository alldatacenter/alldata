package com.datasophon.api.master.handler.service;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceStartHandler extends ServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServiceStartHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to start service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        //启动
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + serviceRoleInfo.getClusterId());
        ServiceRoleOperateCommand serviceRoleOperateCommand = new ServiceRoleOperateCommand();
        serviceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());
        serviceRoleOperateCommand.setStartRunner(serviceRoleInfo.getStartRunner());
        serviceRoleOperateCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        serviceRoleOperateCommand.setStatusRunner(serviceRoleInfo.getStatusRunner());
        serviceRoleOperateCommand.setSlave(serviceRoleInfo.isSlave());
        serviceRoleOperateCommand.setCommandType(serviceRoleInfo.getCommandType());
        serviceRoleOperateCommand.setMasterHost(serviceRoleInfo.getMasterHost());

        logger.info("service master host is {}", serviceRoleInfo.getMasterHost());

        serviceRoleOperateCommand.setEnableRangerPlugin(serviceRoleInfo.getEnableRangerPlugin());
        serviceRoleOperateCommand.setRunAs(serviceRoleInfo.getRunAs());
        Boolean enableKerberos = Boolean.parseBoolean(globalVariables.get("${enable" + serviceRoleInfo.getParentName() + "Kerberos}"));
        logger.info("{} enable kerberos is {}", serviceRoleInfo.getParentName(), enableKerberos);
        serviceRoleOperateCommand.setEnableKerberos(enableKerberos);
        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }
        ActorSelection startActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + serviceRoleInfo.getHostname() + ":2552/user/worker/startServiceActor");
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, serviceRoleOperateCommand, timeout);
        ExecResult startResult = (ExecResult) Await.result(startFuture, timeout.duration());
        if (Objects.nonNull(startResult) && startResult.getExecResult()) {
            //角色启动成功
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
        }
        return startResult;
    }
}
