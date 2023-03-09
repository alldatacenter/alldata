package com.linkedin.feathr.offline.derived.strategies

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureTransformationException}
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.derived.functions.SQLFeatureDerivationFunction
import com.linkedin.feathr.offline.job.FeatureTransformation.FEATURE_NAME_PREFIX
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import org.apache.spark.sql.functions.expr
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters._

/**
 * This class executes SQL-expression based derived feature.
 */
class SqlDerivationSpark extends SqlDerivationSparkStrategy {


  /**
   * Rewrite sqlExpression for a derived feature, e.g, replace the feature name/argument name with Frame internal dataframe column name
   * @param deriveFeature derived feature definition
   * @param keyTag list of tags represented by integer
   * @param keyTagId2StringMap Map from the tag integer id to the string tag
   * @param asIsFeatureNames features names that does not to be rewritten, i.e. passthrough features, as they do not have key tags
   * @return Rewritten SQL expression
   */
  private[offline] def rewriteDerivedFeatureExpression(
                                                        deriveFeature: DerivedFeature,
                                                        keyTag:  Seq[Int],
                                                        keyTagId2StringMap: Seq[String],
                                                        asIsFeatureNames: Set[String]): String = {
    if (!deriveFeature.derivation.isInstanceOf[SQLFeatureDerivationFunction]) {
      throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_ERROR, "Should not rewrite derived feature expression for non-SQLDerivedFeatures")
    }
    val sqlDerivation = deriveFeature.derivation.asInstanceOf[SQLFeatureDerivationFunction]
    val deriveExpr = sqlDerivation.getExpression()
    val parameterNames: Seq[String] = sqlDerivation.getParameterNames().getOrElse(Seq[String]())
    val consumedFeatureNames = deriveFeature.consumedFeatureNames.zipWithIndex.map {
      case (consumeFeatureName, index) =>
        // begin of string, or other char except number and alphabet
        // val featureStartPattern = """(^|[^a-zA-Z0-9])"""
        // end of string, or other char except number and alphabet
        // val featureEndPattern = """($|[^a-zA-Z0-9])"""
        val namePattern = if (parameterNames.isEmpty) consumeFeatureName.getFeatureName else parameterNames(index)
        // getBinding.map(keyTag.get) resolves the call tags
        val newName =
          if (!asIsFeatureNames.contains(FEATURE_NAME_PREFIX + consumeFeatureName.getFeatureName)
            // Feature generation code path does not create columns with tags.
            // The check ensures we do not run into IndexOutOfBoundsException when keyTag & keyTagId2StringMap are empty.
            && keyTag.nonEmpty
            && keyTagId2StringMap.nonEmpty) {
            DataFrameColName.genFeatureColumnName(
              consumeFeatureName.getFeatureName,
              Some(consumeFeatureName.getBinding.asScala.map(keyTag(_)).map(keyTagId2StringMap)))
          } else {
            DataFrameColName.genFeatureColumnName(consumeFeatureName.getFeatureName)
          }
        (namePattern, newName)
    }.toMap

    // replace all feature name to column names
    // featureName is consist of numAlphabetic
    val ss: SparkSession = SparkSession.builder().getOrCreate()
    val dependencyFeatures = ss.sessionState.sqlParser.parseExpression(deriveExpr).references.map(_.name).toSeq
    // \w is [a-zA-Z0-9_], not inclusion of _ and exclusion of -, as - is ambiguous, e.g, a-b could be a feature name or feature a minus feature b
    val rewrittenExpr = dependencyFeatures.foldLeft(deriveExpr)((acc, ca) => {
      // in scala \W does not work as ^\w
      // "a+B+1".replaceAll("([^\w])B([^\w])", "$1abc$2"     =  A+abc+1
      // "a+B".replaceAll("([^\w])B$", "$1abc"         = a+abc
      // "B+1".replaceAll("^B([^\w])", "abc$1"   = abc+1
      // "B".replaceAll("^B$", "abc"  = abc
      val newVal = consumedFeatureNames.getOrElse(ca, ca)
      val patterns = Seq("([^\\w])" + ca + "([^\\w])", "([^\\w])" + ca + "$", "^" + ca + "([^\\w])", "^" + ca + "$")
      val replacements = Seq("$1" + newVal + "$2", "$1" + newVal, newVal + "$1", newVal)
      val replacedExpr = patterns
        .zip(replacements)
        .toMap
        .foldLeft(acc)((orig, pairs) => {
          orig.replaceAll(pairs._1, pairs._2)
        })
      replacedExpr
    })
    rewrittenExpr
  }

  /**
   * Apply the derivation strategy.
   *
   * @param keyTags            keyTags for the derived feature.
   * @param keyTagList         integer keyTag to string keyTag map.
   * @param df                 input DataFrame.
   * @param derivedFeature     Derived feature metadata.
   * @param derivationFunction Derivation function to evaluate the derived feature
   * @return output DataFrame with derived feature.
   */
  override def apply(keyTags: Seq[Int],
                      keyTagList: Seq[String],
                      df: DataFrame,
                      derivedFeature: DerivedFeature,
                      derivationFunction: SQLFeatureDerivationFunction,
                      mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame = {
    // sql expression based derived feature needs rewrite, e.g, replace the feature names with feature column names in the dataframe
    // Passthrough fields do not need rewrite as they do not have tags.
    val passthroughFieldNames = df.schema.fields.map(f =>
      if (f.name.startsWith(FEATURE_NAME_PREFIX)) {
        f.name
      } else {
        FEATURE_NAME_PREFIX + f.name
      }
    ).toSet
    val rewrittenExpr = rewriteDerivedFeatureExpression(derivedFeature, keyTags, keyTagList, passthroughFieldNames)
    val tags = Some(keyTags.map(keyTagList).toList)
    val featureColumnName = DataFrameColName.genFeatureColumnName(derivedFeature.producedFeatureNames.head, tags)
    df.withColumn(featureColumnName, expr(rewrittenExpr))
  }

}
