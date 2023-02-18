package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterYarnQueue;

/**
 * 
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-07-13 19:34:14
 */
public interface ClusterYarnQueueService extends IService<ClusterYarnQueue> {


    Result listByPage(Integer clusterId, Integer page, Integer pageSize);

    Result refreshQueues(Integer clusterId) throws Exception;
}

