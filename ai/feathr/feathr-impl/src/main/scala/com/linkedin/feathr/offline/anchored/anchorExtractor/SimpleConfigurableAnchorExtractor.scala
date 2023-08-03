package com.linkedin.feathr.offline.anchored.anchorExtractor

import com.fasterxml.jackson.annotation.JsonProperty
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.util.CoercionUtils
import com.linkedin.feathr.common.{AnchorExtractor, FeatureTypeConfig, FeatureTypes, FeatureValue, SparkRowExtractor}
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.config.MVELFeatureDefinition
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.mvel.{MvelContext, MvelUtils}
import com.linkedin.feathr.offline.util.FeatureValueTypeValidator
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.types._
import org.mvel2.MVEL

import scala.collection.JavaConverters._

/**
 * A general-purpose configurable FeatureAnchor that extract features based on its definitions,
 * the definition includes mvel expression and feature type (optional)
 *
 * @param key An MVEL expression that provides the entityId for a given data record. This could be as simple as the name
 *            of the field containing this data, e.g. "a_id". The expression may return NULL if a record should be
 *            filtered out
 * @param features A map of feature name to feature definition that provides the feature for a given data record
 */
private[offline] class SimpleConfigurableAnchorExtractor( @JsonProperty("key") key: Seq[String],
                                                          @JsonProperty("features") features: Map[String, MVELFeatureDefinition])
  extends AnchorExtractor[Any] with SparkRowExtractor {

  var mvelContext: Option[FeathrExpressionExecutionContext] = None
  @transient private lazy val log = LogManager.getLogger(getClass)

  def getKeyExpression(): Seq[String] = key

  def getFeaturesDefinitions(): Map[String, MVELFeatureDefinition] = features

  override def getProvidedFeatureNames: Seq[String] = features.keys.toIndexedSeq

  @transient private lazy val parserContext = MvelContext.newParserContext

  private val keyExpression = if (key == null) Seq() else key.map(k => MVEL.compileExpression(k, parserContext))

  /*
   * Create a map of FeatureRef string to (MVEL expression, optional FeatureType) tuple.
   * FeatureRef string is a string representation of FeatureRef and is backward compatible
   * with legacy feature names.
   */
  private val featureExpressions = features map {
    case (featureRefStr, featureDef) =>
      if (featureDef.featureExpr.isEmpty) {
        throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"featureExpr field in feature $featureDef is not defined.")
      }
      (featureRefStr, (MVEL.compileExpression(featureDef.featureExpr, parserContext), featureDef.featureTypeConfig.map(x => x.getFeatureType)))
  }

  private val featureTypeConfigs = features map {
    case (featureRefStr, featureDef) =>
      (featureRefStr, featureDef.featureTypeConfig.getOrElse(new FeatureTypeConfig(FeatureTypes.UNSPECIFIED)))
  }


  /**
   * Get key from input row
   * @param datum input row
   * @return list of feature keys
   */
  override def getKeyFromRow(datum: Any): Seq[String] = {
    getKey(datum.asInstanceOf[Any])
  }

  override def getKey(datum: Any): Seq[String] = {
    MvelContext.ensureInitialized()
    // be more strict for resolving keys (don't swallow exceptions)
    keyExpression.map(k =>
      try {
        Option(MvelContext.executeExpressionWithPluginSupport(k, datum, mvelContext.orNull)) match {
          case None => null
          case Some(keys) => keys.toString
        }
      } catch {
        case e: Exception =>
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"Could not evaluate key expression $k on feature data. Please fix your key expression.",
            e)
    })
  }

  // only evaluate required feature, aims to improve performance
  override def getSelectedFeatures(datum: Any, selectedFeatures: Set[String]): Map[String, FeatureValue] = {
    MvelContext.ensureInitialized()

    featureExpressions collect {
      case (featureRefStr, (expression, featureType)) if selectedFeatures.contains(featureRefStr) =>
        (featureRefStr, (MvelUtils.executeExpression(expression, datum, null, featureRefStr, mvelContext), featureType))
    } collect {
      // Apply a partial function only for non-empty feature values, empty feature values will be set to default later
      case (featureRefStr, (Some(value), fType)) =>
        getFeature(featureRefStr, value, fType.getOrElse(FeatureTypes.UNSPECIFIED))
    }
  }

  /**
   * Extract feature from row
   * @param row input row
   *  @return A map of feature name to feature value
   */
  override def getFeaturesFromRow(row: Any) = {
    getFeatures(row.asInstanceOf[Any])
  }

  override def getFeatures(datum: Any): Map[String, FeatureValue] = {
    evaluateMVELWithFeatureType(datum, getProvidedFeatureNames.toSet) collect {
      // Apply a partial function only for non-empty feature values, empty feature values will be set to default later
      case (featureRefStr, (value, fType)) =>
        getFeature(featureRefStr, value, fType)
    }
  }

  /**
   * Ideally we could use reflection to inspect the type argument T of the implementing class.
   * But I don't know how to do this right now using the reflection API, and the cost of making people implement this is low.
   */
  override def getInputType: Class[_] = classOf[Object]

  /**
   * get feature type for selected features
   * @param selectedFeatureRefs selected features
   * @return featureRef to feature type
   */
  def getFeatureTypes(selectedFeatureRefs: Set[String]): Map[String, FeatureTypes] = {
    featureExpressions.collect {
      case (featureRef, (_, featureTypeOpt)) if selectedFeatureRefs.contains(featureRef) =>
        featureRef -> featureTypeOpt.getOrElse(FeatureTypes.UNSPECIFIED)
    }
  }

  /**
   * Helper function to convert a feature data into FeatureValue.
   */
  private def getFeature(featureRefStr: String, value: Any, featureType: FeatureTypes=FeatureTypes.UNSPECIFIED): (String, FeatureValue) = {
    val featureTypeConfig = if (featureTypeConfigs(featureRefStr).getFeatureType == FeatureTypes.UNSPECIFIED) {
      new FeatureTypeConfig(featureType)
    } else {
      featureTypeConfigs(featureRefStr)
    }
    val featureValue = offline.FeatureValue.fromTypeConfig(value, featureTypeConfig)
    FeatureValueTypeValidator.validate(featureRefStr, featureValue, featureTypeConfigs(featureRefStr) )
    (featureRefStr, featureValue)
  }

  /**
   * Evaluate mvel expression
   * @param datum record to evaluate against
   * @param selectedFeatureRefs selected features to evaluate
   * @return map featureRefStr -> feature value
   */
  private def evaluateMVEL(datum: Any, selectedFeatureRefs: Set[String]): Map[String, Option[AnyRef]] = {
    MvelContext.ensureInitialized()
    featureExpressions collect {
      case (featureRefStr, (expression, _)) if selectedFeatureRefs.contains(featureRefStr) =>
        /*
         * Note that FeatureTypes is ignored above. A feature's value type and dimension type(s) coupled with the
         * heuristics implemented in the TensorBuilder class obviate the need to consider (Feathr) FeatureTypes object
         * for building a tensor. Feature's value type and dimension type(s) are obtained via Feathr's Feature Metadata
         * Library during tensor construction.
         */
        (featureRefStr, MvelUtils.executeExpression(expression, datum, null, featureRefStr, mvelContext))
    }
  }

  /**
   * Evaluate mvel expression, return the feature value and inferred feature types
   * The type is inferred for each row, this is the same as the existing behavior,
   * i.e. coerce each MVEL result into a feature value by looking at the MVEL result
   * if type is not specified
   * @param datum record to evaluate against
   * @return map of (featureRefStr -> (feature value, featureType))
   */
  def evaluateMVELWithFeatureType(datum: Any, selectedFeatureRefs: Set[String]): Map[String, (AnyRef, FeatureTypes)] = {
    val featureTypeMap = getFeatureTypes(selectedFeatureRefs)
    val result = evaluateMVEL(datum, selectedFeatureRefs)
    val resultWithType = result collect {
      // Apply a partial function only for non-empty feature values, empty feature values will be set to default later
      case (featureRefStr, Some(value)) =>
        val javaValue = if (value.isInstanceOf[scala.collection.Map[_, _]]) {
          value.asInstanceOf[scala.collection.Map[_, _]].asJava
        } else {
          value
        }
        // If user already provided the feature type, we don't need to coerce/infer it
        val coercedFeatureType = if (featureTypeMap(featureRefStr) == FeatureTypes.UNSPECIFIED) {
          CoercionUtils.getCoercedFeatureType(javaValue)
        } else featureTypeMap(featureRefStr)
        (featureRefStr, (javaValue, coercedFeatureType))
    }
    resultWithType map {
      case (featureRef, (featureValue, coercedFeatureType)) =>
        val userProvidedFeatureType = featureTypeMap.getOrElse(featureRef, FeatureTypes.UNSPECIFIED)
        val featureType = if (userProvidedFeatureType == FeatureTypes.UNSPECIFIED) coercedFeatureType else userProvidedFeatureType
        featureRef -> (featureValue, featureType)
    }
  }
}
