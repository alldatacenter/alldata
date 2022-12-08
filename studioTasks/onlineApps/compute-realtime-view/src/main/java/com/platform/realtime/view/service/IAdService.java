package com.platform.realtime.view.service;

import com.platform.realtime.view.module.AdUserClickCount;
import com.platform.realtime.view.common.base.IBaseService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IAdService extends IBaseService<AdUserClickCount> {
  public List<AdUserClickCount> findByDate(@Param("date") String date);
}
