package com.datasophon.api.controller;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterYarnQueueService;
import com.datasophon.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterYarnQueue;

/**
 * 
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-07-13 19:34:14
 */
@RestController
@RequestMapping("cluster/yarn/queue")
public class ClusterYarnQueueController {
    @Autowired
    private ClusterYarnQueueService clusterYarnQueueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId, Integer page,Integer pageSize){
        return clusterYarnQueueService.listByPage(clusterId,page,pageSize);
    }

    /**
     * 刷新队列
     */
    @RequestMapping("/refreshQueues")
    public Result refreshQueues(Integer clusterId) throws Exception {
        return clusterYarnQueueService.refreshQueues(clusterId);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterYarnQueue clusterYarnQueue = clusterYarnQueueService.getById(id);

        return Result.success().put("clusterYarnQueue", clusterYarnQueue);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterYarnQueue clusterYarnQueue){
        List<ClusterYarnQueue> list = clusterYarnQueueService.list(new QueryWrapper<ClusterYarnQueue>().eq(Constants.QUEUE_NAME, clusterYarnQueue.getQueueName()));
        if(Objects.nonNull(list) && list.size() == 1){
            return Result.error(Status.QUEUE_NAME_ALREADY_EXISTS.getMsg());
        }
        clusterYarnQueue.setCreateTime(new Date());
        clusterYarnQueueService.save(clusterYarnQueue);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterYarnQueue clusterYarnQueue){

        clusterYarnQueueService.updateById(clusterYarnQueue);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterYarnQueueService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
