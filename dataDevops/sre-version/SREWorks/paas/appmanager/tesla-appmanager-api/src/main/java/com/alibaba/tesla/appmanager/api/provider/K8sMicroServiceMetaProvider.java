package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.K8sMicroServiceMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQuickUpdateReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaUpdateByOptionReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaUpdateReq;

import java.util.List;

/**
 * K8S 微应用元信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface K8sMicroServiceMetaProvider {

    /**
     * 分页查询微应用元信息
     */
    Pagination<K8sMicroServiceMetaDTO> list(K8sMicroServiceMetaQueryReq request);

    /**
     * 通过微应用 ID 查询微应用元信息
     */
    K8sMicroServiceMetaDTO get(Long id);

    /**
     * 通过条件查询微应用元信息
     */
    K8sMicroServiceMetaDTO get(K8sMicroServiceMetaQueryReq request);

    /**
     * 通过微应用 ID 删除微应用元信息
     */
    boolean delete(Long id);

    /**
     * 创建 K8S Microservice
     *
     * @param dto K8S Microservice DTO 对象
     * @return K8s Microservice DTO 对象
     */
    K8sMicroServiceMetaDTO create(K8sMicroServiceMetaDTO dto);

    /**
     * 创建 K8S Microservice (普通)
     */
    K8sMicroServiceMetaDTO create(K8sMicroServiceMetaUpdateReq request);

    /**
     * 创建 K8S Microservice (快速)
     */
    K8sMicroServiceMetaDTO create(K8sMicroServiceMetaQuickUpdateReq request);

    /**
     * 根据 options Yaml (build.yaml) 创建或更新 K8s MicroService
     */
    List<K8sMicroServiceMetaDTO> updateByOption(K8sMicroServiceMetaUpdateByOptionReq request);

    /**
     * 更新 K8S Microservice
     *
     * @param dto K8S Microservice DTO 对象
     * @return K8s Microservice DTO 对象
     */
    K8sMicroServiceMetaDTO update(K8sMicroServiceMetaDTO dto);

    /**
     * 更新 K8s Microservice
     */
    K8sMicroServiceMetaDTO update(K8sMicroServiceMetaUpdateReq request);

    /**
     * 快速更新 K8S Microservice
     */
    K8sMicroServiceMetaDTO update(K8sMicroServiceMetaQuickUpdateReq request);
}
