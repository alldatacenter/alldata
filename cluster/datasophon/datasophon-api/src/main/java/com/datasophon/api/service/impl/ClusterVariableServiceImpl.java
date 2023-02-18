package com.datasophon.api.service.impl;

import com.datasophon.common.Constants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterVariableMapper;
import com.datasophon.dao.entity.ClusterVariable;
import com.datasophon.api.service.ClusterVariableService;


@Service("clusterVariableService")
public class ClusterVariableServiceImpl extends ServiceImpl<ClusterVariableMapper, ClusterVariable> implements ClusterVariableService {


    @Override
    public ClusterVariable getVariableByVariableName(String variableName, Integer clusterId) {
        List<ClusterVariable> list = this.list(new QueryWrapper<ClusterVariable>().eq(Constants.VARIABLE_NAME, variableName).eq(Constants.CLUSTER_ID, clusterId));
        if(Objects.nonNull(list) && list.size() >= 1){
            return list.get(0);
        }
        return null;
    }
}
