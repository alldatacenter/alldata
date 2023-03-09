package com.linkedin.feathr.offline.derived.strategies

import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureDerivationFunctionBase}
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.derived.functions.{SQLFeatureDerivationFunction, SeqJoinDerivationFunction}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.sparkcommon.FeatureDerivationFunctionSpark
import org.apache.spark.sql.DataFrame

/**
 * Feathr supports different types of derivation functions - SqlDerivationFunction, MvelDerivationFunction, SeqJoinDerivationFunction and so on.
 * The execution of each of these functions is different. Hence the derived feature evaluations adopt a "strategy" design pattern.
 * A derivation strategy encapsulates the execution of derivations.
 */
private[offline] trait DerivationStrategy[T <: FeatureDerivationFunctionBase] {
  /**
   * Apply the derivation strategy.
   * @param keyTags               keyTags for the derived feature.
   * @param keyTagList            integer keyTag to string keyTag map.
   * @param df                    input DataFrame.
   * @param derivedFeature        Derived feature metadata.
   * @param derivationFunction    Derivation function to evaluate the derived feature
   * @return output DataFrame with derived feature.
   */
  def apply(keyTags: Seq[Int], keyTagList: Seq[String], df: DataFrame, derivedFeature: DerivedFeature, derivationFunction: T, mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame
}

/**
 * Implementation should define how a derivation logic defined in the implementation of FeatureDerivationFunctionSpark is applied on a feature.
 */
private[offline] trait SparkUdfDerivationStrategy extends DerivationStrategy[FeatureDerivationFunctionSpark]

/**
 * Implementation should define how a derivation logic defined in the implementation of FeatureDerivationFunction is applied on a feature.
 * This strategy covers MvelFeatureDerivationFunction as well as Row-based custom derivation UDF.
 */
private[offline] trait RowBasedDerivationStrategy extends DerivationStrategy[FeatureDerivationFunction]

/**
 * Implementation should define how a sequential join feature is evaluated as a derivation.
 */
private[offline] trait SequentialJoinDerivationStrategy extends DerivationStrategy[SeqJoinDerivationFunction]

/**
 * Implementation should define how a SQL-expression based derivation is evaluated.
 */
private[offline] trait SqlDerivationSparkStrategy extends DerivationStrategy[SQLFeatureDerivationFunction]

/**
 * This case class holds the implementations of supported strategies.
 */
private[offline] case class DerivationStrategies(
    customDerivationSparkStrategy: SparkUdfDerivationStrategy,
    rowBasedDerivationStrategy: RowBasedDerivationStrategy,
    sequentialJoinDerivationStrategy: SequentialJoinDerivationStrategy,
    sqlDerivationSparkStrategy: SqlDerivationSparkStrategy) {
}
