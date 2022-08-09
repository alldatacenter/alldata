package com.platform.website.dao;

import com.platform.website.module.InboundDimension;
import org.apache.ibatis.annotations.Param;

public interface InboundDimensionMapper {

  InboundDimension getInboundDimensionById(@Param("id") int id);

  InboundDimension getInboundDimensionByName(@Param("name") String name);
}
