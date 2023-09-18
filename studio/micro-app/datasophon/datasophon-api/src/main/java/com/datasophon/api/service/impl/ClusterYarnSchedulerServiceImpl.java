package com.datasophon.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.ClusterYarnScheduler;
import com.datasophon.dao.mapper.ClusterYarnSchedulerMapper;
import org.springframework.stereotype.Service;


@Service("clusterYarnSchedulerService")
public class ClusterYarnSchedulerServiceImpl extends ServiceImpl<ClusterYarnSchedulerMapper, ClusterYarnScheduler> implements ClusterYarnSchedulerService {

    @Override
    public ClusterYarnScheduler getScheduler(Integer clusterId) {
        return this.getOne(new QueryWrapper<ClusterYarnScheduler>().eq(Constants.CLUSTER_ID,clusterId));
    }

    @Override
    public void createDefaultYarnScheduler(Integer clusterId) {
        ClusterYarnScheduler scheduler = new ClusterYarnScheduler();
        scheduler.setScheduler("capacity");
        scheduler.setClusterId(clusterId);
        scheduler.setInUse(1);
        this.save(scheduler);
    }
}
