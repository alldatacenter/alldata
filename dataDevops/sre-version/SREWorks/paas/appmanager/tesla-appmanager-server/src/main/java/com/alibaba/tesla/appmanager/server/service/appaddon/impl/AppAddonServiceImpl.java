package com.alibaba.tesla.appmanager.server.service.appaddon.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.AddonUtil;
import com.alibaba.tesla.appmanager.common.util.EnvUtil;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.container.DeployConfigTypeId;
import com.alibaba.tesla.appmanager.domain.dto.AppAddonDTO;
import com.alibaba.tesla.appmanager.domain.req.AppAddonCreateReq;
import com.alibaba.tesla.appmanager.domain.req.appaddon.AppAddonSyncReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigUpdateReq;
import com.alibaba.tesla.appmanager.server.assembly.AppAddonDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.AppAddonRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import com.alibaba.tesla.appmanager.server.service.addon.AddonMetaService;
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Function;

/**
 * 应用组件服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Service
@Slf4j
public class AppAddonServiceImpl implements AppAddonService {

    @Autowired
    private AppAddonRepository appAddonRepository;

    @Autowired
    private AddonMetaService addonMetaService;

    @Autowired
    private AppAddonDtoConvert appAddonDtoConvert;

    @Autowired
    private DeployConfigService deployConfigService;

    /**
     * 根据条件过滤应用组件
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<AppAddonDO> list(AppAddonQueryCondition condition) {
        List<AppAddonDO> page = appAddonRepository.selectByCondition(condition);
        return Pagination.valueOf(page, Function.identity());
    }

    /**
     * 根据条件查询某个应用组件
     *
     * @param condition 查询条件
     * @return AppAddonDO
     */
    @Override
    public AppAddonDO get(AppAddonQueryCondition condition) {
        List<AppAddonDO> records = appAddonRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        return records.get(0);
    }

    /**
     * 更新指定的应用组件
     *
     * @param record App Addon 记录
     */
    @Override
    public int update(AppAddonDO record, AppAddonQueryCondition condition) {
        return appAddonRepository.updateByCondition(record, condition);
    }

    /**
     * 更新指定的应用组件
     *
     * @param request 创建请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppAddonDO create(AppAddonCreateReq request) {
        AddonMetaDO addonMetaDO;
        if (StringUtils.isNotEmpty(request.getAddonType()) && StringUtils.isNotEmpty(request.getAddonId())) {
            addonMetaDO = addonMetaService.get(ComponentTypeEnum.parse(request.getAddonType()), request.getAddonId());
        } else {
            addonMetaDO = addonMetaService.get(request.getAddonMetaId());
        }
        ComponentTypeEnum addonType = ComponentTypeEnum.parse(addonMetaDO.getAddonType());
        AppAddonDTO appAddonDTO = AppAddonDTO.builder()
                .appId(request.getAppId())
                .namespaceId(request.getNamespaceId())
                .stageId(request.getStageId())
                .addonType(addonType)
                .addonId(addonMetaDO.getAddonId())
                .name(request.getAddonName())
                .addonVersion(addonMetaDO.getAddonVersion())
                .spec(request.getSpec())
                .build();
        AppAddonDO record = appAddonDtoConvert.from(appAddonDTO);
        int count = appAddonRepository.insert(record);
        if (count == 0) {
            return null;
        }

        // 更新 application configuration 绑定关系
        String componentName;
        if (addonType.isInternalAddon()) {
            componentName = addonMetaDO.getAddonId();
        } else {
            componentName = AddonUtil.combineComponentName(addonMetaDO.getAddonId(), request.getAddonName());
        }
        String typeId = new DeployConfigTypeId(addonType, componentName).toString();
        String requestNamespaceId = request.getNamespaceId();
        String requestStageId = request.getStageId();
        // TODO: FOR SREWORKS ONLY TEMPORARY
        if (EnvUtil.isSreworks()) {
            requestNamespaceId = EnvUtil.defaultNamespaceId();
            requestStageId = EnvUtil.defaultStageId();
        }
        deployConfigService.update(DeployConfigUpdateReq.builder()
                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                .appId(request.getAppId())
                .typeId(typeId)
                .envId("")
                .inherit(true)
                .config("")
                .isolateNamespaceId(requestNamespaceId)
                .isolateStageId(requestStageId)
                .build());
        return record;
    }

    /**
     * 根据条件删除应用组件
     *
     * @param condition 查询条件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(AppAddonQueryCondition condition) {
        AppAddonDO appAddon = get(condition);
        if (appAddon == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find app addon by condition|condition=%s",
                            JSONObject.toJSONString(condition)));
        }
        String appId = condition.getAppId();
        String componentName;
        if (appAddon.getAddonType().isInternalAddon()) {
            componentName = appAddon.getAddonId();
        } else {
            componentName = AddonUtil.combineComponentName(appAddon.getAddonId(), appAddon.getName());
        }
        String typeId = new DeployConfigTypeId(appAddon.getAddonType(), componentName).toString();
        deployConfigService.delete(DeployConfigDeleteReq.builder()
                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                .appId(appId)
                .typeId(typeId)
                .envId("")
                .build());
        return appAddonRepository.deleteByCondition(condition);
    }

    /**
     * 同步当前所有 app addon 绑定关系到 deploy config 中
     */
    @Override
    public void sync(AppAddonSyncReq request) {
        List<AppAddonDO> appAddons = appAddonRepository.selectByCondition(AppAddonQueryCondition.builder()
                .pageSize(DefaultConstant.UNLIMITED_PAGE_SIZE)
                .build());
        String requestNamespaceId = request.getNamespaceId();
        String requestStageId = request.getStageId();
        // TODO: FOR SREWORKS ONLY TEMPORARY
        if (EnvUtil.isSreworks()) {
            requestNamespaceId = EnvUtil.defaultNamespaceId();
            requestStageId = EnvUtil.defaultStageId();
        }
        for (AppAddonDO appAddon : appAddons) {
            String componentName;
            if (appAddon.getAddonType().isInternalAddon()) {
                componentName = appAddon.getAddonId();
            } else {
                componentName = AddonUtil.combineComponentName(appAddon.getAddonId(), appAddon.getName());
            }
            String typeId = new DeployConfigTypeId(appAddon.getAddonType(), componentName).toString();
            deployConfigService.update(DeployConfigUpdateReq.builder()
                    .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                    .appId(appAddon.getAppId())
                    .typeId(typeId)
                    .envId("")
                    .inherit(true)
                    .config("")
                    .isolateNamespaceId(requestNamespaceId)
                    .isolateStageId(requestStageId)
                    .build());
        }
    }
}
