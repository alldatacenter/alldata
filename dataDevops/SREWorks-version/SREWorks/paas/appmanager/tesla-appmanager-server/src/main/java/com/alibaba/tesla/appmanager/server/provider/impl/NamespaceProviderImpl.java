package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.NamespaceProvider;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.JsonUtil;
import com.alibaba.tesla.appmanager.domain.dto.NamespaceDTO;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceCreateReq;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceQueryReq;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceUpdateReq;
import com.alibaba.tesla.appmanager.server.assembly.NamespaceDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.NamespaceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.NamespaceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 命名空间服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class NamespaceProviderImpl implements NamespaceProvider {

    @Autowired
    private NamespaceRepository namespaceRepository;

    @Resource
    private NamespaceDtoConvert namespaceConvert;

    /**
     * 根据条件查询 Namespaces
     *
     * @param request 请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<NamespaceDTO> queryByCondition(NamespaceQueryReq request) {
        NamespaceQueryCondition condition = new NamespaceQueryCondition();
        ClassUtil.copy(request, condition);
        List<NamespaceDO> results = namespaceRepository.selectByCondition(condition);
        return Pagination.valueOf(results, item -> namespaceConvert.to(item));
    }

    /**
     * 获取指定 namespaceId 对应的数据
     *
     * @param namespaceId 命名空间 ID
     * @return 单条记录，如果存不在则返回 null
     */
    @Override
    public NamespaceDTO get(String namespaceId) {
        NamespaceQueryCondition condition = NamespaceQueryCondition.builder().namespaceId(namespaceId).build();
        List<NamespaceDO> namespaces = namespaceRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(namespaces)) {
            return null;
        }
        if (namespaces.size() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple namespaces found, namespaceId=%s", namespaceId));
        }
        return namespaceConvert.to(namespaces.get(0));
    }

    /**
     * 创建 Namespace
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    @Override
    public NamespaceDTO create(NamespaceCreateReq request, String operator) {
        String namespaceId = request.getNamespaceId();
        if (checkExist(namespaceId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("the specified namespaceId %s already exists", namespaceId));
        }
        NamespaceDO namespaceDO = NamespaceDO.builder()
                .namespaceId(namespaceId)
                .namespaceName(request.getNamespaceName())
                .namespaceExt(JsonUtil.toJsonString(request.getNamespaceExt()))
                .namespaceCreator(operator)
                .namespaceModifier(operator)
                .build();
        namespaceRepository.insert(namespaceDO);
        return get(namespaceId);
    }

    /**
     * 更新指定的 Namespace
     *
     * @param namespaceId 定位 NamespaceId
     * @param request     请求更新数据
     * @param operator    操作人
     * @return 更新后的数据内容
     */
    @Override
    public NamespaceDTO update(String namespaceId, NamespaceUpdateReq request, String operator) {
        NamespaceDO namespaceDO = NamespaceDO.builder()
                .namespaceName(request.getNamespaceName())
                .namespaceExt(JsonUtil.toJsonString(request.getNamespaceExt()))
                .namespaceModifier(operator)
                .build();
        namespaceRepository.updateByCondition(
                namespaceDO,
                NamespaceQueryCondition.builder().namespaceId(namespaceId).build()
        );
        return get(namespaceId);
    }

    /**
     * 删除指定的 Namespace
     *
     * @param namespaceId 定位 NamespaceId
     * @param operator    操作人
     */
    @Override
    public void delete(String namespaceId, String operator) {
        NamespaceQueryCondition condition = NamespaceQueryCondition.builder().namespaceId(namespaceId).build();
        namespaceRepository.deleteByCondition(condition);
    }

    /**
     * 检测指定 namespaceId 是否存在对应记录
     *
     * @param namespaceId 定位 NamespaceId
     * @return true or false
     */
    private boolean checkExist(String namespaceId) {
        NamespaceDTO namespaceDTO = get(namespaceId);
        return namespaceDTO != null;
    }
}
