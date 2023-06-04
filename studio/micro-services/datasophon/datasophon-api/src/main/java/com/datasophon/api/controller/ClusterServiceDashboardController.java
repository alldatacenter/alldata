package com.datasophon.api.controller;



import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * 集群服务总览仪表盘
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-06-23 17:01:58
 */
@RestController
@RequestMapping("cluster/service/dashboard")
public class ClusterServiceDashboardController {
    @Autowired
    private ClusterServiceDashboardService clusterServiceDashboardService;

    /**
     * 列表
     */
    @RequestMapping("/getDashboardUrl")
    public Result getDashboardUrl(Integer clusterId){

        return clusterServiceDashboardService.getDashboardUrl(clusterId);
    }



}
