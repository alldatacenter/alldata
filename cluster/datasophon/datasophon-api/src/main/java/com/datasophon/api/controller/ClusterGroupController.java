package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("cluster/group")
public class ClusterGroupController {
    @Autowired
    private ClusterGroupService clusterGroupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(String groupName, Integer page, Integer pageSize){


        return clusterGroupService.listPage(groupName,page,pageSize);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterGroup clusterGroup = clusterGroupService.getById(id);

        return Result.success().put("clusterGroup", clusterGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(Integer clusterId, String groupName){
        return  clusterGroupService.saveClusterGroup(clusterId,groupName);
    }
    /**
     * 删除用户组
     */
    @RequestMapping("/delete")
    public Result delete(Integer id){
        return clusterGroupService.deleteUserGroup(id);
    }


    /**
     * 刷新用户组到主机
     */
    @RequestMapping("/refreshUserGroupToHost")
    public Result refreshUserGroupToHost(Integer clusterId){

        clusterGroupService.refreshUserGroupToHost(clusterId);
        
        return Result.success();
    }


}
