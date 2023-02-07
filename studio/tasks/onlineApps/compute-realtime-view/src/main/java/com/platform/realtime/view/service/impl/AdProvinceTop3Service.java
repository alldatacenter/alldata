package com.platform.realtime.view.service.impl;

import com.platform.realtime.view.module.AdProvinceTop3;
import com.platform.realtime.view.service.IAdProvinceTop3Service;
import com.platform.realtime.view.common.base.BaseService;
import com.platform.realtime.view.common.base.IBaseMapper;
import com.platform.realtime.view.dao.AdProvinceTop3Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("adProvinceTop3Service")
public class AdProvinceTop3Service extends BaseService<AdProvinceTop3> implements IAdProvinceTop3Service {

  @Autowired
  private AdProvinceTop3Mapper adProvinceTop3Mapper;

  @Override
  public List<AdProvinceTop3> findByDate(String date) {
    List<AdProvinceTop3> adProvinceTop3s = adProvinceTop3Mapper.selectByDate(date);
    return adProvinceTop3s;
  }

  @Override
  public IBaseMapper<AdProvinceTop3> getBaseMapper() {
    return adProvinceTop3Mapper;
  }
}
