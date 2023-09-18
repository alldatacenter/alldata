package com.datasophon.api.service.impl;

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.mapper.FrameInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.FrameServiceMapper;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.api.service.FrameServiceService;


@Service("frameServiceService")
public class FrameServiceServiceImpl extends ServiceImpl<FrameServiceMapper, FrameServiceEntity> implements FrameServiceService {
    @Autowired
    ClusterInfoService clusterInfoService;

    @Autowired
    FrameInfoMapper frameInfoMapper;

    @Autowired
    ClusterServiceInstanceService serviceInstanceService;

    @Override
    public Result getAllFrameService(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.getClusterFrame());
        List<FrameServiceEntity> list = this.list(new QueryWrapper<FrameServiceEntity>()
                .eq(Constants.FRAME_ID, frameInfo.getId())
                .orderByAsc(Constants.SORT_NUM));
        setInstalled(clusterId, list);
        return Result.success(list);
    }

    private void setInstalled(Integer clusterId, List<FrameServiceEntity> list) {
        for (FrameServiceEntity serviceEntity : list) {
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getServiceInstanceByClusterIdAndServiceName(clusterId, serviceEntity.getServiceName());
            if(Objects.nonNull(serviceInstance) && !serviceInstance.getServiceState().equals(ServiceState.WAIT_INSTALL)){
                serviceEntity.setInstalled(true);
            }else{
                serviceEntity.setInstalled(false);
            }
        }
    }

    @Override
    public Result getServiceListByServiceIds(List<Integer> serviceIds) {
        Collection<FrameServiceEntity> list = this.listByIds(serviceIds);
        return Result.success(list);
    }

    @Override
    public FrameServiceEntity getServiceByFrameIdAndServiceName(Integer frameId, String serviceName) {
        return this.getOne(new QueryWrapper<FrameServiceEntity>()
                .eq(Constants.FRAME_ID,frameId)
                .eq(Constants.SERVICE_NAME,serviceName));
    }

    @Override
    public FrameServiceEntity getServiceByFrameCodeAndServiceName(String clusterFrame, String serviceName) {
        return this.getOne(new QueryWrapper<FrameServiceEntity>()
                .eq(Constants.FRAME_CODE_1,clusterFrame)
                .eq(Constants.SERVICE_NAME,serviceName));
    }

    @Override
    public List<FrameServiceEntity> getAllFrameServiceByFrameCode(String clusterFrame) {
        return this.list(new QueryWrapper<FrameServiceEntity>().eq(Constants.FRAME_CODE_1,clusterFrame));
    }

    @Override
    public List<FrameServiceEntity> listServices(String serviceIds) {
        List<String> ids = Arrays.stream(serviceIds.split(",")).collect(Collectors.toList());
        return this.list(new QueryWrapper<FrameServiceEntity>().in(Constants.ID,ids));
    }


}
