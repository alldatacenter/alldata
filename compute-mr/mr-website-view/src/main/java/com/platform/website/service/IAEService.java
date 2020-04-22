package com.platform.website.service;

import com.platform.website.module.QueryModel;

import java.util.*;

public interface IAEService {
  public List<Map<String, Object>> execute(QueryModel queryModel) throws Exception;
}
