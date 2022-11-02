package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.JsonUtil;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageDTO;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageVersionItemDTO;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.*;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageCreateRes;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageTaskNextVersionRes;
import com.alibaba.tesla.appmanager.server.assembly.ComponentPackageDtoConvert;
import com.alibaba.tesla.appmanager.server.assembly.ComponentPackageTaskDtoConvert;
import com.alibaba.tesla.appmanager.server.event.componentpackage.OpRetryComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.event.componentpackage.StartComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageComponentRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ComponentPackage 服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class ComponentPackageProviderImpl implements ComponentPackageProvider {

    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ComponentPackageRepository componentPackageRepository;

    @Resource
    private ComponentPackageTaskDtoConvert componentPackageTaskDtoConvert;

    @Resource
    private ComponentPackageDtoConvert componentPackageDtoConvert;

    @Autowired
    private AppPackageComponentRelRepository appPackageComponentRelRepository;

    @Autowired
    private ComponentPackageTaskService componentPackageTaskService;

    @Autowired
    private SystemProperties systemProperties;

    /**
     * 根据条件查询组件包列表
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return ComponentPackage 列表
     */
    @Override
    public Pagination<ComponentPackageDTO> list(ComponentPackageQueryReq request, String operator) {
        ComponentPackageQueryCondition condition = new ComponentPackageQueryCondition();
        ClassUtil.copy(request, condition);
        List<ComponentPackageDO> results = componentPackageRepository.selectByCondition(condition);
        return Pagination.valueOf(results, item -> componentPackageDtoConvert.to(item));
    }

    /**
     * 创建组件包
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 任务 ID
     */
    @Override
    public ComponentPackageCreateRes createTask(ComponentPackageTaskCreateReq request, String operator) {
        String appId = request.getAppId();
        String namespaceId = request.getNamespaceId();
        String stageId = request.getStageId();

        // 对当前版本合法性进行检测
        String newVersion = request.getVersion();
        ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                .appId(appId)
                .componentType(request.getComponentType())
                .componentName(request.getComponentName())
                .withBlobs(false)
                .build();
        List<ComponentPackageDO> latestComponentPackage = componentPackageRepository.selectByCondition(condition);
        String oldVersion = null;
        if (CollectionUtils.isNotEmpty(latestComponentPackage)) {
            oldVersion = latestComponentPackage.get(0).getPackageVersion();
        }

        String fullVersion = PackageUtil.fullVersion(oldVersion, newVersion);

        // 插入实际任务
        ComponentPackageTaskDO taskDO = ComponentPackageTaskDO.builder()
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .componentType(request.getComponentType())
                .componentName(request.getComponentName())
                .packageVersion(fullVersion)
                .packageCreator(operator)
                .packageOptions(JsonUtil.toJsonString(request.getOptions()))
                .taskStatus(ComponentPackageTaskStateEnum.CREATED.toString())
                .appPackageTaskId(request.getAppPackageTaskId())
                .envId(systemProperties.getEnvId())
                .build();
        componentPackageTaskRepository.insert(taskDO);
        publisher.publishEvent(new StartComponentPackageTaskEvent(this, taskDO.getId()));
        log.info("component package task has created|taskId={}|creator={}|task={}", taskDO.getId(), operator,
                JSONObject.toJSONString(taskDO));
        return ComponentPackageCreateRes.builder().componentPackageTaskId(taskDO.getId()).build();
    }

    /**
     * 查询任务状态
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return
     */
    @Override
    public ComponentPackageTaskDTO getTask(ComponentPackageTaskQueryReq request, String operator) {
        ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                .id(request.getComponentPackageTaskId())
                .build();
        ComponentPackageTaskDO record = componentPackageTaskRepository.getByCondition(condition);
        return componentPackageTaskDtoConvert.to(record);
    }

    /**
     * 根据条件批量查询任务列表
     *
     * @param req 请求数据
     * @return List
     */
    @Override
    public Pagination<ComponentPackageTaskDTO> listTask(ComponentPackageTaskListQueryReq req, String operator) {
        ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                .idList(req.getComponentPackageTaskIdList())
                .appPackageTaskId(req.getAppPackageTaskId())
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .withBlobs(req.isWithBlobs())
                .build();
        Pagination<ComponentPackageTaskDO> tasks = componentPackageTaskService.list(condition);
        return Pagination.transform(tasks, item -> componentPackageTaskDtoConvert.to(item));
    }

    @Override
    public ComponentPackageTaskDTO getTask(Long componentPackageTaskId, String operator) {
        ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                .id(componentPackageTaskId)
                .build();
        ComponentPackageTaskDO componentPackageTaskDO = componentPackageTaskRepository.getByCondition(condition);
        return componentPackageTaskDtoConvert.to(componentPackageTaskDO);
    }

    /**
     * 重试指定任务
     *
     * @param request  请求数据
     * @param operator 操作人
     */
    @Override
    public void retryTask(ComponentPackageTaskRetryReq request, String operator) {
        Long componentPackageTaskId = request.getComponentPackageTaskId();
        publisher.publishEvent(new OpRetryComponentPackageTaskEvent(this, componentPackageTaskId));
    }

    /**
     * 获取指定应用包的下一个最新版本
     */
    @Override
    public List<ComponentPackageVersionItemDTO> latestVersions(
            ComponentPackageLatestVersionListReq request, String operator) {
        String appId = request.getAppId();
        String componentType = request.getComponentType();
        String componentName = request.getComponentName();

        // 获取组件包历史记录
        ComponentPackageQueryReq condition = ComponentPackageQueryReq.builder()
                .appId(appId)
                .componentType(componentType)
                .componentName(componentName)
                .pagination(false)
                .build();
        Pagination<ComponentPackageDTO> componentPackageList = list(condition, operator);

        // 额外获取组件任务中的最新版本记录，确保满足递增需求
        ComponentPackageTaskNextVersionRes packageTaskNextVersionRes = componentPackageTaskService.nextVersion(
                ComponentPackageTaskNextVersionReq.builder()
                        .appId(appId)
                        .componentType(componentType)
                        .componentName(componentName)
                        .build());

        // 计算确认当前最新版本 max(component package, component package task)
        String currentVersion;
        if (CollectionUtils.isNotEmpty(componentPackageList.getItems())) {
            currentVersion = componentPackageList.getItems().get(0).getPackageVersion();
            if (VersionUtil.compareTo(currentVersion, packageTaskNextVersionRes.getCurrentVersion()) < 0) {
                currentVersion = packageTaskNextVersionRes.getCurrentVersion();
            }
        } else {
            currentVersion = packageTaskNextVersionRes.getCurrentVersion();
        }
        String nextSimpleVersion = VersionUtil.buildNextPatch(currentVersion);

        // 组装最后结果
        List<ComponentPackageVersionItemDTO> componentVersionList = new ArrayList<>();
        componentVersionList.add(ComponentPackageVersionItemDTO.builder()
                .name(nextSimpleVersion)
                .label(nextSimpleVersion)
                .build());
        componentVersionList.addAll(componentPackageList.getItems().stream()
                .map(item -> ComponentPackageVersionItemDTO.builder()
                        .name(item.getPackageVersion())
                        .label(item.getPackageVersion())
                        .build())
                .collect(Collectors.toList()));
        return componentVersionList;
    }

    /**
     * 根据 app package id 获取应用包列表
     */
    @Override
    public Pagination<ComponentPackageDTO> listByAppPackageId(ComponentPackageListQueryReq request, String operator) {
        AppPackageComponentRelQueryCondition condition = new AppPackageComponentRelQueryCondition();
        ClassUtil.copy(request, condition);
        List<Long> idList = appPackageComponentRelRepository.selectByCondition(condition).stream()
                .map(AppPackageComponentRelDO::getComponentPackageId)
                .collect(Collectors.toList());
        ComponentPackageQueryCondition condition2 = ComponentPackageQueryCondition.builder()
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .pagination(request.isPagination())
                .idList(idList)
                .withBlobs(false)
                .build();
        List<ComponentPackageDO> componentPackages = componentPackageRepository.selectByCondition(condition2);
        return Pagination.valueOf(componentPackages, item -> componentPackageDtoConvert.to(item));
    }
}
