package com.datasophon.api.service.impl;


import akka.actor.ActorRef;
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
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.command.remote.CreateUnixGroupCommand;
import com.datasophon.common.command.remote.DelUnixGroupCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.mapper.ClusterGroupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("clusterGroupService")
@Transactional
public class ClusterGroupServiceImpl extends ServiceImpl<ClusterGroupMapper, ClusterGroup> implements ClusterGroupService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterGroupServiceImpl.class);

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterUserGroupService userGroupService;

    @Override
    public Result saveClusterGroup(Integer clusterId, String groupName)  {
        //判读groupName是否重复
        if(hasRepeatGroupName(clusterId,groupName)){
            return Result.error(Status.GROUP_NAME_DUPLICATION.getMsg());
        }
        ClusterGroup clusterGroup = new ClusterGroup();
        clusterGroup.setClusterId(clusterId);
        clusterGroup.setGroupName(groupName);
        this.save(clusterGroup);

        List<ClusterHostEntity> hostList = hostService.getHostListByClusterId(clusterId);
        for (ClusterHostEntity clusterHost : hostList) {
            ActorRef unixGroupActor = ActorUtils.getRemoteActor(clusterHost.getHostname(), "unixGroupActor");
            CreateUnixGroupCommand createUnixGroupCommand = new CreateUnixGroupCommand();
            createUnixGroupCommand.setGroupName(groupName);
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(unixGroupActor, createUnixGroupCommand, timeout);
            ExecResult execResult = null;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("create unix group success at {}", clusterHost.getHostname());
                } else {
                    logger.info(execResult.getExecOut());
                    throw new ServiceException(500,"create unix group "+groupName+" failed at "+clusterHost.getHostname());
                }
            } catch (Exception e) {
                throw new ServiceException(500,"create unix group "+groupName+" failed at "+clusterHost.getHostname());
            }
        }

        return Result.success();
    }

    private boolean hasRepeatGroupName(Integer clusterId, String groupName) {
        List<ClusterGroup> list = this.list(new QueryWrapper<ClusterGroup>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.GROUP_NAME, groupName));
        if(list.size() > 0){
            return true;
        }
        return false;
    }

    @Override
    public void refreshUserGroupToHost(Integer clusterId) {
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterId(clusterId);
        List<ClusterGroup> groupList = this.list();
        for (ClusterGroup clusterGroup : groupList) {
            ProcessUtils.syncUserGroupToHosts(hostList, clusterGroup.getGroupName(),"groupadd");
        }
    }



    @Override
    public Result deleteUserGroup(Integer id) {
        ClusterGroup clusterGroup = this.getById(id);
        Integer num = userGroupService.countGroupUserNum(id);
        if(num > 0){
            return Result.error(Status.USER_GROUP_TIPS_ONE.getMsg());
        }
        this.removeById(id);
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterId(clusterGroup.getClusterId());
        for (ClusterHostEntity clusterHost : hostList) {
            ActorRef unixGroupActor = ActorUtils.getRemoteActor(clusterHost.getHostname(), "unixGroupActor");
            DelUnixGroupCommand delUnixGroupCommand = new DelUnixGroupCommand();
            delUnixGroupCommand.setGroupName(clusterGroup.getGroupName());
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(unixGroupActor, delUnixGroupCommand, timeout);
            ExecResult execResult = null;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("del unix group success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix group failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix group failed at {}", clusterHost.getHostname());
            }
        }
        return Result.success();
    }

    @Override
    public Result listPage(String groupName, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterGroup> list = this.list(new QueryWrapper<ClusterGroup>().like(Constants.GROUP_NAME, groupName)
                .last("limit " + offset + "," + pageSize));
        for (ClusterGroup clusterGroup : list) {
            List<ClusterUser> clusterUserList = userGroupService.listClusterUsers(clusterGroup.getId());
            if(Objects.nonNull(clusterUserList) && !clusterUserList.isEmpty()){
                String clusterUsers = clusterUserList.stream().map(e -> e.getUsername()).collect(Collectors.joining(","));
                clusterGroup.setClusterUsers(clusterUsers);
            }
        }
        int total = this.count(new QueryWrapper<ClusterGroup>().like(Constants.GROUP_NAME, groupName));
        return Result.success(list).put(Constants.TOTAL,total);
    }
}
