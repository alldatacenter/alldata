package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.ScheduleLogEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 数据质量监控任务日志信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
@Mapper
public interface ScheduleLogDao extends BaseDao<ScheduleLogEntity> {

    @Override
    <E extends IPage<ScheduleLogEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<ScheduleLogEntity> queryWrapper);
}
