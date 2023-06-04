package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.dao.entity.ClusterYarnScheduler;

/**
 * 
 *
 * @author dygao2
 * @email dygao2@datasophon.com
 * @date 2022-11-25 15:02:11
 */
public interface ClusterYarnSchedulerService extends IService<ClusterYarnScheduler> {


    ClusterYarnScheduler getScheduler(Integer clusterId);

    void createDefaultYarnScheduler(Integer clusterId);
}

