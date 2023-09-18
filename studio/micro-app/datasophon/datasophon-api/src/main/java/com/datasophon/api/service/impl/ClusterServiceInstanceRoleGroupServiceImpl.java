package com.datasophon.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service("clusterServiceInstanceRoleGroupService")
public class ClusterServiceInstanceRoleGroupServiceImpl extends ServiceImpl<ClusterServiceInstanceRoleGroupMapper, ClusterServiceInstanceRoleGroup> implements ClusterServiceInstanceRoleGroupService {

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Override
    public ClusterServiceInstanceRoleGroup getRoleGroupByServiceInstanceId(Integer serviceInstanceId) {
        return this.getOne(new QueryWrapper<ClusterServiceInstanceRoleGroup>()
                .eq(Constants.SERVICE_INSTANCE_ID,serviceInstanceId)
                .eq(Constants.ROLE_GROUP_TYPE,"default"));
    }

    @Override
    public Result saveRoleGroup(Integer serviceInstanceId, Integer roleGroupId, String roleGroupName) {
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        //is repeat name
        if(isRepeatRoleGroupName(serviceInstanceId,roleGroupName)){
            return Result.error(Status.REPEAT_ROLE_GROUP_NAME.getMsg());
        }
        ClusterServiceInstanceRoleGroup roleGroup = new ClusterServiceInstanceRoleGroup();
        roleGroup.setRoleGroupType("custom");
        roleGroup.setRoleGroupName(roleGroupName);
        roleGroup.setServiceName(serviceInstance.getServiceName());
        roleGroup.setServiceInstanceId(serviceInstanceId);
        roleGroup.setClusterId(serviceInstance.getClusterId());
        this.save(roleGroup);
        ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
        ClusterServiceRoleGroupConfig roleGroupConfig = new ClusterServiceRoleGroupConfig();
        BeanUtils.copyProperties(config,roleGroupConfig);
        roleGroupConfig.setConfigVersion(1);
        roleGroupConfig.setId(null);
        roleGroupConfig.setRoleGroupId(roleGroup.getId());
        roleGroupConfigService.save(roleGroupConfig);
        return Result.success();
    }

    private boolean isRepeatRoleGroupName(Integer serviceInstanceId, String roleGroupName) {
        List<ClusterServiceInstanceRoleGroup> list = this.list(new QueryWrapper<ClusterServiceInstanceRoleGroup>()
                .eq(Constants.SERVICE_INSTANCE_ID, serviceInstanceId)
                .eq(Constants.ROLE_GROUP_NAME, roleGroupName));
        if(list.size() > 0){
            return true;
        }
        return false;
    }

    @Override
    public void bind(String roleInstanceIds, Integer roleGroupId) {
        String[] ids = roleInstanceIds.split(",");
        ArrayList<ClusterServiceRoleInstanceEntity> list = new ArrayList<>();
        for (String id : ids) {
            ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceService.getById(id);
            //判断新角色组与原角色组配置是否相同，不相同则需标识该角色实例需要重启
            if(!isSameConfig(roleInstanceEntity.getRoleGroupId(),roleGroupId)){
                roleInstanceEntity.setNeedRestart(NeedRestart.YES);
            }
            roleInstanceEntity.setRoleGroupId(roleGroupId);
            list.add(roleInstanceEntity);
        }
        roleInstanceService.updateBatchById(list);
    }

    private boolean isSameConfig(Integer oldRoleGroupId, Integer newRoleGroupId) {
        ClusterServiceRoleGroupConfig oldConfig = roleGroupConfigService.getConfigByRoleGroupId(oldRoleGroupId);
        ClusterServiceRoleGroupConfig newConfig = roleGroupConfigService.getConfigByRoleGroupId(newRoleGroupId);
        if(oldConfig.getConfigJsonMd5().equals(newConfig.getConfigJsonMd5())){
            return true;
        }
        return false;
    }

    @Override
    public ClusterServiceRoleGroupConfig getRoleGroupConfigByServiceId(Integer serviceInstanceId) {
        ClusterServiceInstanceRoleGroup instanceRoleGroup = this.getOne(new QueryWrapper<ClusterServiceInstanceRoleGroup>()
                .eq(Constants.SERVICE_INSTANCE_ID, serviceInstanceId)
                .eq(Constants.ROLE_GROUP_TYPE, "default"));
        return roleGroupConfigService.getConfigByRoleGroupId(instanceRoleGroup.getId());
    }

    @Override
    public Result rename(Integer roleGroupId, String roleGroupName) {
        ClusterServiceInstanceRoleGroup roleGroup = this.getById(roleGroupId);
        if(!roleGroup.getRoleGroupName().equals(roleGroupName) && isRepeatRoleGroupName(roleGroup.getServiceInstanceId(),roleGroupName)){
            return Result.error(Status.REPEAT_ROLE_GROUP_NAME.getMsg());
        }
        roleGroup.setRoleGroupName(roleGroupName);
        this.updateById(roleGroup);
        return Result.success();
    }

    @Override
    public Result deleteRoleGroup(Integer roleGroupId) {
        if(hasRoleInstanceUse(roleGroupId)){
            return Result.error(Status.THE_CURRENT_ROLE_GROUP_BE_USING.getMsg());
        }
        this.removeById(roleGroupId);
        roleGroupConfigService.removeAllByRoleGroupId(roleGroupId);
        return Result.success();
    }

    @Override
    public List<ClusterServiceInstanceRoleGroup> listRoleGroupByServiceInstanceId(Integer serviceInstanceId) {

        return this.list(new QueryWrapper<ClusterServiceInstanceRoleGroup>().eq(Constants.SERVICE_INSTANCE_ID,serviceInstanceId));
    }

    private boolean hasRoleInstanceUse(Integer roleGroupId) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                .eq(Constants.ROLE_GROUP_ID, roleGroupId));
        if(Objects.nonNull(list) && list.size() > 0){
            return true;
        }
        return false;
    }
}
