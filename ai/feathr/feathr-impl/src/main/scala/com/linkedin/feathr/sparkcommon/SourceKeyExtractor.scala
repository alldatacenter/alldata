package com.linkedin.feathr.sparkcommon

import com.typesafe.config.{Config, ConfigRenderOptions}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

/**
  *
  *
  *
  * This is the base class for user customized key extractor for a source (feature dataset, rather than observation dataset)
  * in an anchor, it is used in the 'keyExtractor' of an anchor,
  * 'keyExtractor' is a replacement for 'key' section in the anchor.
  * Note that, though this is called 'keyExtractor', it is actually a general purpose interface used to
  * generate the fact table that will be passed to anchorExtractors later. Arbitrary transformations are
  * allowed in appendKeyColumns(), only requirement is that it generates the key columns in the output dataframe
  *
  * For example,
  * swaAnchorWithKeyExtractor: {
  *   source: "swaSource"
  *   keyExtractor: "com.linkedin.feathr.offline.SimpleSampleKeyExtractor"
  *   features: {
  *     f: {
  *       def: "aggregationWindow"
  *       aggregation: SUM
  *       window: 3d
  *     }
  *   }
  * }
  *
  * We provide a few default source key extractors, such as {@link MVELSourceKeyExtractor}, {@link SQLSourceKeyExtractor},
  * {@link SpecificRecordSourceKeyExtractor} in repo 'feathr'. So internally, Feathr will create one of these source key extractors,
  * if 'key' section is being used instead of 'keyExtractor' in the anchor.
  *
  * There're also some simple examples such as {@link SimpleSampleKeyExtractor}, {@link SimpleSampleKeyExtractor2} in repo 'feathr'
  *
  */
abstract class SourceKeyExtractor() extends Serializable {
  var params: Option[Config] = None

  /**
    * init with parameter map
    * @param _params
    */
  def init(_params: Config) = {
    params = Some(_params)
  }
  /**
    * Get key column names in the source dataframe, the key columns will be used as join keys to join with observation data.
    * Note: Some extractors such as MVELSourceKeyExtractor and SpecificRecordSourceKeyExtractor need a sample row of the dataset
    * to determine number of key columns, for most of user customized source key extractor, the input datum may be ignored.
    * @param datum a sample row of the dataset, could be None if the dataset is empty
    * @return the key column names of the source in the current anchor
    */
  def getKeyColumnNames(datum: Option[Any] = None): Seq[String]

  /**
    * return list of alias for each key column name
    * an alias should contain only alphaNumeric characters
    * @param datum a sample row of the dataset, could be None if the dataset is empty
    * @return
    */
  def getKeyColumnAlias(datum: Option[Any] = None): Seq[String] = getKeyColumnNames(datum).map(_.replaceAll("[^\\w]",""))

  /**
    * Append the key columns specified in [[getKeyColumnNames]] to the input dataframe
    * This function will be called on the feature source dataframe before join with observation, to prepare the
    * 'dataFrameWithKeyColumns' dataframe which is the input parameters of [[GenericAnchorExtractorSpark]] and
    * [[SimpleAnchorExtractorSpark]]
    * You may do any necessary transformations (e.g, explode/filter/group the dataframe, even run a spark job)
    * in this function to prepare the input dataframe (or fact table) for your anchor extractor.
    * @param dataFrame source dataframe to work with
    * @return input dataframe with key columns appended
    */
  def appendKeyColumns(dataFrame: DataFrame): DataFrame

  /**
    * Check the validity of the input DataFrame, raise an exception if the schema is invalid,
    * e.g, does not contain required input columns or has incorrect column types
    * It is the developer's responsibility to validate the input schema's validity
    * @param schema
    */
  def validateInputSchema(schema: StructType): Unit = {}

  // override toString function so that anchors with same source and SourceKeyExtractor can be grouped and evaluated on the same
  // dataframe in sequential order,this helps to reduce the number of joins to the observation data. The default
  // toString does not work, because toString of each object have different values.

  // Since we already provide this default implementation here, Feathr is now able to group all instances of a single
  // extractor class. User usually does not need to override this function in their customized source key extractor.

  // However, in some cases, user might want to override this, e.g, if their extractor accepts parameters/key expression,
  // and they want to Feathr to group them together for better performance as long as they have same parameters.
  // See MVELSourceKeyExtractor, and SQLSourceKeyExtractor in feathr repo for more details.
  override def toString: String = getClass.getCanonicalName + " withParams:" + params.map(_.root().render(ConfigRenderOptions.concise()).mkString(",")
  )

}
