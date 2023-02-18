package com.datasophon.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceRoleInstanceWebuisMapper;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;

import java.util.List;


@Service("clusterServiceRoleInstanceWebuisService")
public class ClusterServiceRoleInstanceWebuisServiceImpl extends ServiceImpl<ClusterServiceRoleInstanceWebuisMapper, ClusterServiceRoleInstanceWebuis> implements ClusterServiceRoleInstanceWebuisService {


    @Override
    public Result getWebUis(Integer serviceInstanceId) {
        List<ClusterServiceRoleInstanceWebuis> list = this.list(new QueryWrapper<ClusterServiceRoleInstanceWebuis>().eq(Constants.SERVICE_INSTANCE_ID, serviceInstanceId));
        return Result.success(list);
    }

    @Override
    public void removeByServiceInsId(Integer serviceInstanceId) {
        this.remove(new QueryWrapper<ClusterServiceRoleInstanceWebuis>().eq(Constants.SERVICE_INSTANCE_ID,serviceInstanceId));
    }
}
