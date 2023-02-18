package com.datasophon.api.service.impl;

import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.datasophon.dao.mapper.ClusterServiceDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


@Service("clusterServiceDashboardService")
public class ClusterServiceDashboardServiceImpl extends ServiceImpl<ClusterServiceDashboardMapper, ClusterServiceDashboard> implements ClusterServiceDashboardService {
    @Autowired
    ClusterServiceDashboardService dashboardService;

    @Override
    public Result getDashboardUrl(Integer clusterId) {
        HashMap<String, String> globalVariables = (HashMap<String, String>) CacheUtils.get("globalVariables"+ Constants.UNDERLINE+clusterId);
        ClusterServiceDashboard dashboard = dashboardService.getOne(new QueryWrapper<ClusterServiceDashboard>().eq(Constants.SERVICE_NAME, "TOTAL"));
        String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables, Constants.REGEX_VARIABLE);
        return Result.success(dashboardUrl);
    }
}
