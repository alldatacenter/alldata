package cn.datax.service.workflow.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.workflow.api.entity.BusinessEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 业务流程配置表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Mapper
@Repository
public interface BusinessDao extends BaseDao<BusinessEntity> {

}
