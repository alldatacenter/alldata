package com.linkedin.feathr.compute.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.linkedin.feathr.compute.AggregationType;
import com.linkedin.feathr.compute.LateralViewArray;
import com.linkedin.feathr.compute.SlidingWindowFeature;
import com.linkedin.feathr.compute.SqlExpression;
import com.linkedin.feathr.compute.Window;
import com.linkedin.feathr.core.config.TimeWindowAggregationType;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;


public class SlidingWindowAggregationBuilder extends SlidingWindowOperationBuilder<SlidingWindowFeature> {
  private static final SlidingWindowAggregationBuilder
      INSTANCE = new SlidingWindowAggregationBuilder();

  private static final Map<TimeWindowAggregationType, AggregationType> AGGREGATION_TYPE_MAP = new HashMap<TimeWindowAggregationType, AggregationType>() {
    {
      put(TimeWindowAggregationType.AVG, AggregationType.AVG);
      put(TimeWindowAggregationType.MIN, AggregationType.MIN);
      put(TimeWindowAggregationType.MAX, AggregationType.MAX);
      put(TimeWindowAggregationType.SUM, AggregationType.SUM);
      put(TimeWindowAggregationType.COUNT, AggregationType.COUNT);
      put(TimeWindowAggregationType.LATEST, AggregationType.LATEST);
      put(TimeWindowAggregationType.AVG_POOLING, AggregationType.AVG_POOLING);
      put(TimeWindowAggregationType.MAX_POOLING, AggregationType.MAX_POOLING);
      put(TimeWindowAggregationType.MIN_POOLING, AggregationType.MIN_POOLING);
    }};

  private SlidingWindowAggregationBuilder() {
  }

  public static SlidingWindowAggregationBuilder getInstance() {
    return INSTANCE;
  }

  public static boolean isSlidingWindowAggregationType(TimeWindowAggregationType timeWindowAggregationType) {
    return AGGREGATION_TYPE_MAP.containsKey(timeWindowAggregationType);
  }

  @Override
  SlidingWindowFeature buildSlidingWindowOperationObject(@Nullable String filterStr, @Nullable String groupByStr,
      @Nullable Integer limit, @Nonnull Window window, @NonNull String targetColumnStr,
      @NonNull LateralViewArray lateralViews, @NonNull TimeWindowAggregationType timeWindowAggregationType) {
    Preconditions.checkNotNull(window);
    Preconditions.checkNotNull(timeWindowAggregationType);
    Preconditions.checkNotNull(targetColumnStr);
    Preconditions.checkNotNull(lateralViews);
    SlidingWindowFeature slidingWindowAggregation = new SlidingWindowFeature();
    if (filterStr != null) {
      SqlExpression sparkSqlExpression = new SqlExpression();
      sparkSqlExpression.setSql(filterStr);
      SlidingWindowFeature.Filter filter = new SlidingWindowFeature.Filter();
      filter.setSqlExpression(sparkSqlExpression);
      slidingWindowAggregation.setFilter(filter);
    }
    if (groupByStr != null) {
      SlidingWindowFeature.GroupBy groupBy = new SlidingWindowFeature.GroupBy();
      SqlExpression sparkSqlExpression = new SqlExpression();
      sparkSqlExpression.setSql(groupByStr);
      groupBy.setSqlExpression(sparkSqlExpression);
      slidingWindowAggregation.setGroupBy(groupBy);
    }
    if (limit != null) {
      slidingWindowAggregation.setLimit(limit);
    }
    slidingWindowAggregation.setWindow(window);
    AggregationType aggregationType = AGGREGATION_TYPE_MAP.get(timeWindowAggregationType);
    if (aggregationType == null) {
      throw new IllegalArgumentException(String.format("Unsupported aggregation type %s for SlidingWindowAggregation."
          + "Supported types are %s", timeWindowAggregationType, AGGREGATION_TYPE_MAP.keySet()));
    }
    slidingWindowAggregation.setAggregationType(aggregationType);
    SlidingWindowFeature.TargetColumn targetColumn = new SlidingWindowFeature.TargetColumn();
    SqlExpression sparkSqlExpression = new SqlExpression();
    sparkSqlExpression.setSql(targetColumnStr);
    targetColumn.setSqlExpression(sparkSqlExpression);
    slidingWindowAggregation.setTargetColumn(targetColumn);
    slidingWindowAggregation.setLateralViews(lateralViews);
    return slidingWindowAggregation;
  }
}

