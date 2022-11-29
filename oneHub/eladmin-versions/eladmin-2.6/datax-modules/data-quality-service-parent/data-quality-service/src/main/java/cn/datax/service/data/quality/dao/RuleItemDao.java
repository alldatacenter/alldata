package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.RuleItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 规则核查项信息表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-10-15
 */
@Mapper
public interface RuleItemDao extends BaseDao<RuleItemEntity> {

}
