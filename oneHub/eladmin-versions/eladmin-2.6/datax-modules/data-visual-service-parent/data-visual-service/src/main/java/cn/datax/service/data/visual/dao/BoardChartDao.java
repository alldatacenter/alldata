package cn.datax.service.data.visual.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.visual.api.entity.BoardChartEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 可视化看板和图表关联表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-11
 */
@Mapper
public interface BoardChartDao extends BaseDao<BoardChartEntity> {

    void insertBatch(List<BoardChartEntity> list);
}
