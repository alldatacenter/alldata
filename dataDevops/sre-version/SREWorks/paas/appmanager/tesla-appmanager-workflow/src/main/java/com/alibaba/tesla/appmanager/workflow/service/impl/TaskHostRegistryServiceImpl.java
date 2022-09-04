package com.alibaba.tesla.appmanager.workflow.service.impl;

import com.alibaba.tesla.appmanager.workflow.repository.domain.TaskHostRegistryDO;
import com.alibaba.tesla.appmanager.workflow.service.TaskHostRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务执行器注册服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class TaskHostRegistryServiceImpl implements TaskHostRegistryService {

    /**
     * 上报当前机器的心跳到系统中
     *
     * @return 返回当前上报的数据对象
     */
    @Override
    public TaskHostRegistryDO reportStatus() {
        return null;
    }

    /**
     * 获取当前系统中长时间没有心跳的任务执行器列表
     *
     * @return 任务执行器列表，包含 hostname 和最后一次上报数据
     */
    @Override
    public List<TaskHostRegistryDO> findExpiredServers() {
        return null;
    }

    /**
     * 删除指定 hostname 对应的 Server 对象
     *
     * @param hostname 主机名
     * @return 删除数量
     */
    @Override
    public int removeServer(String hostname) {
        return 0;
    }
}
