package com.linkedin.feathr.compute;

/**
 * In the compute graph, operators are referenced by their names.
 *
 */
public class Operators {
  private Operators() {
  }

  /**
   * Name: anchor mvel
   * Description: MVEL operator for an anchored feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_ANCHOR_MVEL = "feathr:anchor_mvel:0";

  /**
   * Name: derived mvel
   * Description: MVEL operator for an anchored feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_DERIVED_MVEL = "feathr:derived_mvel:0";

  /**
   * Name: passthrough mvel
   * Description: MVEL operator for a passthrough feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_PASSTHROUGH_MVEL = "feathr:passthrough_mvel:0";

  /**
   * Name: lookup mvel
   * Description: MVEL operator for a lookup key
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_LOOKUP_MVEL = "feathr:lookup_mvel:0";

  /**
   * Name: sliding_window_aggregation
   * Description: Configurable sliding window aggregator
   *
   * Input: Series
   * Output: Any
   *
   * Parameters:
   *  - target_column
   *  - aggregation_type
   *  - window_size
   *  - window_unit
   *  - lateral_view_expression_0, lateral_view_expression_1, ...
   *  - lateral_view_table_alias_0, lateral_view_table_alias_1, ...
   *  - filter_expression
   *  - group_by_expression
   *  - max_number_groups
   */
  public static final String OPERATOR_ID_SLIDING_WINDOW_AGGREGATION = "feathr:sliding_window_aggregation:0";

  /**
   * Name: anchor_java_udf_feature_extractor
   * Description: Runs a Java UDF for an anchored feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - class
   *  - userParam_foo, userParam_bar
   */
  public static final String OPERATOR_ID_ANCHOR_JAVA_UDF_FEATURE_EXTRACTOR = "feathr:anchor_java_udf_feature_extractor:0";

  /**
   * Name: passthrough_java_udf_feature_extractor
   * Description: Runs a Java UDF for a passthrough feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - class
   *  - userParam_foo, userParam_bar
   */
  public static final String OPERATOR_ID_PASSTHROUGH_JAVA_UDF_FEATURE_EXTRACTOR = "feathr:passthrough_java_udf_feature_extractor:0";

  /**
   * Name: derived_java_udf_feature_extractor
   * Description: Runs a Java UDF for a derived feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - class
   *  - userParam_foo, userParam_bar
   */
  public static final String OPERATOR_ID_DERIVED_JAVA_UDF_FEATURE_EXTRACTOR = "feathr:derived_java_udf_feature_extractor:0";

  /**
   * Name: anchor_spark_sql_feature_extractor
   * Description: SQL operator for an anchored feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_ANCHOR_SPARK_SQL_FEATURE_EXTRACTOR = "feathr:anchor_spark_sql_feature_extractor:0";

  /**
   * Name: passthrough_spark_sql_feature_extractor
   * Description: SQL operator for a passthrough feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_PASSTHROUGH_SPARK_SQL_FEATURE_EXTRACTOR = "feathr:passthrough_spark_sql_feature_extractor:0";

  /**
   * Name: derived_spark_sql_feature_extractor
   * Description: SQL operator for a derived feature
   *
   * Input: Any
   * Output: Any
   *
   * Parameters:
   *  - expression
   */
  public static final String OPERATOR_ID_DERIVED_SPARK_SQL_FEATURE_EXTRACTOR = "feathr:derived_spark_sql_feature_extractor:0";

  /**
   * Name: extract_from_tuple
   * Description: select i-th item from tuple
   *
   * Input: Tuple
   * Output: Any
   *
   * Parameter:
   *  - index
   */
  public static final String OPERATOR_ID_EXTRACT_FROM_TUPLE = "feathr:extract_from_tuple:0";

  /**
   * Name: feature_alias
   * Description: given a feature, create another feature with the same values but different feature name. Main usage
   * is for intermediate features in sequential join and derived features. Note that no parameters are needed because
   * the input node's output feature will be aliases as this transformation node's feature name.
   *
   * Input: Feature
   * Output: Alias Feature
   *
   * Parameter: None
   */
  public static final String OPERATOR_FEATURE_ALIAS = "feathr:feature_alias:0";
}