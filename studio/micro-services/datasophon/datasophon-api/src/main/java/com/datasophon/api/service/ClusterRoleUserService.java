package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;

import java.util.List;

/**
 * 集群角色用户中间表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface ClusterRoleUserService extends IService<ClusterRoleUserEntity> {

    boolean isClusterManager(Integer id, String clusterId);

    Result saveClusterManager(Integer clusterId, String userIds);

    List<UserInfoEntity> getAllClusterManagerByClusterId(Integer id);
}

