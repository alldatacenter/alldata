package com.datasophon.api.service.impl;


import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.command.remote.DelUnixUserCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserGroup;
import com.datasophon.dao.mapper.ClusterUserMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;





@Service("clusterUserService")
@Transactional
public class ClusterUserServiceImpl extends ServiceImpl<ClusterUserMapper, ClusterUser> implements ClusterUserService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterUserServiceImpl.class);
    @Autowired
    private ClusterGroupService groupService;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterUserGroupService userGroupService;

    @Override
    public Result create(Integer clusterId , String username, Integer mainGroupId,String groupIds) {

        if(hasRepeatUserName(clusterId,username)){
            return Result.error(Status.DUPLICATE_USER_NAME.getMsg());
        }
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterId(clusterId);

        ClusterUser clusterUser = new ClusterUser();
        clusterUser.setUsername(username);
        clusterUser.setClusterId(clusterId);
        this.save(clusterUser);
        buildClusterUserGroup(clusterId, clusterUser.getId(), mainGroupId, 1);

        String otherGroup = null;
        if(StringUtils.isNotBlank(groupIds)){
            List<Integer> otherGroupIds = Arrays.stream(groupIds.split(",")).map(e -> Integer.parseInt(e)).collect(Collectors.toList());
            for (Integer id : otherGroupIds) {
                buildClusterUserGroup(clusterId, clusterUser.getId(), id, 2);
            }
            Collection<ClusterGroup> clusterGroups = groupService.listByIds(otherGroupIds);
            otherGroup = clusterGroups.stream().map(e -> e.getGroupName()).collect(Collectors.joining(","));
        }

        ClusterGroup mainGroup = groupService.getById(mainGroupId);
        //sync to all hosts
        for (ClusterHostEntity clusterHost : hostList) {
            ActorSelection unixUserActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + clusterHost.getHostname() + ":2552/user/worker/unixUserActor");

            CreateUnixUserCommand createUnixUserCommand = new CreateUnixUserCommand();
            createUnixUserCommand.setUsername(username);
            createUnixUserCommand.setMainGroup(mainGroup.getGroupName());
            createUnixUserCommand.setOtherGroups(otherGroup);

            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(unixUserActor, createUnixUserCommand, timeout);
            ExecResult execResult = null;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("create unix user {} success at {}", username,clusterHost.getHostname());
                } else {
                    logger.info(execResult.getExecOut());
                    throw new ServiceException(500,"create unix user "+username+" failed at "+clusterHost.getHostname());
                }
            } catch (Exception e) {
                throw new ServiceException(500,"create unix user "+username+" failed at "+clusterHost.getHostname());
            }
        }
        return Result.success();
    }

    private void buildClusterUserGroup(Integer clusterId,  Integer userId, Integer groupId,Integer userGroupType) {
        ClusterUserGroup clusterUserGroup = new ClusterUserGroup();
        clusterUserGroup.setUserId(userId);
        clusterUserGroup.setGroupId(groupId);
        clusterUserGroup.setClusterId(clusterId);
        clusterUserGroup.setUserGroupType(userGroupType);
        userGroupService.save(clusterUserGroup);
    }

    private boolean hasRepeatUserName(Integer clusterId, String username) {
        List<ClusterUser> list = this.list(new QueryWrapper<ClusterUser>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.USERNAME, username));
        if(list.size() > 0){
            return true;
        }
        return false;
    }

    @Override
    public Result listPage(Integer clusterId ,String username, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterUser> list = this.list(new QueryWrapper<ClusterUser>().like(Constants.USERNAME, username)
                .last("limit " + offset + "," + pageSize));
        for (ClusterUser clusterUser : list) {
            ClusterGroup mainGroup = userGroupService.queryMainGroup(clusterUser.getId());
            List<ClusterGroup> otherGroupList = userGroupService.listOtherGroups(clusterUser.getId());
            if(Objects.nonNull(otherGroupList) && !otherGroupList.isEmpty()){
                String otherGroups = otherGroupList.stream().map(e -> e.getGroupName()).collect(Collectors.joining(","));
                clusterUser.setOtherGroups(otherGroups);
            }
            clusterUser.setMainGroup(mainGroup.getGroupName());
        }
        int total = this.count(new QueryWrapper<ClusterUser>().like(Constants.USERNAME, username));
        return Result.success(list).put(Constants.TOTAL,total);
    }

    @Override
    public Result deleteClusterUser(Integer id) {
        ClusterUser clusterUser = this.getById(id);
        //delete user and group
        userGroupService.deleteByUser(id);
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterId(clusterUser.getClusterId());
        //sync to all hosts
        for (ClusterHostEntity clusterHost : hostList) {
            ActorSelection unixUserActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + clusterHost.getHostname() + ":2552/user/worker/unixUserActor");
            DelUnixUserCommand createUnixUserCommand = new DelUnixUserCommand();
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            createUnixUserCommand.setUsername(clusterUser.getUsername());
            Future<Object> execFuture = Patterns.ask(unixUserActor, createUnixUserCommand, timeout);
            ExecResult execResult = null;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("del unix user success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix user failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix user failed at {}", clusterHost.getHostname());
            }
        }
        this.removeById(id);
        return Result.success();
    }
}
