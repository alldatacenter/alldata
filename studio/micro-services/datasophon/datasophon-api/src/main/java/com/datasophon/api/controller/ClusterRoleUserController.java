package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.Map;

import com.datasophon.api.security.UserPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.api.service.ClusterRoleUserService;
import com.datasophon.common.utils.Result;


/**
 * 集群角色用户中间表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/cluster/user")
public class ClusterRoleUserController {
    @Autowired
    private ClusterRoleUserService clusterRoleUserService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(@RequestParam Map<String, Object> params){


        return Result.success();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterRoleUserEntity clusterRoleUser = clusterRoleUserService.getById(id);

        return Result.success().put("clusterRoleUser", clusterRoleUser);
    }

    /**
     * 保存
     */
    @RequestMapping("/saveClusterManager")
    @UserPermission
    public Result saveClusterManager(Integer clusterId, String userIds){
        return clusterRoleUserService.saveClusterManager(clusterId,userIds);
    }
    

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterRoleUserEntity clusterRoleUser){
        clusterRoleUserService.updateById(clusterRoleUser);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterRoleUserService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
