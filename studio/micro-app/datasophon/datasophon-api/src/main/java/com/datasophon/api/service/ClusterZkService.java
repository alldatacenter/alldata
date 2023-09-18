package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.dao.entity.ClusterZk;

import java.util.List;

/**
 * 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-09-07 10:04:16
 */
public interface ClusterZkService extends IService<ClusterZk> {


    Integer getMaxMyId(Integer clusterId);

    List<ClusterZk> getAllZkServer(Integer clusterId);
}

