package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;


import java.util.List;

/**
 * 集群服务操作指令表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
public interface ClusterServiceCommandService extends IService<ClusterServiceCommandEntity> {

    Result generateCommand(Integer clusterId, CommandType commandType,List<String> serviceNames);

    Result getServiceCommandlist(Integer clusterId, Integer page, Integer pageSize);

    Result generateServiceCommand(Integer clusterId, CommandType command, List<String> ids);

    Result generateServiceRoleCommand(Integer clusterId, CommandType command, Integer serviceIntanceId, List<String> ids);

    void startExecuteCommand(Integer clusterId, String commandType, String commandIds);

    void cancelCommand(String commandId);

    ClusterServiceCommandEntity getLastRestartCommand(Integer id);
}

