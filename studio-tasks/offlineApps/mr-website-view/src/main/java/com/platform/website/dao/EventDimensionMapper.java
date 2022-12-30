package com.platform.website.dao;

import com.platform.website.module.EventDimension;
import org.apache.ibatis.annotations.Param;

public interface EventDimensionMapper {

  EventDimension getEventDimensionById(@Param("id") int id);

  EventDimension getEventDimensionByCategoryAndAction(@Param("category") String category,
      @Param("action") String action);
}
