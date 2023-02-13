package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import cn.datax.service.data.quality.api.dto.CheckRuleDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 核查规则信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
public interface CheckRuleService extends BaseService<CheckRuleEntity> {

    CheckRuleEntity saveCheckRule(CheckRuleDto checkRule);

    CheckRuleEntity updateCheckRule(CheckRuleDto checkRule);

    CheckRuleEntity getCheckRuleById(String id);

    void deleteCheckRuleById(String id);

    void deleteCheckRuleBatch(List<String> ids);

	CheckRuleEntity getBySourceId(String sourceId);
}
