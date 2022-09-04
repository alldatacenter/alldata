package com.alibaba.tesla.appmanager.server.service.componentpackage.impl;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskNextVersionReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageTaskNextVersionRes;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * 组件包任务服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class ComponentPackageTaskServiceImpl implements ComponentPackageTaskService {

    private final ComponentPackageTaskRepository taskRepository;

    public ComponentPackageTaskServiceImpl(ComponentPackageTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 根据条件过滤组件包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<ComponentPackageTaskDO> list(ComponentPackageTaskQueryCondition condition) {
        List<ComponentPackageTaskDO> tasks = taskRepository.selectByCondition(condition);
        return Pagination.valueOf(tasks, Function.identity());
    }

    /**
     * 根据条件获取单个组件包
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public ComponentPackageTaskDO get(ComponentPackageTaskQueryCondition condition) {
        return taskRepository.getByCondition(condition);
    }

    /**
     * 更新组件构建任务包
     *
     * @param record    任务对象
     * @param condition 查询条件
     * @return 更新数量
     */
    @Override
    public int update(ComponentPackageTaskDO record, ComponentPackageTaskQueryCondition condition) {
        return taskRepository.updateByCondition(record, condition);
    }

    /**
     * 根据查询条件删除任务记录
     *
     * @param condition 查询条件
     * @return 删除数量
     */
    @Override
    public int delete(ComponentPackageTaskQueryCondition condition) {
        return taskRepository.deleteByCondition(condition);
    }

    /**
     * 获取指定组件包任务的下一个可用版本 (with build number)
     *
     * @param req 查询请求
     * @return 下一个可用版本 (含当前)
     */
    @Override
    public ComponentPackageTaskNextVersionRes nextVersion(ComponentPackageTaskNextVersionReq req) {
        ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                .appId(req.getAppId())
                .componentType(req.getComponentType())
                .componentName(req.getComponentName())
                .withBlobs(false)
                .page(1)
                .pageSize(1)
                .build();
        List<ComponentPackageTaskDO> tasks = taskRepository.selectByCondition(condition);
        String currentVersion = DefaultConstant.INIT_VERSION;
        if (CollectionUtils.isNotEmpty(tasks)) {
            currentVersion = tasks.get(0).getPackageVersion();
        }
        String nextVersion = VersionUtil.buildVersion(VersionUtil.buildNextPatch(currentVersion));
        return ComponentPackageTaskNextVersionRes.builder()
                .currentVersion(currentVersion)
                .nextVersion(nextVersion)
                .build();
    }
}
