package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.FrameServiceRoleEntity;

import java.util.List;

/**
 * 框架服务角色表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-18 14:38:53
 */
public interface FrameServiceRoleService extends IService<FrameServiceRoleEntity> {


    Result getServiceRoleList(Integer clusterId, String serviceIds, Integer serviceRoleType);

    FrameServiceRoleEntity getServiceRoleByServiceIdAndServiceRoleName(Integer id, String name);

    FrameServiceRoleEntity getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame, String serviceRoleName);

    Result getNonMasterRoleList(Integer clusterId, String serviceIds);

    Result getServiceRoleByServiceName(Integer clusterId, String serviceName);

    List<FrameServiceRoleEntity> getAllServiceRoleList(Integer frameServiceId);
}

