package com.datasophon.api.master.handler.service;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.io.FileUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceInstallHandler extends ServiceHandler{
    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallHandler.class);
    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterHostService clusterHostService = SpringTool.getApplicationContext().getBean(ClusterHostService.class);
        ClusterServiceRoleInstanceEntity serviceRole = roleInstanceService.getOneServiceRole(serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), serviceRoleInfo.getClusterId());
        ClusterHostEntity hostEntity = clusterHostService.getClusterHostByHostname(serviceRoleInfo.getHostname());
        if(Objects.nonNull(serviceRole) ){
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            execResult.setExecOut("already installed");
            return execResult;
        }
        InstallServiceRoleCommand installServiceRoleCommand = new InstallServiceRoleCommand();
        installServiceRoleCommand.setServiceName(serviceRoleInfo.getParentName());
        installServiceRoleCommand.setServiceRoleName(serviceRoleInfo.getName());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setPackageName(serviceRoleInfo.getPackageName());
        installServiceRoleCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        installServiceRoleCommand.setRunAs(serviceRoleInfo.getRunAs());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        String md5 = FileUtil.readString(Constants.MASTER_MANAGE_PACKAGE_PATH+Constants.SLASH + serviceRoleInfo.getPackageName() + ".md5", Charset.defaultCharset());
        installServiceRoleCommand.setPackageMd5(md5);
        if("aarch64".equals(hostEntity.getCpuArchitecture()) && FileUtil.exist(Constants.MASTER_MANAGE_PACKAGE_PATH+Constants.SLASH +serviceRoleInfo.getDecompressPackageName()+"-arm.tar.gz")){
            installServiceRoleCommand.setPackageName(serviceRoleInfo.getDecompressPackageName()+"-arm.tar.gz");
            logger.info("find arm package {}",installServiceRoleCommand.getPackageName());
            String armMd5 = FileUtil.readString(Constants.MASTER_MANAGE_PACKAGE_PATH+Constants.SLASH + serviceRoleInfo.getDecompressPackageName() + "-arm.tar.gz.md5", Charset.defaultCharset());
            installServiceRoleCommand.setPackageMd5(armMd5);
        }
        ActorSelection actorSelection = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + serviceRoleInfo.getHostname() + ":2552/user/worker/installServiceActor");
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> future = Patterns.ask(actorSelection, installServiceRoleCommand, timeout);
        ExecResult installResult = (ExecResult) Await.result(future, timeout.duration());
        if(Objects.nonNull(installResult) && installResult.getExecResult()){
            if( Objects.nonNull(getNext())){
                return getNext().handlerRequest(serviceRoleInfo);
            }
        }
        return installResult;

    }
}
