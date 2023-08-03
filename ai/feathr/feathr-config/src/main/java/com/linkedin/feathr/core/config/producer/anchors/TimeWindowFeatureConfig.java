package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.TimeWindowAggregationType;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;


/**
 *
 * This represents 2 types of configs:-
 *  1. a time-window (sliding window) feature offline config.
 * <pre>
 * {@code
 *   <feature name>: {
 *    def: <column name>
 *    aggregation: <aggregation type>
 *    window: <length of window time>
 *    filter: <string>
 *    groupBy: <column name>
 *    limit: <int>
 *    decay: <string>
 *    weight: <string>
 *    embeddingSize: <int>
 *  }
 * }
 * </pre>
 *  2. a nearline feature config
 *  <feature name>: {
 *      def/def.mvel: <column name> // the field on which the aggregation will be computed OR an MVEL expression (use def.mvel)
 *      aggregation: <aggregation type> //aggregation types: SUM, COUNT, MAX, AVG
 *      windowParameters:
 *      {
 *        type: <String> //The window type: SlidingWindow (MVP), FixedWindow (MVP), SessionWindow
 *        size: <Duration> length of window time
 *        slidingInterval: <Duration> // (Optional) Used only by sliding windowin nearline features. Specifies the interval of sliding window starts
 *      }
 *      groupBy: <field name(s)> // (Optional) comma separated columns/fields on which the data will be ‘grouped by’ before aggregation
 *      filter/filter.mvel: <MVEL String> // (Optional) An expression for filtering the fact data before aggregation. For mvel expression, use filter.mvel).
 *    }
 * Details can be referenced in the FeatureDefConfigSchema
 * In the offline world, it is always a sliding window and window in offline is equivalent to size in nearline.
 * So, we convert the offline config to the nearline config, with the only difference being window used in offline, windowParameters used in
 * nearline.
 *
 */
public final class TimeWindowFeatureConfig extends FeatureConfig {
  private final TypedExpr _typedColumnExpr;
  private final TimeWindowAggregationType _aggregation;
  private final WindowParametersConfig _windowParameters;
  private final Optional<TypedExpr> _typedFilter;
  private final Optional<String> _groupBy;
  private final Optional<Integer> _limit;
  private final Optional<String> _decay;
  private final Optional<String> _weight;
  private final Optional<Integer> _embeddingSize;
  private final Optional<FeatureTypeConfig> _featureTypeConfig;
  private final Optional<String> _defaultValue;


  private String _configStr;

  /**
   * Constructor with all parameters
   * @param typedColumnExpr The column/field on which the aggregation will be computed, with the expr type
   * @param aggregation Aggregation type as specified in [[TimeWindowAggregationType]]
   * @param windowParameters windowParameters as specified in [[WindowParametersConfig]]
   * @param typedFilter Spark SQL / MVEL expression for filtering the fact data before aggregation, with expr type
   * @param groupBy column/field on which the data will be grouped by before aggregation
   * @param limit positive integer to limit the number of records for each group
   * @param decay not supported currently
   * @param weight not supported currently
   * @param embeddingSize embedding size
   * @param featureTypeConfig featureTypeConfig for this faeture
   */
  public TimeWindowFeatureConfig(TypedExpr typedColumnExpr, TimeWindowAggregationType aggregation,
      WindowParametersConfig windowParameters, TypedExpr typedFilter, String groupBy, Integer limit,
      String decay, String weight, Integer embeddingSize, FeatureTypeConfig featureTypeConfig, String defaultValue) {
    _typedColumnExpr = typedColumnExpr;
    _aggregation = aggregation;
    _windowParameters = windowParameters;
    _typedFilter = Optional.ofNullable(typedFilter);
    _groupBy = Optional.ofNullable(groupBy);
    _limit = Optional.ofNullable(limit);
    _decay = Optional.ofNullable(decay);
    _weight = Optional.ofNullable(weight);
    _embeddingSize = Optional.ofNullable(embeddingSize);
    _featureTypeConfig = Optional.ofNullable(featureTypeConfig);
    _defaultValue = Optional.ofNullable(defaultValue);

    constructConfigStr();
  }

  /**
   * Constructor with all parameters
   * @param typedColumnExpr The column/field on which the aggregation will be computed, with the expr type
   * @param aggregation Aggregation type as specified in [[TimeWindowAggregationType]]
   * @param windowParameters windowParameters as specified in [[WindowParametersConfig]]
   * @param typedFilter Spark SQL / MVEL expression for filtering the fact data before aggregation, with expr type
   * @param groupBy column/field on which the data will be grouped by before aggregation
   * @param limit positive integer to limit the number of records for each group
   * @param decay not supported currently
   * @param weight not supported currently
   * @param embeddingSize embedding size
   */
  public TimeWindowFeatureConfig(TypedExpr typedColumnExpr, TimeWindowAggregationType aggregation,
      WindowParametersConfig windowParameters, TypedExpr typedFilter, String groupBy, Integer limit, String decay,
      String weight, Integer embeddingSize) {
    this(typedColumnExpr, aggregation, windowParameters, typedFilter, groupBy, limit, decay, weight, embeddingSize,
        null, null);
  }

  /**
   * @param columnExpr The column/field on which the aggregation will be computed
   * @param columnExprType The column/field expr type
   * @param aggregation Aggregation type as specified in [[TimeWindowAggregationType]]
   * @param windowParameters windowParameters as specified in [[WindowParametersConfig]]
   * @param filter Spark SQL / MVEL expression for filtering the fact data before aggregation
   * @param filterExprType the filter expression type
   * @param groupBy column/field on which the data will be grouped by before aggregation
   * @param limit positive integer to limit the number of records for each group
   * @param decay not supported currently
   * @param weight not supported currently
   * @deprecated please use the constructor with all parameters
   */
  public TimeWindowFeatureConfig(String columnExpr, ExprType columnExprType, TimeWindowAggregationType aggregation,
      WindowParametersConfig windowParameters, String filter, ExprType filterExprType, String groupBy, Integer limit,
      String decay, String weight) {
    this(new TypedExpr(columnExpr, columnExprType), aggregation, windowParameters,
        filter == null ? null : new TypedExpr(filter, filterExprType),
        groupBy, limit, decay, weight, null);
  }

  /**
   * Constructor
   * @param columnExpr The column/field on which the aggregation will be computed
   * @param aggregation Aggregation type as specified in [[TimeWindowAggregationType]]
   * @param windowParameters windowParameters as specified in [[WindowParametersConfig]]
   * @param filter Spark SQL expression for filtering the fact data before aggregation
   * @param groupBy column/field on which the data will be grouped by before aggregation
   * @param limit positive integer to limit the number of records for each group
   * @param decay not supported currently
   * @param weight not supported currently
   * @deprecated please use the constructor with all parameters
   */
  @Deprecated
  public TimeWindowFeatureConfig(String columnExpr, TimeWindowAggregationType aggregation, WindowParametersConfig windowParameters,
      String filter, String groupBy, Integer limit,
      String decay, String weight) {
    this(new TypedExpr(columnExpr, ExprType.SQL), aggregation, windowParameters,
        filter == null ? null : new TypedExpr(filter, ExprType.SQL), groupBy, limit, decay, weight, null);
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();

    sb.append(FeatureConfig.DEF).append(": ").append(_typedColumnExpr.getExpr()).append("\n");
    sb.append("def expr type").append(": ").append(_typedColumnExpr.getExprType()).append("\n");
    sb.append(FeatureConfig.AGGREGATION).append(": ").append(_aggregation).append("\n");
    sb.append(FeatureConfig.WINDOW_PARAMETERS).append(": ").append(_windowParameters).append("\n");
    _typedFilter.ifPresent(v -> sb.append(FeatureConfig.FILTER).append(": ").append(v.getExpr()).append("\n").
        append("filter expr type").append(": ").append(v.getExprType()).append("\n"));
    _groupBy.ifPresent(v -> sb.append(FeatureConfig.GROUPBY).append(": ").append(v).append("\n"));
    _limit.ifPresent(v -> sb.append(FeatureConfig.LIMIT).append(": ").append(v).append("\n"));
    _decay.ifPresent(v -> sb.append(FeatureConfig.DECAY).append(": ").append(v).append("\n"));
    _weight.ifPresent(v -> sb.append(FeatureConfig.WEIGHT).append(": ").append(v).append("\n"));
    _embeddingSize.ifPresent(v -> sb.append(FeatureConfig.EMBEDDING_SIZE).append(": ").append(v).append("\n"));

    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  public String getColumnExpr() {
    return _typedColumnExpr.getExpr();
  }

  public TimeWindowAggregationType getAggregation() {
    return _aggregation; }

  public Duration getWindow() {
    return _windowParameters.getSize();
  }

  public WindowParametersConfig getWindowParameters() {
    return _windowParameters; }

  public Optional<String> getFilter() {
    return _typedFilter.map(TypedExpr::getExpr);
  }

  public Optional<String> getGroupBy() {
    return _groupBy;
  }

  public Optional<Integer> getLimit() {
    return _limit;
  }

  public Optional<String> getDecay() {
    return _decay;
  }

  public Optional<String> getWeight() {
    return _weight;
  }

  public ExprType getColumnExprType() {
    return _typedColumnExpr.getExprType();
  }

  public Optional<ExprType> getFilterExprType() {
    return _typedFilter.map(TypedExpr::getExprType);
  }

  public TypedExpr getTypedColumnExpr() {
    return _typedColumnExpr;
  }

  public Optional<TypedExpr> getTypedFilter() {
    return _typedFilter;
  }

  public Optional<Integer> getEmbeddingSize() {
    return _embeddingSize;
  }

  @Override
  public Optional<String> getDefaultValue() {
    return _defaultValue;
  }

  @Override
  public Optional<FeatureTypeConfig> getFeatureTypeConfig() {
    return _featureTypeConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimeWindowFeatureConfig that = (TimeWindowFeatureConfig) o;
    return Objects.equals(_typedColumnExpr, that._typedColumnExpr) && _aggregation == that._aggregation
        && Objects.equals(_windowParameters, that._windowParameters) && Objects.equals(_typedFilter, that._typedFilter)
        && Objects.equals(_groupBy, that._groupBy) && Objects.equals(_limit, that._limit) && Objects.equals(_decay,
        that._decay) && Objects.equals(_weight, that._weight) && Objects.equals(_embeddingSize, that._embeddingSize)
        && Objects.equals(_featureTypeConfig, that._featureTypeConfig) && Objects.equals(_defaultValue, that._defaultValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_typedColumnExpr, _aggregation, _windowParameters, _typedFilter, _groupBy, _limit, _decay,
        _weight, _embeddingSize, _featureTypeConfig, _defaultValue);
  }
}
