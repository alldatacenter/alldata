package com.platform.website.service.impl;

import com.platform.website.common.AEConstants;
import com.platform.website.dao.UserBehaviorMapper;
import com.platform.website.module.QueryModel;
import com.platform.website.service.IAEService;
import com.platform.website.utils.AECalculaterUtil;
import java.util.*;
import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service("aeService")
public class AEService implements IAEService {

  @Resource
  ApplicationContext aeContext;

  @Resource
  private UserBehaviorMapper userBehaviorMapper;

  private static AEDao aeDao = new AEDao();

  @Override
  public List<Map<String, Object>> execute(QueryModel queryModel) throws Exception {
    List<Map<String, Object>> metricData = aeDao.fetchMetricData(queryModel);
//    List<Map<String, Object>> metricData = userBehaviorMapper.getUserStats(queryModel);
    if (metricData == null) {
      return new ArrayList<Map<String, Object>>();
    }

    Map<String, Set<String>> metrics = queryModel.getMetrics();
    Set<String> metricKeys = metrics.keySet();
    Set<String> groups = queryModel.getGroups();
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

    for (Map<String, Object> item : metricData) {
      if (item == null) {
        continue;
      }
      Set<String> allColumn = item.keySet();
      Map<String, Object> resMap = new HashMap<String, Object>();

      for (String metricKey : metricKeys) {
        Set<String> metricColumns = metrics.get(metricKey);
        Map<String, Object> metricColumnValues = new HashMap<String, Object>();
        for (String metricColumn : metricColumns) {
          Object metricColumnValue = item.get(metricColumn);
          if (metricColumnValue == null) {
            metricColumnValue = 0;
          }
          if (AEConstants.KPI_NAME.equals(metricColumn)) {
            metricColumnValues.put(metricColumn, metricColumnValue);
          } else {
            metricColumnValues.put(AEConstants.PRIFIX + metricColumn, metricColumnValue);
          }
        }

        if (!metricKey.equals(AEConstants.ACTIVE_USER_RATE)) {
          resMap.putAll(metricColumnValues);
        } else {
          Object calculateRes = AECalculaterUtil.calculate(metricColumnValues);
          if (calculateRes instanceof Map) {
            resMap.putAll((Map<String, Object>) calculateRes);
          } else {
            resMap.put(AEConstants.PRIFIX + metricKey, calculateRes);
          }
        }
      }

      if (groups != null) {
        for (String column : allColumn) {
          if (column.startsWith(AEConstants.GROUP_FLAG)) {
            String c = column.replace(AEConstants.GROUP_FLAG, AEConstants.EMPTY_STR);
            resMap.put(c, item.get(column));
          }
        }
      }

      result.add(resMap);
    }
    return result;
  }
}
