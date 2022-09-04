package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.AppAddonProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppAddonDTO;
import com.alibaba.tesla.appmanager.domain.req.AppAddonCreateReq;
import com.alibaba.tesla.appmanager.domain.req.AppAddonQueryReq;
import com.alibaba.tesla.appmanager.domain.req.AppAddonUpdateReq;
import com.alibaba.tesla.appmanager.server.assembly.AppAddonDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import com.alibaba.tesla.appmanager.server.service.addon.AddonMetaService;
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 应用 Addon 信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Slf4j
@Service
public class AppAddonProviderImpl implements AppAddonProvider {

    @Autowired
    private AppAddonService appAddonService;

    @Autowired
    private AddonMetaService addonMetaService;

    @Autowired
    private AppAddonDtoConvert appAddonDtoConvert;

    /**
     * 分页查询应用 Addon 信息
     */
    @Override
    public Pagination<AppAddonDTO> list(AppAddonQueryReq request) {
        AppAddonQueryCondition condition = new AppAddonQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<AppAddonDO> addons = appAddonService.list(condition);
        return Pagination.transform(addons, item -> appAddonDtoConvert.to(item));
    }

    /**
     * 通过 ID 查询 Addon 信息
     */
    @Override
    public AppAddonDTO get(Long id) {
        AppAddonQueryCondition condition = AppAddonQueryCondition.builder().id(id).build();
        return get(condition);
    }

    /**
     * 通过 name 查询 Addon 信息
     */
    @Override
    public AppAddonDTO get(String appId, String namespaceId, String stageId, String name) {
        AppAddonQueryCondition condition = AppAddonQueryCondition.builder()
                .appId(appId)
                .addonName(name)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .build();
        return get(condition);
    }

    /**
     * 通过 ID 删除 Addon 信息
     */
    @Override
    public boolean delete(String appId, String namespaceId, String stageId, String addonName) {
        AppAddonQueryCondition condition = AppAddonQueryCondition.builder()
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .addonName(addonName)
                .build();
        return appAddonService.delete(condition) > 0;
    }

    /**
     * 更新应用 Addon 绑定
     */
    @Override
    public boolean update(AppAddonUpdateReq request) {
        AppAddonQueryCondition condition = AppAddonQueryCondition.builder()
                .appId(request.getAppId())
                .namespaceId(request.getNamespaceId())
                .stageId(request.getStageId())
                .addonName(request.getAddonName())
                .build();
        AppAddonDTO appAddonDTO = AppAddonDTO.builder()
                .spec(request.getSpec())
                .build();
        int count = appAddonService.update(appAddonDtoConvert.from(appAddonDTO), condition);
        return count > 0;
    }

    /**
     * 新建应用 Addon 绑定
     */
    @Override
    public boolean create(AppAddonCreateReq request) {
        AppAddonDO record = appAddonService.create(request);
        return record != null;
    }

    private AppAddonDTO get(AppAddonQueryCondition condition) {
        Pagination<AppAddonDO> addonList = appAddonService.list(condition);
        if (addonList.isEmpty()) {
            return null;
        }
        Pagination<AppAddonDTO> addonDTOList = Pagination.transform(addonList, item -> appAddonDtoConvert.to(item));
        assert addonDTOList.getItems().size() == 1;
        return addonDTOList.getItems().get(0);
    }
}
