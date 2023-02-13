package com.platform.realtime.view.service;

import com.platform.realtime.view.module.AdClickTrend;
import com.platform.realtime.view.common.base.IBaseService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IAdTrendService extends IBaseService<AdClickTrend> {
  public List<AdClickTrend> findByDate(@Param("date") String date);
}
