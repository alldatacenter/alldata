package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.ScheduleJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 数据质量监控任务信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Mapper
@Repository
public interface ScheduleJobDao extends BaseDao<ScheduleJobEntity> {

}
