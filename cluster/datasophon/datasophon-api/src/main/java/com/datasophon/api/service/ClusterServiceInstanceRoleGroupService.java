package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;

import java.util.List;

/**
 * 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-08-16 16:56:00
 */
public interface ClusterServiceInstanceRoleGroupService extends IService<ClusterServiceInstanceRoleGroup> {


    ClusterServiceInstanceRoleGroup getRoleGroupByServiceInstanceId(Integer id);

    Result saveRoleGroup(Integer serviceInstanceId, Integer roleGroupId, String roleGroupName);

    void bind(String roleInstanceIds, Integer roleGroupId);

    ClusterServiceRoleGroupConfig getRoleGroupConfigByServiceId(Integer id);

    Result rename(Integer roleGroupId, String roleGroupName);

    Result deleteRoleGroup(Integer roleGroupId);

    List<ClusterServiceInstanceRoleGroup> listRoleGroupByServiceInstanceId(Integer serviceInstanceId);
}

