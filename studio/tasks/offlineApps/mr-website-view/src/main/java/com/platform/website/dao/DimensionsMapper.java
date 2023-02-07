package com.platform.website.dao;

import java.util.List;
import java.util.Map;

public interface DimensionsMapper {

  List<Map<String, Object>> queryDimensionData(Map<String, String> paramMap);
}
