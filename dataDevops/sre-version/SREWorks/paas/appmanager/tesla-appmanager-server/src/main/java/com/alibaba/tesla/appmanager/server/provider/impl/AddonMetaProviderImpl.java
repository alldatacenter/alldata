package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.AddonMetaProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.AddonMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.AddonMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.appaddon.AppAddonSyncReq;
import com.alibaba.tesla.appmanager.server.assembly.AddonMetaDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import com.alibaba.tesla.appmanager.server.service.addon.AddonMetaService;
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class AddonMetaProviderImpl implements AddonMetaProvider {

    @Resource
    private AddonMetaDtoConvert addonMetaDtoConvert;

    @Autowired
    private AddonMetaService addonMetaService;

    @Autowired
    private AppAddonService appAddonService;

    @Override
    public Pagination<AddonMetaDTO> list(AddonMetaQueryReq request) {
        AddonMetaQueryCondition condition = new AddonMetaQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<AddonMetaDO> metaList = addonMetaService.list(condition);
        return Pagination.transform(metaList, item -> addonMetaDtoConvert.to(item));
    }

    @Override
    public AddonMetaDTO get(Long id) {
        return addonMetaDtoConvert.to(addonMetaService.get(id));
    }

    @Override
    public AddonMetaDTO get(String addonId, String addonVersion) {
        AddonMetaQueryCondition condition = AddonMetaQueryCondition.builder()
                .addonId(addonId)
                .addonVersion(addonVersion)
                .build();
        Pagination<AddonMetaDO> metaList = addonMetaService.list(condition);
        if (metaList.isEmpty()) {
            return null;
        }
        return addonMetaDtoConvert.to(metaList.getItems().get(0));
    }

    @Override
    public void update(AddonMetaDTO record) {
        AddonMetaDO metaDO = addonMetaDtoConvert.from(record);
        addonMetaService.update(metaDO);
    }

    @Override
    public AddonMetaDTO create(AddonMetaDTO record) {
        AddonMetaDO metaDO = addonMetaDtoConvert.from(record);
        addonMetaService.create(metaDO);
        return get(metaDO.getId());
    }

    @Override
    public void delete(Long id) {
        addonMetaService.delete(id);
    }

    /**
     * 同步当前所有 app addon 绑定关系到 deploy config 中
     */
    @Override
    public void sync(AppAddonSyncReq request) {
        appAddonService.sync(request);
    }
}
