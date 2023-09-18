package com.datasophon.api.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import com.datasophon.api.service.ClusterServiceInstanceConfigService;
import com.datasophon.common.utils.Result;



/**
 * 集群服务角色实例配置表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@RestController
@RequestMapping("cluster/service/instance/config")
public class ClusterServiceInstanceConfigController {
    @Autowired
    private ClusterServiceInstanceConfigService clusterServiceInstanceConfigService;

    /**
     * 列表
     */
    @RequestMapping("/getConfigVersion")
    public Result getConfigVersion(Integer serviceInstanceId,Integer roleGroupId){
        return clusterServiceInstanceConfigService.getConfigVersion(serviceInstanceId,roleGroupId);
    }


    /**
     * 信息
     */
    @RequestMapping("/info")
    public Result info(Integer serviceInstanceId,Integer version,Integer roleGroupId,Integer page,Integer pageSize){
        return clusterServiceInstanceConfigService.getServiceInstanceConfig(serviceInstanceId,version,roleGroupId,page,pageSize);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig){
        clusterServiceInstanceConfigService.save(clusterServiceInstanceConfig);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig){
        clusterServiceInstanceConfigService.updateById(clusterServiceInstanceConfig);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterServiceInstanceConfigService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
