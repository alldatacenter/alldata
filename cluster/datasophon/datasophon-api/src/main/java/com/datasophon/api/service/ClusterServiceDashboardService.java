package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceDashboard;

/**
 * 集群服务总览仪表盘
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-23 17:01:58
 */
public interface ClusterServiceDashboardService extends IService<ClusterServiceDashboard> {


    Result getDashboardUrl(Integer clusterId);
}

