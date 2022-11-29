package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.RuleLevelEntity;
import cn.datax.common.base.BaseService;

/**
 * <p>
 * 规则级别信息表 服务类
 * </p>
 *
 * @author yuwei
 * @since 2020-10-14
 */
public interface RuleLevelService extends BaseService<RuleLevelEntity> {

    RuleLevelEntity getRuleLevelById(String id);
}
