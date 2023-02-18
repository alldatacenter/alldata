package com.datasophon.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceCommandHostMapper;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;

import java.util.List;


@Service("clusterServiceCommandHostService")
public class ClusterServiceCommandHostServiceImpl extends ServiceImpl<ClusterServiceCommandHostMapper, ClusterServiceCommandHostEntity> implements ClusterServiceCommandHostService {

    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;

    @Autowired
    private ClusterServiceCommandHostMapper hostMapper;

    @Override
    public Result getCommandHostList(Integer clusterId, String commandId, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterServiceCommandHostEntity> list = this.list(new QueryWrapper<ClusterServiceCommandHostEntity>()
                .eq(Constants.COMMAND_ID, commandId)
                .orderByDesc(Constants.CREATE_TIME)
                .last("limit " + offset + "," + pageSize));
        int total = this.count(new QueryWrapper<ClusterServiceCommandHostEntity>()
                .eq(Constants.COMMAND_ID, commandId));
        for (ClusterServiceCommandHostEntity commandHostEntity : list) {
            commandHostEntity.setCommandStateCode(commandHostEntity.getCommandState().getValue());
        }
        return Result.success(list).put(Constants.TOTAL, total);
    }

    @Override
    public Integer getCommandHostSizeByCommandId(String commandId) {
        return this.count(new QueryWrapper<ClusterServiceCommandHostEntity>().eq(Constants.COMMAND_ID,commandId));
    }

    @Override
    public Integer getCommandHostTotalProgressByCommandId(String commandId) {
        return hostMapper.getCommandHostTotalProgressByCommandId(commandId);
    }

    @Override
    public List<ClusterServiceCommandHostEntity> findFailedCommandHost(String commandId) {
        return this.list(new QueryWrapper<ClusterServiceCommandHostEntity>()
                .eq(Constants.COMMAND_ID,commandId).eq(Constants.COMMAND_STATE, CommandState.FAILED));
    }

    @Override
    public List<ClusterServiceCommandHostEntity> findCanceledCommandHost(String commandId) {
        return this.list(new QueryWrapper<ClusterServiceCommandHostEntity>()
                .eq(Constants.COMMAND_ID,commandId).eq(Constants.COMMAND_STATE, CommandState.CANCEL));
    }

}
