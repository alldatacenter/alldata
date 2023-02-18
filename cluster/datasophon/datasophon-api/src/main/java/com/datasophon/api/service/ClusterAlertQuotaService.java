package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertQuota;

import java.util.List;

/**
 * 集群告警指标表 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-24 15:10:41
 */
public interface ClusterAlertQuotaService extends IService<ClusterAlertQuota> {


    Result getAlertQuotaList(Integer clusterId, Integer alertGroupId, String quotaName, Integer page, Integer pageSize);

    Result start(Integer clusterId, String alertQuotaIds);

    Result stop(Integer clusterId,String alertQuotaIds);

    void saveAlertQuota(ClusterAlertQuota clusterAlertQuota);

    List<ClusterAlertQuota> listAlertQuotaByServiceName(String serviceName);
}

