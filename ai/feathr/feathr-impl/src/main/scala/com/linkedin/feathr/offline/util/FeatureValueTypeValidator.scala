package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.types.FeatureType
import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes, FeatureValue}

/**
 * FeatureValueTypeValidator checks extracted FeatureValue to user-defined feature config.
 */
private[offline] object FeatureValueTypeValidator {
  /**
   * Validate the FeatureTypeConfig produced by the features against the FeatureTypeConfig specified by the Feathr
   * config.
   */
  def validate(features: Map[String, FeatureValue], featureTypeConfigs: Map[String, FeatureTypeConfig]): Unit = {
    features.foreach {
      case (key, value) =>
        featureTypeConfigs.get(key).foreach(
          featureTypeConfig => FeatureValueTypeValidator.validate(key, value, featureTypeConfig))
    }
  }

  /**
   * Convenience method of validate(featureValue : FeatureValue, featureTypeConfig : FeatureTypeConfig).
   * It takes in a Option[FeatureTypeConfig] and does a None check.
   *
   * @param featureValue      value extracted from data
   * @param featureTypeConfig user-defined config, optional
   */
  def validate(featureValue: FeatureValue, featureTypeConfig: Option[FeatureTypeConfig], featureName: String): Unit = {
    featureTypeConfig match {
      case Some(f) => validate(featureName, featureValue, f)
      case None =>
    }
  }

  /**
   * Compares FeatureTypes in extracted FeatureValue against FeatureTypeConfig provided in feathr config.
   * For Tensor type, it also compares ValueType, TensorCategory, and DimensionType.
   *
   * @param featureValue      value extracted from data
   * @param featureTypeConfig user-defined config
   */
  def validate(featureName: String, featureValue: FeatureValue, featureTypeConfig: FeatureTypeConfig): Unit = {
    val configFeatureTypes = featureTypeConfig.getFeatureType
    val valueBasicType = featureValue.getFeatureType.getBasicType
    if (configFeatureTypes != FeatureTypes.UNSPECIFIED) {
      if (valueBasicType != FeatureType.BasicType.TENSOR || configFeatureTypes != FeatureTypes.TENSOR) {
        if (configFeatureTypes != FeatureTypes.valueOf(valueBasicType.name)) {
          throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, "The FeatureValue type of : " + featureName +
            " is " + valueBasicType + ", which is not consistent with the type specified in the Feathr config: ." + configFeatureTypes);
        }
      } else if (featureTypeConfig.getTensorType != null) {
        val configTensorType = featureTypeConfig.getTensorType
        val valueTensorType = featureValue.getAsTypedTensor.getType
        if (configTensorType.getValueType != null && configTensorType.getValueType != valueTensorType.getValueType) {
          throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, "The tensor value type of :" + featureName +
            " is " + valueTensorType + ", which is not consistent with the type specified in the Feathr config: ." + configTensorType);
        }
        if (configTensorType.getTensorCategory != null &&
          configTensorType.getTensorCategory != valueTensorType.getTensorCategory) {
          throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, "The tensor category type of : " + featureName + " is "
            + valueTensorType + ", which is not consistent with the type specified in the Feathr config: ." + configTensorType);
        }
        if (configTensorType.getDimensionTypes != null &&
          configTensorType.getDimensionTypes != valueTensorType.getDimensionTypes) {
          throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, "The tensor dimension type of : "  + featureName + " is "
            + valueTensorType + ", which is not consistent with the type specified in the Feathr config: ." + configTensorType);
        }
      }
    }
  }
}
