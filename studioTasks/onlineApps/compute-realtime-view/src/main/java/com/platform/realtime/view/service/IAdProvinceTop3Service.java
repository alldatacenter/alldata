package com.platform.realtime.view.service;

import com.platform.realtime.view.module.AdProvinceTop3;
import com.platform.realtime.view.common.base.IBaseService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IAdProvinceTop3Service extends IBaseService<AdProvinceTop3> {
  public List<AdProvinceTop3> findByDate(@Param("date") String date);
}
