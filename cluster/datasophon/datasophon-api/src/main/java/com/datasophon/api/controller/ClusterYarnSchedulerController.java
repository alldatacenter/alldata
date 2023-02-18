package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterYarnScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 
 *
 * @author dygao2
 * @email dygao2@datasophon.com
 * @date 2022-11-25 15:02:11
 */
@RestController
@RequestMapping("cluster/yarn/scheduler")
public class ClusterYarnSchedulerController {
    @Autowired
    private ClusterYarnSchedulerService clusterYarnSchedulerService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(){


        return Result.success();
    }


    /**
     * 信息
     */
    @RequestMapping("/info")
    public Result info(Integer clusterId){
        ClusterYarnScheduler clusterYarnScheduler = clusterYarnSchedulerService.getScheduler(clusterId);

        return Result.success(clusterYarnScheduler.getScheduler());
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterYarnScheduler clusterYarnScheduler){
        clusterYarnSchedulerService.save(clusterYarnScheduler);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterYarnScheduler clusterYarnScheduler){

        clusterYarnSchedulerService.updateById(clusterYarnScheduler);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterYarnSchedulerService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
