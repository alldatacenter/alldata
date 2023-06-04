package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;

import java.util.List;

/**
 * 集群服务操作指令主机指令表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
public interface ClusterServiceCommandHostCommandService extends IService<ClusterServiceCommandHostCommandEntity> {

    Result getHostCommandList(String hostname,String commandHostId, Integer page, Integer pageSize);

    List<ClusterServiceCommandHostCommandEntity> getHostCommandListByCommandId(String id);

    ClusterServiceCommandHostCommandEntity getByHostCommandId(String hostCommandId);

    void updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand);

    Integer getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId);

    Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId);

    Result getHostCommandLog(Integer clusterId , String hostCommandId) throws Exception;

    List<ClusterServiceCommandHostCommandEntity> findFailedHostCommand(String hostname, String commandHostId);

    List<ClusterServiceCommandHostCommandEntity> findCanceledHostCommand(String hostname, String commandHostId);
}

