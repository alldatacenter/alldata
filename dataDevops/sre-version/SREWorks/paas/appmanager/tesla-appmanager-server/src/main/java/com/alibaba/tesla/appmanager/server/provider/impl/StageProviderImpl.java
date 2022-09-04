package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.StageProvider;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.JsonUtil;
import com.alibaba.tesla.appmanager.domain.dto.StageDTO;
import com.alibaba.tesla.appmanager.domain.req.stage.StageCreateReq;
import com.alibaba.tesla.appmanager.domain.req.stage.StageQueryReq;
import com.alibaba.tesla.appmanager.domain.req.stage.StageUpdateReq;
import com.alibaba.tesla.appmanager.server.assembly.EnvDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.EnvRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.EnvQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.EnvDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * Stage 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class StageProviderImpl implements StageProvider {

    @Autowired
    private EnvRepository envRepository;

    @Resource
    private EnvDtoConvert envDtoConvert;

    /**
     * 根据条件查询 Stages
     *
     * @param namespaceId NamespaceId
     * @param request     请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<StageDTO> list(String namespaceId, StageQueryReq request) {
        EnvQueryCondition condition = EnvQueryCondition.builder()
                .namespaceId(namespaceId)
                .envId(request.getStageId())
                .envCreator(request.getStageCreator())
                .envModifier(request.getStageModifier())
                .envName(request.getStageName())
                .production(request.getProduction())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .pagination(request.isPagination())
                .build();
        List<EnvDO> stages = envRepository.selectByCondition(condition);
        return Pagination.valueOf(stages, item -> envDtoConvert.to(item));
    }

    /**
     * 根据条件查询 Stages
     *
     * @param request 请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<StageDTO> list(StageQueryReq request) {
        EnvQueryCondition condition = EnvQueryCondition.builder()
                .envId(request.getStageId())
                .envCreator(request.getStageCreator())
                .envModifier(request.getStageModifier())
                .envName(request.getStageName())
                .production(request.getProduction())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .pagination(request.isPagination())
                .build();
        List<EnvDO> stages = envRepository.selectByCondition(condition);
        return Pagination.valueOf(stages, item -> envDtoConvert.to(item));
    }

    /**
     * 获取指定环境对应的数据
     *
     * @param namespaceId NamespaceId
     * @param stageId     StageId
     * @return 单条记录，如果存不在则返回 null
     */
    @Override
    public StageDTO get(String namespaceId, String stageId) {
        EnvQueryCondition condition = EnvQueryCondition.builder()
                .namespaceId(namespaceId)
                .envId(stageId)
                .build();
        List<EnvDO> records = envRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        if (records.size() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple stages found, namespaceId=%s, stageId=%s", namespaceId, stageId));
        }
        return envDtoConvert.to(records.get(0));
    }

    /**
     * 更新指定的 Stage
     *
     * @param namespaceId NamespaceId
     * @param stageId     StageId
     * @param request     请求更新数据
     * @param operator    操作人
     * @return 更新后的数据内容
     */
    @Override
    public StageDTO update(String namespaceId, String stageId, StageUpdateReq request, String operator) {
        EnvDO envDO = EnvDO.builder()
                .envName(request.getStageName())
                .envExt(JsonUtil.toJsonString(request.getStageExt()))
                .envModifier(operator)
                .build();
        envRepository.updateByCondition(
                envDO,
                EnvQueryCondition.builder().namespaceId(namespaceId).envId(stageId).build()
        );
        return get(namespaceId, stageId);
    }

    /**
     * 创建 Stage
     *
     * @param namespaceId NamespaceId
     * @param request     请求数据
     * @param operator    操作人
     * @return 创建后的数据内容
     */
    @Override
    public StageDTO create(String namespaceId, StageCreateReq request, String operator) {
        String stageId = request.getStageId();
        if (checkExist(namespaceId, stageId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("the specified env (%s|%s) already exists", namespaceId, stageId));
        }
        EnvDO envDO = EnvDO.builder()
                .namespaceId(namespaceId)
                .envId(stageId)
                .envName(request.getStageName())
                .envExt(JsonUtil.toJsonString(request.getStageExt()))
                .envCreator(operator)
                .envModifier(operator)
                .build();
        envRepository.insert(envDO);
        return get(namespaceId, stageId);
    }

    /**
     * 删除指定的 Stage
     *
     * @param namespaceId NamespaceId
     * @param stageId     StageId
     * @param operator    操作人
     */
    @Override
    public void delete(String namespaceId, String stageId, String operator) {
        EnvQueryCondition condition = EnvQueryCondition.builder()
                .namespaceId(namespaceId)
                .envId(stageId)
                .build();
        envRepository.deleteByCondition(condition);
    }

    /**
     * 检测指定环境是否存在对应记录
     *
     * @param namespaceId 定位 NamespaceId
     * @param stageId     定位 StageId
     * @return true or false
     */
    private boolean checkExist(String namespaceId, String stageId) {
        StageDTO record = get(namespaceId, stageId);
        return record != null;
    }
}
