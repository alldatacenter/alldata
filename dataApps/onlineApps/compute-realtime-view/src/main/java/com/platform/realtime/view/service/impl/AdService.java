package com.platform.realtime.view.service.impl;

import com.platform.realtime.view.module.AdUserClickCount;
import com.platform.realtime.view.service.IAdService;
import com.platform.realtime.view.common.base.BaseService;
import com.platform.realtime.view.common.base.IBaseMapper;
import com.platform.realtime.view.dao.AdUserClickCountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("adService")
public class AdService extends BaseService<AdUserClickCount> implements IAdService {

  @Autowired
  private AdUserClickCountMapper adUserClickCountMapper;

  @Override
  public List<AdUserClickCount> findByDate(String date) {
    List<AdUserClickCount> adUserClickCounts = adUserClickCountMapper.selectByDate(date);
    return adUserClickCounts;
  }

  @Override
  public IBaseMapper<AdUserClickCount> getBaseMapper() {
    return adUserClickCountMapper;
  }
}
