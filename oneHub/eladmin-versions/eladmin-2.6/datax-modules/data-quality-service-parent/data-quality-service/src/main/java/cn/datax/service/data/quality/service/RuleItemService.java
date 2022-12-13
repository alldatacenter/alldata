package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.RuleItemEntity;
import cn.datax.common.base.BaseService;

/**
 * <p>
 * 规则核查项信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
public interface RuleItemService extends BaseService<RuleItemEntity> {

    RuleItemEntity getRuleItemById(String id);
}
