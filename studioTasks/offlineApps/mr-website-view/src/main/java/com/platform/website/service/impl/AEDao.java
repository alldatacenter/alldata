package com.platform.website.service.impl;

import com.platform.website.module.QueryModel;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AEDao extends BaseDao {


  /**
   * 获得data数据,最终所有的请求具体的用户数据都是通过该接口来的，获取dimension除外
   */
  public List<Map<String, Object>> fetchMetricData(QueryModel queryModel) {
    return processWithNoCache(queryModel);
  }

  public List<Map<String, Object>> processWithNoCache(QueryModel queryModel) {
    return this.getSqlSession().selectList(queryModel.getQueryId(), queryModel);
  }

}
