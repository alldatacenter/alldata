package cn.datax.service.data.visual.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.visual.api.entity.ScreenChartEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 可视化酷屏和图表关联表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-12-15
 */
@Mapper
public interface ScreenChartDao extends BaseDao<ScreenChartEntity> {

    void insertBatch(List<ScreenChartEntity> list);
}
