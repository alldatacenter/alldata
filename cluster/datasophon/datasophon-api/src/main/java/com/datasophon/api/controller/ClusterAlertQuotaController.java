package com.datasophon.api.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.api.service.ClusterAlertQuotaService;

/**
 * 集群告警指标表 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-24 15:10:41
 */
@RestController
@RequestMapping("cluster/alert/quota")
public class ClusterAlertQuotaController {
    @Autowired
    private ClusterAlertQuotaService clusterAlertQuotaService;


    /**
     * 信息
     */
    @RequestMapping("/list")
    public Result info(Integer clusterId,Integer alertGroupId,String quotaName,Integer page,Integer pageSize){
        return clusterAlertQuotaService.getAlertQuotaList(clusterId,alertGroupId,quotaName,page,pageSize);
    }

    /**
     * 启用
     */
    @RequestMapping("/start")
    public Result start(Integer clusterId,String alertQuotaIds){
        return clusterAlertQuotaService.start(clusterId,alertQuotaIds);
    }

    /**
     * 停用
     */
    @RequestMapping("/stop")
    public Result stop(Integer clusterId,String alertQuotaIds){
        return clusterAlertQuotaService.stop(clusterId,alertQuotaIds);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterAlertQuota clusterAlertQuota){


        clusterAlertQuotaService.saveAlertQuota(clusterAlertQuota);
        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterAlertQuota clusterAlertQuota){

        clusterAlertQuotaService.updateById(clusterAlertQuota);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterAlertQuotaService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
