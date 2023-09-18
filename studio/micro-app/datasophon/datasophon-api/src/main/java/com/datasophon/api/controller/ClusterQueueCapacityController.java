package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 *
 * @author dygao2
 * @email dygao2@datasophon.com
 * @date 2022-11-25 14:30:11
 */
@RestController
@RequestMapping("cluster/queue/capacity")
public class ClusterQueueCapacityController {

    @Autowired
    private ClusterQueueCapacityService clusterQueueCapacityService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId){

        return clusterQueueCapacityService.listCapacityQueue(clusterId);

    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterQueueCapacity clusterQueueCapacity = clusterQueueCapacityService.getById(id);

        return Result.success().put("clusterQueueCapacity", clusterQueueCapacity);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterQueueCapacity clusterQueueCapacity){
        clusterQueueCapacityService.save(clusterQueueCapacity);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterQueueCapacity clusterQueueCapacity){

        clusterQueueCapacityService.updateById(clusterQueueCapacity);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete( Integer id){
        clusterQueueCapacityService.removeById(id);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/refreshToYarn")
    public Result refreshToYarn( Integer clusterId) throws Exception {
        return clusterQueueCapacityService.refreshToYarn(clusterId);
    }
}
