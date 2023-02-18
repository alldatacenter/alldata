package com.datasophon.api.service.impl;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GetLogCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.enums.RoleType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceCommandHostCommandMapper;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


@Service("clusterServiceCommandHostCommandService")
public class ClusterServiceCommandHostCommandServiceImpl extends ServiceImpl<ClusterServiceCommandHostCommandMapper, ClusterServiceCommandHostCommandEntity> implements ClusterServiceCommandHostCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceCommandHostCommandServiceImpl.class);

    @Autowired
    ClusterServiceCommandHostCommandMapper hostCommandMapper;

    @Autowired
    FrameServiceRoleService frameServiceRoleService;

    @Autowired
    FrameServiceService frameService;

    @Autowired
    ClusterInfoService clusterInfoService;


    @Override
    public Result getHostCommandList(String hostname, String commandHostId, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterServiceCommandHostCommandEntity> list = this.list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.COMMAND_HOST_ID, commandHostId)
                .orderByDesc(Constants.CREATE_TIME)
                .last("limit " + offset + "," + pageSize));
        int total = this.count(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.COMMAND_HOST_ID, commandHostId));
        for (ClusterServiceCommandHostCommandEntity hostCommandEntity : list) {
            hostCommandEntity.setCommandStateCode(hostCommandEntity.getCommandState().getValue());
        }
        return Result.success(list).put(Constants.TOTAL, total);
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> getHostCommandListByCommandId(String commandId) {
        return this.list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>().eq(Constants.COMMAND_ID, commandId));
    }

    @Override
    public ClusterServiceCommandHostCommandEntity getByHostCommandId(String hostCommandId) {
        return this.getOne(new QueryWrapper<ClusterServiceCommandHostCommandEntity>().eq(Constants.HOST_COMMAND_ID, hostCommandId));
    }

    @Override
    public void updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand) {
        this.update(hostCommand, new QueryWrapper<ClusterServiceCommandHostCommandEntity>().eq(Constants.HOST_COMMAND_ID, hostCommand.getHostCommandId()));
    }

    @Override
    public Integer getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId) {
        int size = this.count(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOSTNAME, hostname).eq(Constants.COMMAND_HOST_ID, commandHostId));
        return size;
    }

    @Override
    public Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId) {
        return hostCommandMapper.getHostCommandTotalProgressByHostnameAndCommandHostId(hostname, commandHostId);
    }

    @Override
    public Result getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        ClusterServiceCommandHostCommandEntity hostCommand = this.getOne(new QueryWrapper<ClusterServiceCommandHostCommandEntity>().eq(Constants.HOST_COMMAND_ID, hostCommandId));
        FrameServiceRoleEntity serviceRole = frameServiceRoleService.getServiceRoleByFrameCodeAndServiceRoleName(clusterInfo.getClusterFrame(), hostCommand.getServiceRoleName());
        Map<String, String> globalVariables = (Map<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        if(serviceRole.getServiceRoleType() == RoleType.CLIENT){
            return Result.success("client does not have any log");
        }
        FrameServiceEntity frameServiceEntity = frameService.getById(serviceRole.getServiceId());
        String logFile = serviceRole.getLogFile();
        if(StringUtils.isNotBlank(logFile)){
            logFile = PlaceholderUtils.replacePlaceholders(logFile, globalVariables, Constants.REGEX_VARIABLE);
            logger.info("logFile is {}",logFile);
        }
        GetLogCommand command = new GetLogCommand();
        command.setLogFile(logFile);
        command.setDecompressPackageName(frameServiceEntity.getDecompressPackageName());
        logger.info("start to get {} log from {}", serviceRole.getServiceRoleName(), hostCommand.getHostname());
        ActorSelection configActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + hostCommand.getHostname() + ":2552/user/worker/logActor");
        Timeout timeout = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        Future<Object> logFuture = Patterns.ask(configActor, command, timeout);
        ExecResult logResult = (ExecResult) Await.result(logFuture, timeout.duration());
        if (Objects.nonNull(logResult) && logResult.getExecResult()) {
            return Result.success(logResult.getExecOut());
        }
        return Result.success();
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> findFailedHostCommand(String hostname, String commandHostId) {
        return this.list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOSTNAME, hostname)
                .eq(Constants.COMMAND_HOST_ID, commandHostId)
                .eq(Constants.COMMAND_STATE, CommandState.FAILED));
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> findCanceledHostCommand(String hostname, String commandHostId) {
        return this.list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOSTNAME, hostname)
                .eq(Constants.COMMAND_HOST_ID, commandHostId)
                .eq(Constants.COMMAND_STATE, CommandState.CANCEL));
    }
}
