package com.datasophon.api.service.impl;

import com.datasophon.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterZkMapper;
import com.datasophon.dao.entity.ClusterZk;
import com.datasophon.api.service.ClusterZkService;


@Service("clusterZkService")
public class ClusterZkServiceImpl extends ServiceImpl<ClusterZkMapper, ClusterZk> implements ClusterZkService {

    @Autowired
    private ClusterZkMapper clusterZkMapper;

    @Override
    public Integer getMaxMyId(Integer clusterId) {
        return clusterZkMapper.getMaxMyId(clusterId);
    }

    @Override
    public List<ClusterZk> getAllZkServer(Integer clusterId) {
        return this.list(new QueryWrapper<ClusterZk>().eq(Constants.CLUSTER_ID,clusterId));
    }
}
