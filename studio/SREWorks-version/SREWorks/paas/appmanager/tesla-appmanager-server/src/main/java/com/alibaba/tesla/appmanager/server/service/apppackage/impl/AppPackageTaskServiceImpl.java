package com.alibaba.tesla.appmanager.server.service.apppackage.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageNextVersionReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskNextVersionReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageNextVersionRes;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageTaskNextVersionRes;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTagRepository;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskInQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应用包任务服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class AppPackageTaskServiceImpl implements AppPackageTaskService {

    private final AppPackageTaskRepository appPackageTaskRepository;

    private final AppPackageTagRepository appPackageTagRepository;

    private final ComponentPackageService componentPackageService;

    private final ComponentPackageTaskService componentPackageTaskService;

    public AppPackageTaskServiceImpl(
            AppPackageTaskRepository appPackageTaskRepository, AppPackageTagRepository appPackageTagRepository,
            ComponentPackageService componentPackageService, ComponentPackageTaskService componentPackageTaskService) {
        this.appPackageTaskRepository = appPackageTaskRepository;
        this.appPackageTagRepository = appPackageTagRepository;
        this.componentPackageService = componentPackageService;
        this.componentPackageTaskService = componentPackageTaskService;
    }

    /**
     * 根据条件过滤应用包任务列表
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<AppPackageTaskDO> list(AppPackageTaskQueryCondition condition) {
        List<AppPackageTaskDO> taskList = appPackageTaskRepository.selectByCondition(condition);
        if (condition.isWithBlobs()) {
            taskList.forEach(task -> {
                if (StringUtils.isNotEmpty(task.getPackageOptions())) {
                    JSONObject options = JSONObject.parseObject(task.getPackageOptions());
                    task.setTags(JSONObject.parseArray(options.getString("tags"), String.class));
                }
            });
        }

        List<Long> appPackageIdList = taskList.stream()
                .filter(task -> Objects.nonNull(task.getAppPackageId()))
                .map(AppPackageTaskDO::getAppPackageId)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(appPackageIdList)) {
            List<AppPackageTagDO> onSaleList = appPackageTagRepository.query(appPackageIdList,
                    DefaultConstant.ON_SALE);

            if (CollectionUtils.isNotEmpty(onSaleList)) {
                List<Long> onSaleIdList = onSaleList.stream()
                        .map(AppPackageTagDO::getAppPackageId)
                        .collect(Collectors.toList());
                taskList.stream()
                        .filter(task -> onSaleIdList.contains(task.getAppPackageId()))
                        .forEach(task -> task.setIsOnSale(Boolean.TRUE));
            }
        }
        return Pagination.valueOf(taskList, Function.identity());
    }

    /**
     * 根据 ID List 列出应用包任务列表 (仅状态，无 Blob 数据)
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public List<AppPackageTaskDO> listIn(AppPackageTaskInQueryCondition condition) {
        return appPackageTaskRepository.selectByCondition(AppPackageTaskQueryCondition.builder()
                .idList(condition.getIdList())
                .withBlobs(false)
                .build());
    }

    /**
     * 根据条件获取指定的应用包任务
     *
     * @param condition 过滤条件
     * @return 单个对象
     */
    @Override
    public AppPackageTaskDO get(AppPackageTaskQueryCondition condition) {
        Pagination<AppPackageTaskDO> results = list(condition);
        if (results.isEmpty()) {
            return null;
        } else if (results.getTotal() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple app package tasks found, abort|condition=%s",
                            JSONObject.toJSONString(condition)));
        }
        return results.getItems().get(0);
    }

    @Override
    public int delete(AppPackageTaskQueryCondition condition) {
        return appPackageTaskRepository.deleteByCondition(condition);
    }

    /**
     * 获取指定组件的下一个 Version
     *
     * @param appId         应用 ID
     * @param componentType 组件类型
     * @param componentName 组件名称
     * @param fullVersion   当前提供版本（可为 _，为自动生成）
     * @return next version
     */
    @Override
    public String getComponentNextVersion(
            String appId, ComponentTypeEnum componentType, String componentName, String fullVersion) {
        if (!StringUtils.equals(fullVersion, DefaultConstant.AUTO_VERSION)) {
            return fullVersion;
        }

        // 当 version 为 _ 时（自动生成），根据当前组件包的版本号情况自动获取 nextVersion
        ComponentPackageNextVersionRes packageNextVersion = componentPackageService.nextVersion(
                ComponentPackageNextVersionReq.builder()
                        .appId(appId)
                        .componentType(componentType.toString())
                        .componentName(componentName)
                        .build());
        ComponentPackageTaskNextVersionRes taskNextVersion = componentPackageTaskService.nextVersion(
                ComponentPackageTaskNextVersionReq.builder()
                        .appId(appId)
                        .componentType(componentType.toString())
                        .componentName(componentName)
                        .build());
        fullVersion = packageNextVersion.getNextVersion();
        if (VersionUtil.compareTo(fullVersion, taskNextVersion.getNextVersion()) < 0) {
            fullVersion = taskNextVersion.getNextVersion();
        }
        return fullVersion;
    }
}
