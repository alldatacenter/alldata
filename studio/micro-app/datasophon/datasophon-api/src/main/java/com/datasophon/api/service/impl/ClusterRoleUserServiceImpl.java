package com.datasophon.api.service.impl;

import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.enums.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterRoleUserMapper;
import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.api.service.ClusterRoleUserService;


@Service("clusterRoleUserService")
public class ClusterRoleUserServiceImpl extends ServiceImpl<ClusterRoleUserMapper, ClusterRoleUserEntity> implements ClusterRoleUserService {

    @Autowired
    private ClusterRoleUserMapper clusterRoleUserMapper;

    @Override
    public boolean isClusterManager(Integer userId, String clusterId) {
        List<ClusterRoleUserEntity> list = this.list(new QueryWrapper<ClusterRoleUserEntity>()
                .eq(Constants.DETAILS_USER_ID, userId)
                .eq(Constants.CLUSTER_ID, clusterId));
        if(Objects.nonNull(list) && list.size() == 1){
            return true;
        }
        return false;
    }

    @Override
    public Result saveClusterManager(Integer clusterId, String userIds) {
        this.remove(new QueryWrapper<ClusterRoleUserEntity>().eq(Constants.CLUSTER_ID,clusterId));
        ArrayList<ClusterRoleUserEntity> list = new ArrayList<>();
        for (String userId : userIds.split(",")) {
            Integer id = Integer.parseInt(userId);
            ClusterRoleUserEntity clusterRoleUserEntity = new ClusterRoleUserEntity();
            clusterRoleUserEntity.setClusterId(clusterId);
            clusterRoleUserEntity.setUserId(id);
            clusterRoleUserEntity.setUserType(UserType.CLUSTER_MANAGER);
            list.add(clusterRoleUserEntity);
        }
        this.saveBatch(list);
        return Result.success();
    }

    @Override
    public List<UserInfoEntity> getAllClusterManagerByClusterId(Integer clusterId) {
        return clusterRoleUserMapper.getAllClusterManagerByClusterId(clusterId);
    }
}
