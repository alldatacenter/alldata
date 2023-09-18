package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.Map;

import com.datasophon.api.service.ClusterUserService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("cluster/user")
public class ClusterUserController {
    @Autowired
    private ClusterUserService clusterUserService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId, String username, Integer page, Integer pageSize){


        return clusterUserService.listPage(clusterId ,username,page,pageSize);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterUser clusterUser = clusterUserService.getById(id);

        return Result.success().put("clusterUser", clusterUser);
    }

    /**
     * 保存
     */
    @RequestMapping("/create")
    public Result save(Integer clusterId ,String username,Integer mainGroupId,String otherGroupIds){

        return clusterUserService.create(clusterId,username,mainGroupId,otherGroupIds);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterUser clusterUser){

        clusterUserService.updateById(clusterUser);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(Integer id){
        return clusterUserService.deleteClusterUser(id);
    }

}
