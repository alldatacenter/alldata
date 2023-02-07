package com.platform.website.controller;

import com.platform.website.common.AEConstants;
import com.platform.website.module.Message;
import com.platform.website.module.QueryColumn;
import com.platform.website.module.QueryModel;
import com.platform.website.service.IAEService;
import com.platform.website.utils.ApplicationContextUtil;
import com.platform.website.utils.SetUtil;
import com.platform.website.utils.TimeUtil;
import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 处理数据请求接口 rest api: /stats/summary/{bucket}?metric=xxx&其他参数 rest api:
 * /stats/groupBy/{bucket}?metric=xxx&group_by=xxx&其他参数
 */

@Controller
public class AEController extends AEBaseController {

  private static final Logger log = Logger.getLogger(AEController.class);

//  private static UserBehaviorSpeedRateService userBehaviorSpeedRateService = new UserBehaviorSpeedRateService();

  /**
   * 处理summary请求
   */
  @RequestMapping(value = "/stats/summary/{" + AEConstants.BUCKET + "}", method = RequestMethod.GET)
  @ResponseBody
  public Object getSummary(@PathVariable(AEConstants.BUCKET) String bucket,
      HttpServletRequest request, HttpServletResponse response)
      throws UnsupportedEncodingException {
    String groupBy = request.getParameter(AEConstants.GROUP_BY);
    if (org.apache.commons.lang.StringUtils.isNotBlank(groupBy)) {
      return Message.badRequest("bucket summary can't be group by");
    }
    return this.doProcess(bucket, request);
  }

  /**
   * 处理group by请求
   */
  @RequestMapping(value = "/stats/groupBy/{" + AEConstants.BUCKET + "}", method = RequestMethod.GET)
  @ResponseBody
  public Object getGroupBy(@PathVariable(AEConstants.BUCKET) String bucket,
      HttpServletRequest request, HttpServletResponse response)
      throws UnsupportedEncodingException {
    String groupBy = request.getParameter(AEConstants.GROUP_BY);
    if (org.apache.commons.lang.StringUtils.isBlank(groupBy)) {
      return Message.badRequest("bucket group by must be give group by parameter");
    }
    return this.doProcess(bucket, request);
  }

  /**
   * 具体的处理方法
   */
  private Object doProcess(String bucket, HttpServletRequest request)
      throws UnsupportedEncodingException {
    // 处理bucket，判断是否存在
    Set<String> bucketMetricKeys = this.bucketMetrics.keySet();



    if (!bucketMetricKeys.contains(bucket)) {
      return Message.badRequest("bucket not support:" + bucket);
    }

    // 处理metric，判断对应的bucket中是否有对应的metric存在
    String metric = request.getParameter(AEConstants.METRIC);
    if (org.apache.commons.lang.StringUtils.isBlank(metric)) {
      return Message.badRequest("metric can't set to empty");
    }
    String[] metrics = org.apache.commons.lang.StringUtils
        .split(metric, AEConstants.SEPARTION_COMMA);
    Set<Object> bucketMetricValues = this.bucketMetrics.get(bucket);
    for (String metricItem : metrics) {
      if (!SetUtil.contains(bucketMetricValues, metricItem)) {
        return Message
            .badRequest("Metric=" + metricItem + " doesn't exist of the bucket " + bucket);
      }
    }

    // 处理group by条件
    String groupBy = request.getParameter(AEConstants.GROUP_BY);
    Set<String> groups = new HashSet<String>(); // 最终解析存储
    if (org.apache.commons.lang.StringUtils.isNotBlank(groupBy)) {
      Set<Object> groupByMetrics = this.groupByColumns.get(bucket);
      String[] attr = org.apache.commons.lang.StringUtils
          .split(groupBy, AEConstants.SEPARTION_COMMA);
      for (String str : attr) {
        if (!SetUtil.contains(groupByMetrics, str)) {
          return Message.badRequest("Bucket " + bucket + " can't group by " + str);
        }
        groups.add(str);
      }
    }

    // 处理日期
    String startDate = request.getParameter(AEConstants.START_DATE);
    String endDate = request.getParameter(AEConstants.END_DATE);
    if (org.apache.commons.lang.StringUtils
        .isBlank(startDate) || org.apache.commons.lang.StringUtils.isBlank(endDate)) {
      String date = request.getParameter(AEConstants.DATE);
      if (org.apache.commons.lang.StringUtils.isBlank(date)) {
        return Message.badRequest("Please give the date parameters");
      }
      startDate = date;
      endDate = date;
    }
    if (!TimeUtil.checkDate(startDate) || !TimeUtil.checkDate(endDate) || 0 > endDate
        .compareTo(startDate)) {
      return Message.badRequest(
          "Start date = " + startDate + ", end date = " + endDate + " are invalid arguments.");
    }

    // TODO: 本次项目只针对按天group by，如果需要按照周,月,年进行group by操作，那么需要进行两部多的步骤:
    // 1. 根据group by的请求，将日期进行修改
    // 2. 在querycolumn中添加一个属性dateType来区分不同的日期方式(存在)

    // 开始构建我们的查询对象
    QueryColumn queryColumn = new QueryColumn();
    queryColumn.setStartDate(startDate);
    queryColumn.setEndDate(endDate);

    // 处理参数： platform
    boolean paramStatus = true;
    paramStatus = this.setDimensionPlatformId(request, groups, queryColumn);
    if (!paramStatus) {
      return Message.badRequest("Dimension platform id or platform is invalid arguments");
    }

    switch (bucket) {
      case AEConstants.BUCKET_BROWSER:
        paramStatus = this.setDimensionBrowserId(request, groups, queryColumn);
        if (!paramStatus) {
          return Message.noContent("Dimension browser id or browser is invalid argument.");
        }
        break;

      case AEConstants.BUCKET_LOCATION:
        paramStatus = this.setDimensionLocationId(request, groups, queryColumn);
        if (!paramStatus) {
          return Message.noContent("Dimension location id or location(country, province, city) is invalid argument.");
        }
        break;
      case AEConstants.BUCKET_INBOUND:
        paramStatus = this.setDimensionInboundId(request, groups, queryColumn);
        if (!paramStatus) {
          return Message.noContent("Dimension inbound id or inbound is invalid argument.");
        }
        break;
      case AEConstants.BUCKET_EVENT:
        paramStatus = this.setDimensionEventId(request, groups, queryColumn);
        if (!paramStatus) {
          return Message.noContent("Dimension event id or category&action is invalid argument.");
        }
        break;
      case AEConstants.BUCKET_ORDER:
        paramStatus = this.setDimensionCurrencyTypeId(request, groups, queryColumn);
        if (!paramStatus) {
          return Message
              .noContent("Dimension currency type id or currency_type is invalid argument.");
        }
        paramStatus = this.setDimensionPaymentTypeId(request, groups, queryColumn);
        if (!paramStatus) {
          return Message
              .noContent("Dimension payment type id or payment_type is invalid argument.");
        }
        break;
    }

    // 开始创建每个metric对应的QueryModel
    Map<String, QueryModel> queryModels = new HashMap<String, QueryModel>();
    for (String metricItem : metrics) {
      String key = bucket + AEConstants.DELIMITER + metricItem;
      String queryId = this.bucketMetricQueryId.get(key);
      QueryModel model = queryModels.get(queryId);
      if (model == null) {
        model = new QueryModel(queryId);
        model.setBucket(bucket);
        queryModels.put(queryId, model);
      }

      Integer kpiDimensionId = this.dimensionService.getKpiDimensionIdByKpiName(metricItem);
      if (null == kpiDimensionId) {
        model.setKpiDimensionId(0);
      } else {
        model.setKpiDimensionId(kpiDimensionId);
      }

      model.addMetric(metricItem);
      // 返回值
      String columns = this.bucketMetricColumns.get(key);
      model.addFields(metricItem, columns);
      model.setQueryColumn(queryColumn);
      model.setGroups(groups);
    }

    // 进行返回对象定义，以及具体的查询操作
    List<Map<String, Object>> returnValue = null;
    Map<String, Class<?>> keyTypes = new HashMap<String, Class<?>>(); // 保存数据类型

    Set<String> queryIds = queryModels.keySet();
    for (String queryId : queryIds) {
      QueryModel model = queryModels.get(queryId);
      List<Map<String, Object>> dataItem = null;

      IAEService extendedService = null;
      if (ApplicationContextUtil.getApplicationContext().containsBean(queryId)) {
        // 如果存在，表示给定的是一个特定的bean
        extendedService = (IAEService) ApplicationContextUtil.getApplicationContext()
            .getBean(queryId);
      } else {
        extendedService = this.aeService;
      }

      // 获取数据
      try {
        dataItem = extendedService.execute(model);
        System.out.println(dataItem);
      } catch (Throwable e) {
        log.error("服务器执行异常", e);
        return Message.error("Server is error:" + e.getMessage());
      }

      // 处理返回值; 1. 获取类型参数， 方便后期进行填充。2. 将结果添加到returnValue这个map集合中
      this.registerKeyTypeMap(keyTypes, dataItem);

      if (returnValue != null && returnValue.size() != 0) {
        // 非空，填充
        Set<String> groupSet = model.getGroups();
        if (groupSet != null && groupSet.size() > 0) {
          // 进行合并操作
          for (int i = 0; i < returnValue.size(); i++) {
            int j = 0;
            Map<String, Object> oldMap = returnValue.get(i);
            for (; i < dataItem.size(); ) {
              boolean flag = true;
              Map<String, Object> newMap = dataItem.get(j);
              for (String groupColumn : groupSet) {
                if (newMap != null && newMap.size() > 0 && newMap.get(groupColumn) != null) {
                  if (!newMap.get(groupColumn).equals(oldMap.get(groupColumn))) {
                    flag = false;
                    break;
                  }
                }
              }

              if (flag) {
                returnValue.get(i).putAll(dataItem.get(j));
                dataItem.remove(j);
              } else {
                j++;
              }
            }
          }
          returnValue.addAll(dataItem); // 添加其他剩下的数据
        } else {
          // 不是group by操作
          for (int i = 0; i < dataItem.size(); i++) {
            // 由于是按照时间先排序号的，可以直接添加
            returnValue.get(i).putAll(dataItem.get(i));
          }
        }
      } else {
        // 如果是第一次的情况下
        returnValue = dataItem;
      }
    }

    // 进行填充操作
    this.warpReturnValue(keyTypes, returnValue);
    return Message.ok(returnValue);
  }

  /**
   * 将数据类型保存下来，方便进行填充操作
   */
  private void registerKeyTypeMap(Map<String, Class<?>> keyTypes, List<Map<String, Object>> items) {
    for (Map<String, Object> item : items) {
      for (Map.Entry<String, Object> entry : item.entrySet()) {
        keyTypes.put(entry.getKey(), entry.getValue().getClass());
      }
    }
  }

  /**
   * 填充默认数据
   */
  private void warpReturnValue(Map<String, Class<?>> keyTypes, List<Map<String, Object>> items) {
    for (Map<String, Object> item : items) {
      for (Map.Entry<String, Class<?>> entry : keyTypes.entrySet()) {
        if (!item.containsKey(entry.getKey()) && Number.class.isAssignableFrom(entry.getValue())) {
          item.put(entry.getKey(), 0);
        }
      }
    }
  }
}
