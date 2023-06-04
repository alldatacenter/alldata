package com.datasophon.api.service.impl;

import com.datasophon.api.service.ClusterAlertGroupMapService;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertGroupMap;
import com.datasophon.dao.entity.ClusterAlertQuota;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.AlertGroupMapper;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.api.service.AlertGroupService;


@Service("alertGroupService")
public class AlertGroupServiceImpl extends ServiceImpl<AlertGroupMapper, AlertGroupEntity> implements AlertGroupService {

    @Autowired
    private ClusterAlertGroupMapService alertGroupMapService;

    @Autowired
    private ClusterAlertQuotaService quotaService;

    @Override
    public Result getAlertGroupList(Integer clusterId, String alertGroupName, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterAlertGroupMap> list = alertGroupMapService.list(new QueryWrapper<ClusterAlertGroupMap>().eq(Constants.CLUSTER_ID, clusterId));
        List<Integer> groupIds = list.stream().map(e -> e.getAlertGroupId()).collect(Collectors.toList());
        List<AlertGroupEntity> alertGroupList = this.list(new QueryWrapper<AlertGroupEntity>()
                .in(Constants.ID, groupIds)
                .like(StringUtils.isNotBlank(alertGroupName),Constants.ALERT_GROUP_NAME,alertGroupName)
                .last("limit " + offset + "," + pageSize));
        int count = this.count(new QueryWrapper<AlertGroupEntity>()
                .in(Constants.ID, groupIds)
                .like(StringUtils.isNotBlank(alertGroupName),Constants.ALERT_GROUP_NAME,alertGroupName));
        //查询告警组下告警指标个数
        for (AlertGroupEntity alertGroupEntity : alertGroupList) {
            List<ClusterAlertQuota> quotaList = quotaService.list(new QueryWrapper<ClusterAlertQuota>().eq(Constants.ALERT_GROUP_ID, alertGroupEntity.getId()));
            alertGroupEntity.setAlertQuotaNum(quotaList.size());
        }
        return Result.success(alertGroupList).put(Constants.TOTAL,count);
    }

    @Override
    public Result saveAlertGroup(AlertGroupEntity alertGroup) {
        this.save(alertGroup);
        ClusterAlertGroupMap clusterAlertGroupMap = new ClusterAlertGroupMap();
        clusterAlertGroupMap.setAlertGroupId(alertGroup.getId());
        clusterAlertGroupMap.setClusterId(alertGroup.getClusterId());
        alertGroupMapService.save(clusterAlertGroupMap);
        return Result.success();
    }
}
