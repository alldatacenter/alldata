package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterRackService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterRack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 
 *
 * @author dygao2
 * @email dygao2@datasophon.com
 * @date 2022-11-25 11:31:59
 */
@RestController
@RequestMapping("cluster/rack")
public class ClusterRackController {
    @Autowired
    private ClusterRackService clusterRackService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId){
        List<ClusterRack> list = clusterRackService.queryClusterRack(clusterId);
        return Result.success(list);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterRack clusterRack = clusterRackService.getById(id);

        return Result.success().put("clusterRack", clusterRack);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(Integer clusterId , String rack){
        clusterRackService.saveRack(clusterId,rack);
        return Result.success();
    }



    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(Integer clusterId ,Integer rackId){
        return clusterRackService.deleteRack(rackId);
    }

}
