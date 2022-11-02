package com.alibaba.tesla.appmanager.meta.helm.api;

import com.alibaba.tesla.appmanager.api.provider.HelmMetaProvider;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.HelmMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaCreateReq;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaUpdateReq;
import com.alibaba.tesla.appmanager.meta.helm.assembly.HelmMetaDtoConvert;
import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import com.alibaba.tesla.appmanager.meta.helm.service.HelmMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

@Service
public class HelmMetaProviderImpl implements HelmMetaProvider {

    @Autowired
    HelmMetaService helmMetaService;

    @Autowired
    HelmMetaDtoConvert helmMetaDtoConvert;

    @Override
    public Pagination<HelmMetaDTO> list(HelmMetaQueryReq request) {
        HelmMetaQueryCondition condition = HelmMetaQueryCondition.builder()
                .id(request.getId())
                .appId(request.getAppId())
                .namespaceId(request.getNamespaceId())
                .stageId(request.getStageId())
                .name(request.getName()).build();
        Pagination<HelmMetaDO> helmMetaDOPagination = helmMetaService.list(condition);
        return Pagination.transform(helmMetaDOPagination, item -> helmMetaDtoConvert.to(item));
    }

    @Override
    public HelmMetaDTO get(Long id, String namespaceId, String stageId) {
        HelmMetaDO helmMetaDO = helmMetaService.get(id, namespaceId, stageId);
        if (Objects.isNull(helmMetaDO)) {
            return null;
        }
        return helmMetaDtoConvert.to(helmMetaDO);
    }

    @Override
    public HelmMetaDTO create(HelmMetaCreateReq request) {
        HelmMetaDO helmMetaDO = new HelmMetaDO();
        ClassUtil.copy(request, helmMetaDO);
        helmMetaDO.setComponentType(ComponentTypeEnum.HELM.name());
        helmMetaDO.setPackageType(request.getPackageType().name());
        helmMetaDO.setOptionsByHelmExt(request.getHelmExt().getJSONObject("repo"));
        helmMetaService.create(helmMetaDO);
        return helmMetaDtoConvert.to(helmMetaDO);
    }

    @Override
    public HelmMetaDTO update(HelmMetaUpdateReq request) {
        HelmMetaDO helmMetaDO = new HelmMetaDO();
        ClassUtil.copy(request, helmMetaDO);
        HelmMetaQueryCondition condition = HelmMetaQueryCondition.builder()
                .id(helmMetaDO.getId())
                .appId(helmMetaDO.getAppId())
                .namespaceId(helmMetaDO.getNamespaceId())
                .stageId(helmMetaDO.getStageId())
                .name(helmMetaDO.getName()).build();
        if (Objects.nonNull(request.getPackageType())) {
            helmMetaDO.setPackageType(request.getPackageType().name());
        }
        if (!CollectionUtils.isEmpty(request.getHelmExt())) {
            helmMetaDO.setHelmExt(request.getHelmExt().toJSONString());
        }

        helmMetaService.update(helmMetaDO, condition);
        HelmMetaDO updatedHelmMetaDO = helmMetaService
                .get(helmMetaDO.getId(), helmMetaDO.getNamespaceId(), helmMetaDO.getStageId());
        return helmMetaDtoConvert.to(updatedHelmMetaDO);
    }

    @Override
    public boolean delete(Long id, String namespaceId, String stageId) {
        if (Objects.isNull(id)) {
            return true;
        }
        helmMetaService.delete(id, namespaceId, stageId);
        return true;
    }
}
