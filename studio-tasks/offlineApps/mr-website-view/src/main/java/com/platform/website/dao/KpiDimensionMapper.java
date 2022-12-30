package com.platform.website.dao;

import com.platform.website.module.KpiDimension;
import org.apache.ibatis.annotations.Param;

public interface KpiDimensionMapper {
  KpiDimension getKpiDimensionByKpiName(@Param("kpiName") String kpiName);
}
