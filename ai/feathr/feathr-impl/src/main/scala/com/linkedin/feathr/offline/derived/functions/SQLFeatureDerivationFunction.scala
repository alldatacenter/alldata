package com.linkedin.feathr.offline.derived.functions

import com.linkedin.feathr.common
import com.linkedin.feathr.common.FeatureDerivationFunction
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import org.apache.spark.sql.SparkSession

/**
 * A derivation function representing a derived feature expressed in SparkSQL
 *
 * Note: This class serves as a data container object rather than holding any computation logic.
 * it will be calculated in FeathrClient directly. As a result, the `getFeatures`
 * method is a dummy method and shouldn't be used.
 */
private[offline] class SQLFeatureDerivationFunction(expression: String, parameterNames: Option[Seq[String]] = None) extends FeatureDerivationFunction {
  private val ss: SparkSession = SparkSession.builder().getOrCreate()
  def getExpression(): String = {
    expression
  }
  def getParameterNames(): Option[Seq[String]] = parameterNames

  /**
   * get dependency features from the expression
   * the order of feature is random
   */
  val dependencyFeatureNames: Seq[String] = {
    ss.sessionState.sqlParser.parseExpression(expression).references.map(_.name).toSeq
  }

  /**
   * WARNING: Dummy getFeatures method. This method shouldn't be called as SQLFeatureDerivationFunction is not computed here.
   */
  override def getFeatures(inputs: Seq[Option[common.FeatureValue]]): Seq[Option[common.FeatureValue]] = {
    throw new FeathrException(ErrorLabel.FEATHR_ERROR, "getFeatures() does not apply to SQLFeatureDerivationFunction")
  }
}
