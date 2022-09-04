package com.alibaba.sreworks.pmdb.domain.metric;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface MetricAnomalyDetectionConfigMapper {
    long countByExample(MetricAnomalyDetectionConfigExample example);

    int deleteByExample(MetricAnomalyDetectionConfigExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(MetricAnomalyDetectionConfig record);

    int insertSelective(MetricAnomalyDetectionConfig record);

    List<MetricAnomalyDetectionConfig> selectByExampleWithBLOBsWithRowbounds(MetricAnomalyDetectionConfigExample example, RowBounds rowBounds);

    List<MetricAnomalyDetectionConfig> selectByExampleWithBLOBs(MetricAnomalyDetectionConfigExample example);

    List<MetricAnomalyDetectionConfig> selectByExampleWithRowbounds(MetricAnomalyDetectionConfigExample example, RowBounds rowBounds);

    List<MetricAnomalyDetectionConfig> selectByExample(MetricAnomalyDetectionConfigExample example);

    MetricAnomalyDetectionConfig selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") MetricAnomalyDetectionConfig record, @Param("example") MetricAnomalyDetectionConfigExample example);

    int updateByExampleWithBLOBs(@Param("record") MetricAnomalyDetectionConfig record, @Param("example") MetricAnomalyDetectionConfigExample example);

    int updateByExample(@Param("record") MetricAnomalyDetectionConfig record, @Param("example") MetricAnomalyDetectionConfigExample example);

    int updateByPrimaryKeySelective(MetricAnomalyDetectionConfig record);

    int updateByPrimaryKeyWithBLOBs(MetricAnomalyDetectionConfig record);

    int updateByPrimaryKey(MetricAnomalyDetectionConfig record);
}