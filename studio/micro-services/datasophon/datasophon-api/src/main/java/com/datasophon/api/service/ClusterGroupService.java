package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterGroup;


public interface ClusterGroupService extends IService<ClusterGroup> {


    Result saveClusterGroup(Integer clusterId, String groupName) ;

    void refreshUserGroupToHost(Integer clusterId);

    Result deleteUserGroup(Integer id);

    Result listPage(String groupName, Integer page, Integer pageSize);
}

