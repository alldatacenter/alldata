package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.CheckReportEntity;
import cn.datax.common.base.BaseService;
import cn.datax.service.data.quality.api.entity.DataReportEntity;
import cn.datax.service.data.quality.api.query.CheckReportQuery;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 核查报告信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
public interface CheckReportService extends BaseService<CheckReportEntity> {

    CheckReportEntity getCheckReportById(String id);

    /**
     * 按数据源统计
     * @return
     */
    List<DataReportEntity> getReportBySource(String checkDate);

    /**
     * 按规则类型统计
     * @return
     */
    List<DataReportEntity> getReportByType(String checkDate);

    Map<String, Object> getReportDetail(String checkDate);
}
