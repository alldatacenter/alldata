package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes, FeatureValue}
import com.linkedin.feathr.offline.transformation.DefaultFeatureValueToColumnConverterFactory.getConverter
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, expr, udf, when}
import org.apache.spark.sql.types._
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.mutable

/**
 * Process and substitute defaults for specified features.
 * @tparam T type of inputData.
 */
private[offline] trait DefaultValueSubstituter[T] {

  /**
   * Process the inputs and substitute defaults for specified feature columns.
   * Default value calculations should factor in the user specified default value and the feature type (if any).
   *
   * This API takes in two maps - featureName -> default value and featureName -> featureType.
   * It is important to know that a strong consistency b/w the two maps is not expected or enforced i.e.,
   * a featureName available in "parsedDefaults" does not have to be in "featureTypes" Map.
   * If featureName is not found in "featureTypes" map, it is inferred.
   *
   * This requires changes from Config loading and in FeatureAnchor.
   *
   * @param inputData             The context or input containing the feature columns.
   * @param featureNames          Features that should be processed and substituted with defaults.
   * @param parsedDefaults        A map of feature name to default value constructed when parsing feature definitions.
   * @param featureTypes          A map of feature name to inferred / specified feature type.
   * @param featureFieldResolver  A function that takes in the feature name and resolves the feature column name in the data set.
   * @return default substituted inputData.
   */
  def substituteDefaults(
      inputData: T,
      featureNames: Seq[String],
      parsedDefaults: Map[String, FeatureValue],
      featureTypes: Map[String, FeatureTypeConfig],
      ss: SparkSession,
      featureFieldResolver: (String) => String = Predef.identity[String]): T
}

/**
 * Defaults processor for input DataFrame where the defaults are parsed as FeatureValue.
 */
private[offline] trait DataFrameDefaultValueSubstituter extends DefaultValueSubstituter[DataFrame]

/**
 * A concrete singleton implementation of processing default values parsed as FeatureValue and substituted in inputData represented as DataFrame.
 */
private[offline] object DataFrameDefaultValueSubstituter extends DataFrameDefaultValueSubstituter {

  /**
   * Process the inputs and substitute defaults for specified feature columns.
   * Default value calculations should factor in the user specified default value and the feature type (if any).
   * @param inputDF               The context or input containing the feature columns.
   * @param featureNames          Features that should be processed and substituted with defaults.
   * @param parsedDefaults        A map of feature name to default value constructed when parsing feature definitions.
   * @param featureTypes          A map of feature name to inferred / specified feature type.
   * @param featureFieldResolver  A function that takes in the feature name and resolves the feature column name in the data set.
   * @return default substituted inputData.
   */
  override def substituteDefaults(
      inputDF: DataFrame,
      featureNames: Seq[String],
      parsedDefaults: Map[String, FeatureValue],
      featureTypes: Map[String, FeatureTypeConfig],
      ss: SparkSession,
      featureFieldResolver: String => String): DataFrame = {
    featureNames.foldLeft(inputDF)((baseDF, featureName) =>
      processDefaultSingleFeature(baseDF, featureName, parsedDefaults, featureTypes, ss, featureFieldResolver,
        getConverter(featureName)))
  }

  /**
   * Substitute default for a single feature.
   * @param inputDF                  The context or input containing the feature columns.
   * @param featureName              Feature for which default value is substituted.
   * @param parsedDefaults           A map of feature name to default value constructed when parsing feature definitions.
   * @param featureTypes             A map of feature name to inferred / specified feature type.
   * @param featureFieldResolver     A function that takes in the feature name and resolves the feature column name in the data set.
   * @param valueToColumnConverter   Converter to convert the default value to Spark SQL Column.
   * @return default substituted input DataFrame.
   */
  private def processDefaultSingleFeature(
      inputDF: DataFrame,
      featureName: String,
      parsedDefaults: Map[String, FeatureValue],
      featureTypes: Map[String, FeatureTypeConfig],
      ss: SparkSession,
      featureFieldResolver: String => String,
      valueToColumnConverter: FeatureValueToColumnConverter): DataFrame = {
    // process each feature
    val rawMaybeDefaultVal = parsedDefaults.get(featureName)
    if (rawMaybeDefaultVal.isEmpty) { // if no defaults is specified by the user, then nothing to be done
      inputDF
    } else {
      val defaultFeatureValue = rawMaybeDefaultVal.get
      val featureColumnName = featureFieldResolver(featureName) // Resolve feature column name in DataFrame using featureName.
      val fieldIndex = inputDF.schema.fieldIndex(featureColumnName)
      val field = inputDF.schema.toList(fieldIndex)
      val inferredType = FeaturizedDatasetUtils.inferFeatureType(FeatureTypes.UNSPECIFIED, Some(field.dataType))


      val featureTypeConfig = featureTypes.getOrElse(featureName, new FeatureTypeConfig(inferredType))
      val defaultTypedLit = if (featureTypeConfig.getFeatureType == FeatureTypes.TENSOR) {
        // For tensor default, since we don't have type, so we need to use expr to construct the default column
        val schema = field.dataType
        val tensorData = defaultFeatureValue.getAsTensorData
        val ts = FeaturizedDatasetUtils.tensorToFDSDataFrameRow(tensorData, Some(schema))
        val fdsTensorDefaultUDF = getFDSTensorDefaultUDF(schema, ts)
        ss.udf.register("tz_udf", fdsTensorDefaultUDF)
        expr(s"tz_udf($featureColumnName)")
      } else {
        valueToColumnConverter.convert(featureName, rawMaybeDefaultVal.get, field.dataType, featureTypeConfig.getFeatureType)
      }

      // replace nulls with default value, will throw exception if the column type does not match the declared type
      val tempFeatureColumnName = "_temp_column_for_default_value_" + featureColumnName
      inputDF
        .withColumn(tempFeatureColumnName, when(col(featureColumnName).isNull, defaultTypedLit).otherwise(col(featureColumnName)))
        .drop(featureColumnName)
        .withColumnRenamed(tempFeatureColumnName, featureColumnName)
    }
  }

  private def getFDSTensorDefaultUDF(schema: DataType, tensorData: Any): UserDefinedFunction = {
    def getFDSTensorDefaultUDF(inputRow: mutable.WrappedArray[GenericRowWithSchema]): Any = {
      tensorData
    }

    udf((x: mutable.WrappedArray[GenericRowWithSchema]) => getFDSTensorDefaultUDF(x), schema)
  }
}
