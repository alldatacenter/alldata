package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;

import java.util.List;

/**
 * 集群服务操作指令主机表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
public interface ClusterServiceCommandHostService extends IService<ClusterServiceCommandHostEntity> {




    Result getCommandHostList(Integer clusterId, String commandId, Integer page, Integer pageSize);

    Integer getCommandHostSizeByCommandId(String commandId);

    Integer getCommandHostTotalProgressByCommandId(String commandId);

    List<ClusterServiceCommandHostEntity> findFailedCommandHost(String commandId);

    List<ClusterServiceCommandHostEntity> findCanceledCommandHost(String commandId);
}

