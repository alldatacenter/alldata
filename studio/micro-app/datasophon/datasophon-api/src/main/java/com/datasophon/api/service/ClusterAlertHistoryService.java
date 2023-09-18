package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertHistory;

import java.util.List;

/**
 * 集群告警历史表 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-07 12:04:38
 */
public interface ClusterAlertHistoryService extends IService<ClusterAlertHistory> {


    void saveAlertHistory(String alertMessage);

    Result getAlertList(Integer serviceInstanceId);

    Result getAllAlertList(Integer clusterId, Integer page, Integer pageSize);

    void removeAlertByRoleInstanceIds(List<Integer> ids);
}

