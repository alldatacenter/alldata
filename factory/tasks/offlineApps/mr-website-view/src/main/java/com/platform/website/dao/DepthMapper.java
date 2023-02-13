package com.platform.website.dao;

import com.platform.website.module.QueryModel;

import java.util.List;
import java.util.Map;

public interface DepthMapper {
  List<Map<String, Object>> getActiveUserDepthStats(QueryModel queryModel);

  List<Map<String, Object>> getSessionDepthStats(QueryModel queryModel);
}
