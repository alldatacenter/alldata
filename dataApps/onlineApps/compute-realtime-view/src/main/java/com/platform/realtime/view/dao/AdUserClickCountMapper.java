package com.platform.realtime.view.dao;

import com.platform.realtime.view.module.AdUserClickCount;
import com.platform.realtime.view.common.base.IBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdUserClickCountMapper extends IBaseMapper<AdUserClickCount> {
    List<AdUserClickCount> selectByDate(@Param("date") String date);
}