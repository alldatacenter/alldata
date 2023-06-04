package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.ClusterServiceCommandHostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.common.utils.Result;



/**
 * 集群服务操作指令主机表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@RestController
@RequestMapping("api/cluster/service/command/host")
public class ClusterServiceCommandHostController {
    @Autowired
    private ClusterServiceCommandHostService clusterServiceCommandHostService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId ,String commandId,Integer page,Integer pageSize){
        return clusterServiceCommandHostService.getCommandHostList(clusterId,commandId,page,pageSize);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterServiceCommandHostEntity clusterServiceCommandHost = clusterServiceCommandHostService.getById(id);

        return Result.success().put("clusterServiceCommandHost", clusterServiceCommandHost);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceCommandHostEntity clusterServiceCommandHost){
        clusterServiceCommandHostService.save(clusterServiceCommandHost);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceCommandHostEntity clusterServiceCommandHost){
        clusterServiceCommandHostService.updateById(clusterServiceCommandHost);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterServiceCommandHostService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
