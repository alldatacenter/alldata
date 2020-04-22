package com.platform.realtime.view.dao;

import com.platform.realtime.view.module.AdProvinceTop3;
import com.platform.realtime.view.common.base.IBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdProvinceTop3Mapper extends IBaseMapper<AdProvinceTop3> {
    List<AdProvinceTop3> selectByDate(@Param("date") String date);
}