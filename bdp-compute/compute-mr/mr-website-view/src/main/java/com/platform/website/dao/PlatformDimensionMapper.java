package com.platform.website.dao;

import com.platform.website.module.PlatformDimension;
import org.apache.ibatis.annotations.Param;

public interface PlatformDimensionMapper {

  PlatformDimension getPlatformDimensionById(@Param("id") int id);

  PlatformDimension getPlatformDimensionByPlatformName(@Param("platformName") String platformName);
}
