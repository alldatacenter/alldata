package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.NamespaceDTO;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceCreateReq;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceQueryReq;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceUpdateReq;

/**
 * 命名空间服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface NamespaceProvider {

    /**
     * 根据条件查询 Namespaces
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<NamespaceDTO> queryByCondition(NamespaceQueryReq request);

    /**
     * 获取指定 namespaceId 对应的数据
     *
     * @param namespaceId 命名空间 ID
     * @return 单条记录，如果存不在则返回 null
     */
    NamespaceDTO get(String namespaceId);

    /**
     * 创建 Namespace
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    NamespaceDTO create(NamespaceCreateReq request, String operator);

    /**
     * 更新指定的 Namespace
     *
     * @param namespaceId 定位 NamespaceId
     * @param request     请求更新数据
     * @param operator    操作人
     * @return 更新后的数据内容
     */
    NamespaceDTO update(String namespaceId, NamespaceUpdateReq request, String operator);

    /**
     * 删除指定的 Namespace
     *
     * @param namespaceId 定位 NamespaceId
     * @param operator    操作人
     */
    void delete(String namespaceId, String operator);
}
