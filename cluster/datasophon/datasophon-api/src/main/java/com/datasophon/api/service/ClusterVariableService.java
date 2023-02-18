package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.dao.entity.ClusterVariable;

/**
 * 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-14 15:50:36
 */
public interface ClusterVariableService extends IService<ClusterVariable> {


    ClusterVariable getVariableByVariableName(String variableName, Integer clusterId);
}

