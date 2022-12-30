package com.platform.website.dao;

import com.platform.website.module.QueryModel;

import java.util.List;
import java.util.Map;

public interface UserBehaviorMapper {
  List<Map<String, Object>> getUserStats(QueryModel queryModel);

  List<Map<String, Object>> getHourlyUserStats(QueryModel queryModel);
}
