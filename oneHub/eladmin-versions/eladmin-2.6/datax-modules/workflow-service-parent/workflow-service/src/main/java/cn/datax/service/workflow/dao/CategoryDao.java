package cn.datax.service.workflow.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.workflow.api.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 流程分类表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@Mapper
public interface CategoryDao extends BaseDao<CategoryEntity> {

}
