package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserGroup;

import java.util.List;


public interface ClusterUserGroupService extends IService<ClusterUserGroup> {


    Integer countGroupUserNum(Integer id);

    void deleteByUser(Integer id);

    ClusterGroup queryMainGroup(Integer id);

    List<ClusterGroup> listOtherGroups(Integer userId);

    List<ClusterUser> listClusterUsers(Integer groupId);
}

