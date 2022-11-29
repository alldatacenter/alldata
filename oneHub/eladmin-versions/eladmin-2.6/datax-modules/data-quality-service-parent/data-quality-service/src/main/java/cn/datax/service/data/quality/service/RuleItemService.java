package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.RuleItemEntity;
import cn.datax.common.base.BaseService;

/**
 * <p>
 * 规则核查项信息表 服务类
 * </p>
 *
 * @author yuwei
 * @since 2020-10-15
 */
public interface RuleItemService extends BaseService<RuleItemEntity> {

    RuleItemEntity getRuleItemById(String id);
}
