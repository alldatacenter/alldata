package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterAlertHistory;

/**
 * 集群告警历史表 
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-06-07 12:04:38
 */
@RestController
@RequestMapping("cluster/alert/history")
public class ClusterAlertHistoryController {



    @Autowired
    private ClusterAlertHistoryService clusterAlertHistoryService;

    /**
     * 列表
     */
    @RequestMapping("/getAlertList")
    public Result getAlertList(Integer serviceInstanceId){
        return clusterAlertHistoryService.getAlertList(serviceInstanceId);
    }

    /**
     * 列表
     */
    @RequestMapping("/getAllAlertList")
    public Result getAllAlertList(Integer clusterId, Integer page,Integer pageSize){
        return clusterAlertHistoryService.getAllAlertList(clusterId,page,pageSize);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterAlertHistory clusterAlertHistory = clusterAlertHistoryService.getById(id);

        return Result.success().put("clusterAlertHistory", clusterAlertHistory);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody String alertMessage){
        clusterAlertHistoryService.saveAlertHistory(alertMessage);
        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterAlertHistory clusterAlertHistory){

        clusterAlertHistoryService.updateById(clusterAlertHistory);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterAlertHistoryService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
