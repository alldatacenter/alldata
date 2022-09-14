package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.AppAddonProvider;
import com.alibaba.tesla.appmanager.api.provider.AppComponentProvider;
import com.alibaba.tesla.appmanager.api.provider.HelmMetaProvider;
import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.domain.dto.AppComponentDTO;
import com.alibaba.tesla.appmanager.domain.req.AppAddonQueryReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.appcomponent.AppComponentQueryReq;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 应用关联组件 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class AppComponentProviderImpl implements AppComponentProvider {

    @Autowired
    private K8sMicroServiceMetaProvider k8SMicroServiceMetaProvider;

    @Autowired
    private AppAddonProvider appAddonProvider;

    @Autowired
    private HelmMetaProvider helmMetaProvider;

    /**
     * 获取指定 appId 下的所有关联 Component 对象
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return List of AppComponentDTO
     */
    @Override
    public List<AppComponentDTO> list(AppComponentQueryReq request, String operator) {
        String appId = request.getAppId();
        String namespaceId = request.getNamespaceId();
        String stageId = request.getStageId();
        String arch = request.getArch();
        List<AppComponentDTO> result = new ArrayList<>();

        // 获取 K8S 微应用组件
        K8sMicroServiceMetaQueryReq k8sMicroServiceMetaQueryReq = new K8sMicroServiceMetaQueryReq();
        k8sMicroServiceMetaQueryReq.setAppId(appId);
        k8sMicroServiceMetaQueryReq.setNamespaceId(namespaceId);
        k8sMicroServiceMetaQueryReq.setArch(arch);
        k8sMicroServiceMetaQueryReq.setStageId(stageId);
        k8sMicroServiceMetaQueryReq.setPagination(false);
        k8SMicroServiceMetaProvider.list(k8sMicroServiceMetaQueryReq).getItems()
                .forEach(k8sMicroServiceMetaDTO ->
                        result.add(AppComponentDTO.builder()
                                .id(k8sMicroServiceMetaDTO.getId())
                                .appId(appId)
                                .namespaceId(namespaceId)
                                .stageId(stageId)
                                .componentName(k8sMicroServiceMetaDTO.getMicroServiceId())
                                .componentLabel(k8sMicroServiceMetaDTO.getName())
                                .componentType(k8sMicroServiceMetaDTO.getComponentType())
                                .build()
                        )
                );

        // 获取 HELM 组件
        HelmMetaQueryReq helmMetaQueryReq = new HelmMetaQueryReq();
        helmMetaQueryReq.setAppId(appId);
        helmMetaQueryReq.setNamespaceId(namespaceId);
        helmMetaQueryReq.setStageId(stageId);
        helmMetaQueryReq.setPagination(false);
        helmMetaProvider.list(helmMetaQueryReq).getItems()
                .forEach(helmMetaDO ->
                        result.add(AppComponentDTO.builder()
                                .id(helmMetaDO.getId())
                                .appId(appId)
                                .namespaceId(namespaceId)
                                .stageId(stageId)
                                .componentName(helmMetaDO.getHelmPackageId())
                                .componentLabel(helmMetaDO.getName())
                                .componentType(helmMetaDO.getComponentType())
                                .build()
                        )

                );

        // 获取 Internal Addon
        AppAddonQueryReq internalAddonQueryReq = new AppAddonQueryReq();
        internalAddonQueryReq.setAppId(appId);
        internalAddonQueryReq.setNamespaceId(namespaceId);
        internalAddonQueryReq.setStageId(stageId);
        internalAddonQueryReq.setPagination(false);
        internalAddonQueryReq.setAddonTypeList(Collections.singletonList(ComponentTypeEnum.INTERNAL_ADDON));
        appAddonProvider.list(internalAddonQueryReq).getItems()
                .forEach(item ->
                        result.add(AppComponentDTO.builder()
                                .id(item.getId())
                                .appId(appId)
                                .namespaceId(namespaceId)
                                .stageId(stageId)
                                .componentType(item.getAddonType())
                                .componentName(item.getAddonId())
                                .componentVersion(item.getAddonVersion())
                                .componentLabel(String.format("%s@%s", item.getAddonId(), item.getName()))
                                .build()
                        )
                );

        // 获取 Resource Addon
        AppAddonQueryReq resourceAddonQueryReq = new AppAddonQueryReq();
        resourceAddonQueryReq.setAppId(appId);
        resourceAddonQueryReq.setNamespaceId(namespaceId);
        resourceAddonQueryReq.setStageId(stageId);
        resourceAddonQueryReq.setPagination(false);
        resourceAddonQueryReq.setAddonTypeList(Collections.singletonList(ComponentTypeEnum.RESOURCE_ADDON));
        appAddonProvider.list(resourceAddonQueryReq).getItems()
                .forEach(item ->
                        result.add(AppComponentDTO.builder()
                                .id(item.getId())
                                .appId(appId)
                                .namespaceId(namespaceId)
                                .stageId(stageId)
                                .componentType(item.getAddonType())
                                .componentName(String.format("%s@%s", item.getAddonId(), item.getName()))
                                .componentVersion(item.getAddonVersion())
                                .componentLabel(String.format("%s@%s", item.getAddonId(), item.getName()))
                                .build()
                        )
                );
        return result;
    }
}
