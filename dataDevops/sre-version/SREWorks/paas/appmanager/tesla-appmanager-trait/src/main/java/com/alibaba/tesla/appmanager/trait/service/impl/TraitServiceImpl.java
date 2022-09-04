package com.alibaba.tesla.appmanager.trait.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.trait.repository.TraitRepository;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.service.TraitService;
import com.alibaba.tesla.appmanager.trait.service.handler.TraitHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

/**
 * Trait 内部服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class TraitServiceImpl implements TraitService {

    @Autowired
    private TraitRepository traitRepository;

    @Autowired
    private GroovyHandlerFactory handlerFactory;

    /**
     * 根据指定条件查询对应的 trait 列表
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of Trait
     */
    @Override
    public Pagination<TraitDO> list(TraitQueryCondition condition, String operator) {
        List<TraitDO> traits = traitRepository.selectByCondition(condition);
        return Pagination.valueOf(traits, Function.identity());
    }

    /**
     * 根据指定条件查询对应的 trait (期望只返回一个)
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of Trait
     */
    @Override
    public TraitDO get(TraitQueryCondition condition, String operator) {
        List<TraitDO> results = traitRepository.selectByCondition(condition);
        if (results.size() == 0) {
            return null;
        } else if (results.size() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "multiple records found in database");
        } else {
            return results.get(0);
        }
    }

    /**
     * 向系统中新增或更新一个 Trait
     *
     * @param record   记录的值
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(TraitDO record, String operator) {
        assert !StringUtils.isEmpty(record.getName());
        String name = record.getName();
        TraitQueryCondition condition = TraitQueryCondition.builder().name(name).build();
        List<TraitDO> schemaList = traitRepository.selectByCondition(condition);
        assert schemaList.size() <= 1;
        if (schemaList.size() == 0) {
            traitRepository.insert(record);
            log.info("trait has inserted into db|name={}", name);
        } else {
            traitRepository.updateByCondition(record, condition);
            log.info("trait has updated|name={}", name);
        }
    }

    /**
     * 删除指定条件的 Trait
     *
     * @param condition 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    @Override
    public int delete(TraitQueryCondition condition, String operator) {
        return traitRepository.deleteByCondition(condition);
    }

    /**
     * 调用指定 Trait 的 reconcile 过程
     *
     * @param name    Trait 唯一名称
     * @param payload 请求 Payload
     */
    @Override
    public void reconcile(String name, JSONObject payload) {
        TraitHandler handler = handlerFactory.get(TraitHandler.class, DynamicScriptKindEnum.TRAIT.toString(), name);
        handler.reconcile(payload);
    }

    /**
     * 调用指定 Trait 的 reconcile 过程
     *
     * @param name   Trait 唯一名称
     * @param object Reconcile Object
     * @param properties Reconcile Config
     */
    @Override
    public void reconcile(String name, JSONObject object, JSONObject properties) {
        TraitHandler handler = handlerFactory.get(TraitHandler.class, DynamicScriptKindEnum.TRAIT.toString(), name);
        JSONObject payload = new JSONObject();
        payload.put("object", object);
        payload.put("properties", properties);
        handler.reconcile(payload);
    }
}
