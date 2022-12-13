package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.RuleLevelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 规则级别信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Mapper
public interface RuleLevelDao extends BaseDao<RuleLevelEntity> {

}
