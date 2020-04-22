package com.platform.website.dao;

import com.platform.website.module.QueryModel;

import java.util.List;
import java.util.Map;

public interface InboundMapper {

  List<Map<String, Object>> getInboundStats(QueryModel queryModel);
}
