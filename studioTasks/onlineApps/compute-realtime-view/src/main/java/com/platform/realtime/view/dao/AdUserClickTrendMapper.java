package com.platform.realtime.view.dao;

import com.platform.realtime.view.module.AdClickTrend;
import com.platform.realtime.view.common.base.IBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdUserClickTrendMapper extends IBaseMapper<AdClickTrend> {
    List<AdClickTrend> selectByDate(@Param("date") String date);
}