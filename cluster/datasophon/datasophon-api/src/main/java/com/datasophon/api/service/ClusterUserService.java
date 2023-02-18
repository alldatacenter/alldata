package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterUser;


import java.util.Map;


public interface ClusterUserService extends IService<ClusterUser> {


    Result create(Integer clusterId , String username,Integer mainGroupId , String otherGroupIds);

    Result listPage(Integer clusterId, String username, Integer page, Integer pageSize);

    Result deleteClusterUser(Integer id);
}

