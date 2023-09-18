package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.ClusterServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.common.utils.Result;



/**
 * 集群服务表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@RestController
@RequestMapping("cluster/service/instance")
public class ClusterServiceInstanceController {
    @Autowired
    private ClusterServiceInstanceService clusterServiceInstanceService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId){
        return clusterServiceInstanceService.listAll(clusterId);
    }

    /**
     * 获取服务角色类型列表
     */
    @RequestMapping("/getServiceRoleType")
    public Result getServiceRoleType(Integer serviceInstanceId){
        return clusterServiceInstanceService.getServiceRoleType(serviceInstanceId);
    }


    /**
     * 获取服务角色类型列表
     */
    @RequestMapping("/configVersionCompare")
    public Result configVersionCompare(Integer serviceInstanceId,Integer roleGroupId){
        return clusterServiceInstanceService.configVersionCompare(serviceInstanceId,roleGroupId);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterServiceInstanceEntity clusterServiceInstance = clusterServiceInstanceService.getById(id);

        return Result.success().put("clusterServiceInstance", clusterServiceInstance);
    }

    /**
     * 信息
     */
    @RequestMapping("/downloadClientConfig")
    public Result downloadClientConfig(Integer clusterId,String serviceName){

        return clusterServiceInstanceService.downloadClientConfig(clusterId,serviceName);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceInstanceEntity clusterServiceInstance){
        clusterServiceInstanceService.save(clusterServiceInstance);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceInstanceEntity clusterServiceInstance){
        clusterServiceInstanceService.updateById(clusterServiceInstance);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(Integer serviceInstanceId){
        return clusterServiceInstanceService.delServiceInstance(serviceInstanceId);
    }

}
