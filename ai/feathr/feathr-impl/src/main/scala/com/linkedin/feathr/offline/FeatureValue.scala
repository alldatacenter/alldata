package com.linkedin.feathr.offline

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes}

import java.lang.{Float => JFloat}
import java.util.{HashMap => JHashMap}
import scala.collection.JavaConverters._

/**
 * A companion object for the FeatureValue class.
 * It provides the interface for scala specific usage, such as pattern matching, etc.
 */
private[offline] object FeatureValue {
  def apply(vector: java.util.Map[String, JFloat]): common.FeatureValue =
    common.FeatureValue.createStringTermVector(vector)

  def apply[T: Numeric](vector: Map[String, T]): common.FeatureValue = {
    // scala.collection.convert.wrappers couldn't be used here due to an unserializable error
    val result = new JHashMap[String, JFloat]
    vector.foreach(elem => result.put(elem._1, implicitly[Numeric[T]].toFloat(elem._2).asInstanceOf[JFloat]))
    FeatureValue(result)
  }


  /**
   * Convert feature data into FeatureValue based on the types provided.
   */
  def fromTypeConfig(value: Any, featureTypeConfig: FeatureTypeConfig): common.FeatureValue = {
    // If the return result is already FeatureValue, we should just return as-is. If not, we should convert the raw
    // value into corresponding FeatureValue.
    value match {
      case featureValue: com.linkedin.feathr.common.FeatureValue =>
        featureValue
      case _ =>
        if (featureTypeConfig.getFeatureType == FeatureTypes.TENSOR) {
          // When the type is tensor, always honor the user provided tensor config in FeathrConfig
          if (featureTypeConfig.getTensorType != null) {
            common.FeatureValue.createTensor(value, featureTypeConfig.getTensorType)
          } else {
            new common.FeatureValue(value)
          }
        } else {
          new common.FeatureValue(value, featureTypeConfig.getFeatureType)
        }
    }
  }

  def apply(value : Any, featureType : FeatureTypes, featureName : String): common.FeatureValue = {
      new common.FeatureValue(value, featureType)
  }

  def unapply(arg: common.FeatureValue): Option[Map[String, Float]] =
    Some(arg.getAsTermVector.asScala.toMap.map { case (key, value) => (key, value.floatValue()) })
}
