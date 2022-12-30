package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.StageDTO;
import com.alibaba.tesla.appmanager.domain.req.stage.StageCreateReq;
import com.alibaba.tesla.appmanager.domain.req.stage.StageQueryReq;
import com.alibaba.tesla.appmanager.domain.req.stage.StageUpdateReq;

/**
 * Stage 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface StageProvider {

    /**
     * 根据条件查询 Stages
     *
     * @param namespaceId NamespaceId
     * @param request     请求数据
     * @return 查询结果
     */
    Pagination<StageDTO> list(String namespaceId, StageQueryReq request);

    /**
     * 根据条件查询 Stages
     *
     * @param request     请求数据
     * @return 查询结果
     */
    Pagination<StageDTO> list(StageQueryReq request);

    /**
     * 获取指定环境对应的数据
     *
     * @param namespaceId NamespaceId
     * @param stageId     StageId
     * @return 单条记录，如果存不在则返回 null
     */
    StageDTO get(String namespaceId, String stageId);

    /**
     * 创建 Stage
     *
     * @param namespaceId NamespaceId
     * @param request     请求数据
     * @param operator    操作人
     * @return 创建后的数据内容
     */
    StageDTO create(String namespaceId, StageCreateReq request, String operator);

    /**
     * 更新指定的 Stage
     *
     * @param namespaceId NamespaceId
     * @param stageId     StageId
     * @param request     请求更新数据
     * @param operator    操作人
     * @return 更新后的数据内容
     */
    StageDTO update(String namespaceId, String stageId, StageUpdateReq request, String operator);

    /**
     * 删除指定的 Stage
     *
     * @param namespaceId NamespaceId
     * @param stageId     StageId
     * @param operator    操作人
     */
    void delete(String namespaceId, String stageId, String operator);
}
