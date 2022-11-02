package com.alibaba.tesla.appmanager.workflow.service;

import com.alibaba.tesla.appmanager.workflow.repository.domain.TaskHostRegistryDO;

import java.util.List;

/**
 * 任务执行器注册服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface TaskHostRegistryService {

    /**
     * 上报当前机器的心跳到系统中
     *
     * @return 返回当前上报的数据对象
     */
    TaskHostRegistryDO reportStatus();

    /**
     * 获取当前系统中长时间没有心跳的任务执行器列表
     *
     * @return 任务执行器列表，包含 hostname 和最后一次上报数据
     */
    List<TaskHostRegistryDO> findExpiredServers();

    /**
     * 删除指定 hostname 对应的 Server 对象
     *
     * @param hostname 主机名
     * @return 删除数量
     */
    int removeServer(String hostname);
}
