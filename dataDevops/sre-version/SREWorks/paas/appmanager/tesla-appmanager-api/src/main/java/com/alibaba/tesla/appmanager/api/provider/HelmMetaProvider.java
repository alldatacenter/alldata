package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.HelmMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaCreateReq;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaUpdateReq;

public interface HelmMetaProvider {
    /**
     * 分页查询HELM组件信息
     */
    Pagination<HelmMetaDTO> list(HelmMetaQueryReq request);

    /**
     * 通过HELM组件 ID 查询组件元信息
     */
    HelmMetaDTO get(Long id, String namespaceId, String stageId);

    /**
     * 创建应用HELM组件
     */
    HelmMetaDTO create(HelmMetaCreateReq request);

    /**
     * 更新应用HELM组件
     */
    HelmMetaDTO update(HelmMetaUpdateReq request);

    /**
     * 通过HELM组件 ID 删除组件元信息
     */
    boolean delete(Long id, String namespaceId, String stageId);
}
