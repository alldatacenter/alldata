package cn.datax.service.data.visual.service;

import cn.datax.service.data.visual.api.dto.ChartConfig;
import cn.datax.service.data.visual.api.entity.ChartEntity;
import cn.datax.service.data.visual.api.dto.ChartDto;
import cn.datax.common.base.BaseService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 可视化图表配置信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
public interface ChartService extends BaseService<ChartEntity> {

    ChartEntity saveChart(ChartDto chart);

    ChartEntity updateChart(ChartDto chart);

    ChartEntity getChartById(String id);

    void deleteChartById(String id);

    void deleteChartBatch(List<String> ids);

    void copyChart(String id);

    void buildChart(ChartDto chart);

    Map<String, Object> dataParser(ChartConfig chartConfig);
}
