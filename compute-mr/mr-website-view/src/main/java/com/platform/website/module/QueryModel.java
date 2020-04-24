package com.platform.website.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 查询的model类
 *
 * @author wulinhao
 */
@Data
public class QueryModel {

  private String queryId; // 唯一定位的id，每个api不同
  private String bucket;
  private Map<String, Set<String>> metrics = new HashMap<String, Set<String>>();
  private Set<String> groups = null;
  private QueryColumn queryColumn;
  private int kpiDimensionId;

  public QueryModel() {

  }

  public QueryModel(String queryId) {
    this.queryId = queryId;
  }

  public Set<String> getFields(String metric) {
    return metrics.get(metric);
  }

  public void addFields(String metric, Set<String> fields) {
    Set<String> addedfields = this.metrics.get(metric);
    addedfields.addAll(fields);
    this.metrics.put(metric, addedfields);
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public Map<String, Set<String>> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<String, Set<String>> metrics) {
    this.metrics = metrics;
  }

  public void addMetric(String metric) {
    this.metrics.put(metric, new HashSet<String>());
  }

  public Set<String> getGroups() {
    return groups;
  }

  public void setGroups(Set<String> groups) {
    this.groups = new HashSet<String>(groups);
  }

  public String getStrFields() {
    // 返回如 new_user, new_install形式
    return "";
  }

  public String getQueryId() {
    return queryId;
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public QueryColumn getQueryColumn() {
    return queryColumn;
  }

  public void setQueryColumn(QueryColumn queryColumn) {
    try {
      this.queryColumn = (QueryColumn) queryColumn.clone();
    } catch (CloneNotSupportedException e) {
      this.queryColumn = queryColumn;
    }
  }

  public int getKpiDimensionId() {
    return kpiDimensionId;
  }

  public void setKpiDimensionId(int kpiDimensionId) {
    this.kpiDimensionId = kpiDimensionId;
  }

  public void addFields(String metricItem, String columns) {
    if (StringUtils.isEmpty(columns)) {
      return;
    }

    String[] cols = StringUtils.split(columns, ",");
    Set<String> colSet = new HashSet<String>();
    for (String col : cols) {
      colSet.add(col);
    }
    addFields(metricItem, colSet);
  }
}