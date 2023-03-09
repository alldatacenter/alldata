package com.linkedin.feathr.sparkcommon

import com.linkedin.feathr.common.{AnchorExtractorBase, SELECTED_FEATURES}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Column, DataFrame}

import scala.collection.JavaConverters._

/**
  * Spark DataFrame-based simple anchor extractor.
  *
  * We strongly recommend developer extends this [[SimpleAnchorExtractorSpark]] trait (when SQL based syntax is not able
  * to express the transformation logic) to implement customized transformation logic, instead of extends [[GenericAnchorExtractorSpark]],
  * As this trait is more efficient than GenericAnchorExtractorSpark in Feathr. (less expensive join between observation
  * and transformed feature data), as [[SimpleAnchorExtractorSpark]] guarantees that all rows/columns in the
  * input DataFrame will be "passed through" into the returned DataFrame which helps to avoid the number of join in Feathr,
  * thanks to the fact that Feathr uses the output dataframe of SimpleAnchorExtractorSpark to apply other feature anchors
  * (as long as the anchors share same source and keyExtractor) to improve performance significantly.
  *
  * see [[SQLConfigurableAnchorExtractor]], [[TestxFeatureDataExtractorV2]], [[SimpleConfigurableAnchorExtractor]]
  * in feathr repo for example
  */

abstract class SimpleAnchorExtractorSpark extends AnchorExtractorBase[Any] {

  /**
    *
    * Transform input dataframe to generate feature columns
    * The column names for the features should be the same as the declared feature names,
    * which are the feature names returned by getProvidedFeatureNames().
    *
    * @param dataFrameWithKeyColumns  input dataframe with join key columns appended
    * @return seq[(prefixed feature column name, feature column)], note that feature columns returned here will be appended to the input
    *         dataframe, so the columns returned here must be derived directly from the input dataFrameWithKeyColumns,
    *         e.g, you cannot do:
    *         val df = dataFrameWithKeyColumns.withColumn("x", ..)
    *         and return Seq(("x", df(x)))
    *         because the column df(x) is from df and not appendable to the input dataFrameWithKeyColumns anymore,
    *         instead, you may do something similar to:
    *         val myUDF = udf(...)
    *         return Seq(("x", myUDF(dataFrameWithKeyColumns("inputCol"))))
    *
    * This limitation is by design, so that Feathr can append the returned columns to the input dataframe and pass it to other
    * anchors if possible, this will reduce the numbers of join to the observation data and improve performance significantly.
    *
    * SimpleAnchorExtractorSpark should be able to handle most of the use cases, if not, see if you can move some of the
    * transformation logic to the corresponding SourceKeyExtractor and fit the rest transformation logic within this API
    *
  */
  def transformAsColumns(dataFrameWithKeyColumns: DataFrame): Seq[(String, Column)] = {
    throw new UnsupportedOperationException("This method is not implemented.")
  }

  /** Transform input dataframe to generate feature columns
    * The column names for the features should be the same as the declared feature names,
    * which are the feature names returned by getProvidedFeatureNames().
    *
    * @param dataFrameWithKeyColumns  input dataframe with join key columns appended
    * @param expectedColSchemas    map from column name to the expected structTypes. This is a contract feathr is setting, so that the user
    *                               gives back only the expected columns. We do a sanity check to match these column types. We expect the user
    *                               to also do the same.
    * @return seq[(prefixed feature column name, feature column)], note that feature columns returned here will be appended to the input
    *         dataframe, so the columns returned here must be derived directly from the input dataFrameWithKeyColumns,
    *         e.g, you cannot do:
    *         val df = dataFrameWithKeyColumns.withColumn("x", ..)
    *         and return Seq(("x", df(x)))
    *         because the column df(x) is from df and not appendable to the input dataFrameWithKeyColumns anymore,
    *         instead, you may do something similar to:
    *         val myUDF = udf(...)
    *         return Seq(("x", myUDF(dataFrameWithKeyColumns("inputCol"))))
    *
    * This limitation is by design, so that Feathr can append the returned columns to the input dataframe and pass it to other
    * anchors if possible, this will reduce the numbers of join to the observation data and improve performance significantly.
    *
    * SimpleAnchorExtractorSpark should be able to handle most of the use cases, if not, see if you can move some of the
    *  transformation logic to the corresponding SourceKeyExtractor and fit the rest transformation logic within this API
    *
    */
  def getFeatures(dataFrameWithKeyColumns: DataFrame, expectedColSchemas: Map[String, StructType]): Seq[(String, Column)] = Seq.empty[(String, Column)]

  /**
    * apply aggregations, e.g. via sql window function
    * This function will be called after transformAsColumns. If you don't need aggregation, leave this function as is.
    * @param groupedDataFrame the source dataframe after 3 processing steps:
    *                         1. [[SourceKeyExtractor.appendKeyColumns()]] by key extractor
    *                         2. [[transformAsColumns]]
    *                         3. groupby by feature join keys, i.e., [[SourceKeyExtractor.getKeyColumnNames()]]
    * @return aggregated feature name and aggregation column express
    */
  def aggregateAsColumns(groupedDataFrame: DataFrame): Seq[(String, Column)] = Seq.empty[(String, Column)]

  /**
    * used to apply various processing after aggregation, e.g., topK
    * This function will be called after transformAsColumns and aggregateAsColumns.
    * If you don't need postProcessing, leave this function as is.
    * @return final output feature name and column expression
    */
  def postProcessing(aggregatedDataFrame: DataFrame): Seq[(String, Column)] = Seq.empty[(String, Column)]

  /**
    * Check the validity of the input DataFrame, raise an exception if the schema is invalid,
    * e.g, does not contain required input columns or has incorrect column types
    * It is the developer's responsibility to validate the input schema's validity
    * @param schema the schema of input dataframe (i.e dataFrameWithKeyColumns in transformAsColumns)
    */
  def validateInputSchema(schema: StructType): Unit = {}

  /**
    * get the feature list that are currently selected to evaluate, e.g., defined in the feature join/generation config
    * Do not use this function in constructor or store as member variable, as it relies on internalParams which
    * may not be initialized in constructor, ONLY use it in transformAsColumns, aggregateAsColumns and postProcessing
    * @return selected features of current extractor
    */
  final def getSelectedFeatures(): Set[String] =
    _internalParams.map(_.getStringList(SELECTED_FEATURES).asScala.toSeq)
      .getOrElse(getProvidedFeatureNames)
      .toSet

  /**
    * set the internal parameters
    *
    * @param paramPath path of the param to set
    * @param paramsValueHoconStr value of the param to set
    */
  private[feathr] def setInternalParams(paramPath: String, paramsValueHoconStr: String) = {
    val extraParams = ConfigFactory.parseString(s"${paramPath}:${paramsValueHoconStr}")
    val newParams = _internalParams match {
      case Some(params) => extraParams.withFallback(params)
      case _ => extraParams
    }
    _internalParams = Some(newParams)
  }

  // only used by Feathr internally, user should not touch this
  private[feathr] var _internalParams: Option[Config] = None
}