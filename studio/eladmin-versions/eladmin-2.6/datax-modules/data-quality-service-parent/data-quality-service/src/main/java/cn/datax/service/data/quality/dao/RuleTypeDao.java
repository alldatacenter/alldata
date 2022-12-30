package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.RuleTypeEntity;
import cn.datax.service.data.quality.api.entity.RuleTypeReportEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 规则类型信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Mapper
public interface RuleTypeDao extends BaseDao<RuleTypeEntity> {

    List<RuleTypeReportEntity> selectListForReport();
}
