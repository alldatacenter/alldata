package com.linkedin.feathr.common

/**
  * Provides feature values based on some "raw" data element
 *
 * @tparam T raw data type
  */
trait AnchorExtractor[T] extends AnchorExtractorBase[T] with SparkRowExtractor {

  // NOTE: We may want to decouple this from Extractor. It's more a property of the source, or source-reference
  def getKey(datum: T): Seq[String]

  /**
    * Provides the feature values for a given input data record.
    * Note: Might provide more features than advertised in getProvidedFeatures(); caller should filter out unwanted features if necessary.
    */
  def getFeatures(datum: T): Map[String, FeatureValue]

  /**
    * get partial features instead of all feature in the anchor
    * @param datum
    * @param selectedFeatures
    * @return
    */
  def getSelectedFeatures(datum: T, selectedFeatures: Set[String]): Map[String, FeatureValue] = {
    getFeatures(datum).filter{case (name, _) => selectedFeatures.contains(name)}
  }

  /**
    * Ideally we could use reflection to inspect the type argument T of the implementing class.
    * But I don't know how to do this right now using the reflection API, and the cost of making people implement this is low.
    */
  def getInputType: Class[_]

  /**
   * Get key from input row
   * @param datum input row
   * @return list of feature keys
   */
  def getKeyFromRow(datum: Any): Seq[String] = getKey(datum.asInstanceOf[T])

  /**
   * Get the feature value from the row
   * @param datum input row
   * @return A map of feature name to feature value
   */
  def getFeaturesFromRow(datum: Any): Map[String, FeatureValue] = getFeatures(datum.asInstanceOf[T])

  override def toString: String = getClass.getSimpleName
}
