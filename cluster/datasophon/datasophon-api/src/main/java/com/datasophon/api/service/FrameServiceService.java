package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.FrameServiceEntity;

import java.util.List;

/**
 * 集群框架版本服务表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface FrameServiceService extends IService<FrameServiceEntity> {

    Result getAllFrameService(Integer clusterId);

    Result getServiceListByServiceIds(List<Integer> serviceIds);

    FrameServiceEntity getServiceByFrameIdAndServiceName(Integer id, String serviceName);

    FrameServiceEntity getServiceByFrameCodeAndServiceName(String clusterFrame, String serviceName);

    List<FrameServiceEntity> getAllFrameServiceByFrameCode(String clusterFrame);

    List<FrameServiceEntity> listServices(String serviceIds);
}

