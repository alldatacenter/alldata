package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;

/**
 * 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-08-16 16:56:00
 */
@RestController
@RequestMapping("cluster/service/instance/role/group")
public class ClusterServiceInstanceRoleGroupController {
    @Autowired
    private ClusterServiceInstanceRoleGroupService clusterServiceInstanceRoleGroupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer serviceInstanceId){
        List<ClusterServiceInstanceRoleGroup> list = clusterServiceInstanceRoleGroupService.list(new QueryWrapper<ClusterServiceInstanceRoleGroup>()
                .eq(Constants.SERVICE_INSTANCE_ID, serviceInstanceId));
        return Result.success(list);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = clusterServiceInstanceRoleGroupService.getById(id);

        return Result.success().put("clusterServiceInstanceRoleGroup", clusterServiceInstanceRoleGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(Integer serviceInstanceId,Integer roleGroupId,String roleGroupName){
        clusterServiceInstanceRoleGroupService.saveRoleGroup(serviceInstanceId,roleGroupId,roleGroupName);
        return Result.success();
    }

    /**
     * 分配角色组
     */
    @RequestMapping("/bind")
    public Result bind(String roleInstanceIds,Integer roleGroupId){
        clusterServiceInstanceRoleGroupService.bind(roleInstanceIds,roleGroupId);
        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/rename")
    public Result update(Integer roleGroupId,String roleGroupName){

        return clusterServiceInstanceRoleGroupService.rename(roleGroupId,roleGroupName);
        
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(Integer roleGroupId){
//        clusterServiceInstanceRoleGroupService.removeByIds(Arrays.asList(ids));

        return clusterServiceInstanceRoleGroupService.deleteRoleGroup(roleGroupId);
    }

}
