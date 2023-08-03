package com.linkedin.feathr.sparkcommon

import com.linkedin.feathr.common.AnchorExtractorBase
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Column, DataFrame, Dataset}

/**
 * Spark DataFrame-based generic anchor extractor (Warning: performance impact).
 *
 * We strongly recommend developer extends the other trait, [[SimpleAnchorExtractorSpark]]
 * (when SQL based syntax is not able to express the transformation logic) to implement customized transformation logic,
 * instead of extending this [[GenericAnchorExtractorSpark]]. As this trait is LESS efficient than SQL syntax based or the
 * [[SimpleAnchorExtractorSpark]] in feathr.
 *
 * Each use of this GenericAnchorExtractorSpark will trigger an expensive join between the observation and
 * transformed feature data (i.e, the output dataframe of the transform() method).
 *
 * Only extends this trait when if is NOT possible to use [[SimpleAnchorExtractorSpark]] + [[SourceKeyExtractor]],
 * such case should be rare, e.g, even when you need to filter input rows/columns, explode rows, you could apply some
 * of the transformations in the SourceKeyExtractor's appendKeyColumns, and use [[SimpleAnchorExtractorSpark]]
 * to apply the rest of your transformations.
 */

abstract class GenericAnchorExtractorSpark extends AnchorExtractorBase[Any] {
  /**
   *
   * Transform input dataframe to generate feature columns
   * The column names for the features should be the same as the declared feature names,
   * which are the feature names returned by getProvidedFeatureNames().
   *
   *
   * @param dataFrameWithKeyColumns  input dataframe with join key columns appended
   * @return input dataframe with feature columns appended.
   */
  def transform(dataFrameWithKeyColumns: DataFrame): DataFrame

  /**
   * Check the validity of the input DataFrame, raise an exception if the schema is invalid,
   * e.g, does not contain required input columns or has incorrect column types
   * It is the developer's responsibility to validate the input schema's validity
   * @param schema the schema of input dataframe (i.e dataFrameWithKeyColumns in transform)
   */
  def validateInputSchema(schema: StructType): Unit = {}


}
