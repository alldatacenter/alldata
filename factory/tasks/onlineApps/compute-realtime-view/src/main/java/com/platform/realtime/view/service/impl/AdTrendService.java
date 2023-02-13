package com.platform.realtime.view.service.impl;

import com.platform.realtime.view.module.AdClickTrend;
import com.platform.realtime.view.service.IAdTrendService;
import com.platform.realtime.view.common.base.BaseService;
import com.platform.realtime.view.common.base.IBaseMapper;
import com.platform.realtime.view.dao.AdUserClickTrendMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("adTrendService")
public class AdTrendService extends BaseService<AdClickTrend> implements IAdTrendService {

  @Autowired
  private AdUserClickTrendMapper adUserClickTrendMapper;

  @Override
  public List<AdClickTrend> findByDate(String date) {
    List<AdClickTrend> adClickTrends = adUserClickTrendMapper.selectByDate(date);
    return adClickTrends;
  }

  @Override
  public IBaseMapper<AdClickTrend> getBaseMapper() {
    return adUserClickTrendMapper;
  }
}
