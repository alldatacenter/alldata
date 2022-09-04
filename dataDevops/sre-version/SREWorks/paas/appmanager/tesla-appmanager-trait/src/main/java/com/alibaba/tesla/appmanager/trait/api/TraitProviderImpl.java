package com.alibaba.tesla.appmanager.trait.api;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.TraitProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.dto.TraitDTO;
import com.alibaba.tesla.appmanager.domain.req.trait.TraitQueryReq;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.assembly.TraitDtoConvert;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.service.TraitService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Trait API 服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class TraitProviderImpl implements TraitProvider {

    @Autowired
    private TraitService traitService;

    @Autowired
    private TraitDtoConvert traitDtoConvert;

    /**
     * 根据指定条件查询对应的 Trait 列表
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return PageInfo of Trait DTO
     */
    @Override
    public Pagination<TraitDTO> list(TraitQueryReq request, String operator) {
        TraitQueryCondition condition = new TraitQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<TraitDO> results = traitService.list(condition, operator);
        return Pagination.transform(results, item -> traitDtoConvert.to(item));
    }

    /**
     * 根据指定条件查询对应的 Trait 列表 (期望仅返回一个)
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return Trait DTO, 不存在则返回 null，存在多个则抛出异常
     */
    @Override
    public TraitDTO get(TraitQueryReq request, String operator) {
        TraitQueryCondition condition = TraitQueryCondition.builder()
            .name(request.getName())
            .withBlobs(true)
            .build();
        TraitDO result = traitService.get(condition, operator);
        return traitDtoConvert.to(result);
    }

    /**
     * 向系统中新增或更新一个 Trait
     *  @param request  记录的值
     * @param operator 操作人
     */
    @Override
    public void apply(String request, String operator) {
        TraitDefinition traitDefinition = SchemaUtil.toSchema(TraitDefinition.class, request);

        // className 仅在 plugins 模型下可用，新 groovy 形式 trait 不需要该内容
        String className = traitDefinition.getSpec().getClassName();
        if (StringUtils.isEmpty(className)) {
            className = "";
        }

        TraitDO traitDO = TraitDO.builder()
            .name(traitDefinition.getMetadata().getName())
            .className(className)
            .definitionRef(traitDefinition.getSpec().getDefinitionRef().getName())
            .traitDefinition(request)
            .build();
        traitService.apply(traitDO, operator);
    }

    /**
     * 删除指定条件的 Trait
     *
     * @param request  条件
     * @param operator 操作人
     * @return 删除的数量 (0 or 1)
     */
    @Override
    public int delete(TraitQueryReq request, String operator) {
        TraitQueryCondition condition = TraitQueryCondition.builder()
            .name(request.getName())
            .build();
        return traitService.delete(condition, operator);
    }

    /**
     * 调用指定 Trait 的 reconcile 过程
     *
     * @param name    Trait 唯一名称
     * @param payload 请求 Payload
     */
    @Override
    public void reconcile(String name, JSONObject payload) {
        traitService.reconcile(name, payload);
    }

    /**
     * 调用指定 Trait 的 reconcile 过程
     *
     * @param name   Trait 唯一名称
     * @param object Reconcile Object
     * @param properties Reconcile Properties
     */
    @Override
    public void reconcile(String name, JSONObject object, JSONObject properties) {
        traitService.reconcile(name, object, properties);
    }
}
