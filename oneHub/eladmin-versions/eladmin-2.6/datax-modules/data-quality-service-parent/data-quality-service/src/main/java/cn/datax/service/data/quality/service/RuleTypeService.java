package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.RuleTypeEntity;
import cn.datax.common.base.BaseService;
import cn.datax.service.data.quality.api.entity.RuleTypeReportEntity;

import java.util.List;

/**
 * <p>
 * 规则类型信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
public interface RuleTypeService extends BaseService<RuleTypeEntity> {

    RuleTypeEntity getRuleTypeById(String id);

    List<RuleTypeReportEntity> getRuleTypeListForReport();
}
