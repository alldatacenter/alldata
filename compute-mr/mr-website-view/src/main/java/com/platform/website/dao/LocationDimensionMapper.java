package com.platform.website.dao;

import com.platform.website.module.LocationDimension;
import org.apache.ibatis.annotations.Param;

public interface LocationDimensionMapper {

  LocationDimension getLocationDimensionById(@Param("id") int id);

  LocationDimension getLocationDimensionByLocation(LocationDimension locationDimension);
}
