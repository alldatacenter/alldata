package com.platform.website.dao;

import com.platform.website.module.BrowserDimension;
import org.apache.ibatis.annotations.Param;

public interface BrowserDimensionMapper {

  BrowserDimension getBrowserDimensionById(@Param("id") int id);

  BrowserDimension getBrowserDimensionByNameAndVersion(@Param("browser") String browser, @Param("browserVersion") String browserVersion);

}
