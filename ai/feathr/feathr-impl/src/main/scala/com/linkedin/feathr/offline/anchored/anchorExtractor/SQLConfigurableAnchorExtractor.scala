package com.linkedin.feathr.offline.anchored.anchorExtractor

import com.fasterxml.jackson.annotation.JsonProperty
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrException}
import com.linkedin.feathr.common.tensor.LOLTensorData
import com.linkedin.feathr.offline.config.SQLFeatureDefinition
import com.linkedin.feathr.offline.job.FeatureTransformation
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.{FDS_TENSOR, FeatureColumnFormat, RAW}
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import com.linkedin.feathr.sparkcommon.SimpleAnchorExtractorSpark
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}

/**
 * A SQL-based configurable FeatureAnchor that extract features based on its SQL expression definitions
 * Unlike {@link SimpleConfigurableAnchorExtractor }, this anchor extractor extends AnchorExtractorSpark,
 * which provides a dataFrame based batch transformation
 * */
private[offline] case class SQLKeys(@JsonProperty("sqlExpr") val sqlExpr: Seq[String])

private[offline] class SQLConfigurableAnchorExtractor(
   @JsonProperty("key") val key: SQLKeys,
   @JsonProperty("features") val features: Map[String, SQLFeatureDefinition]) extends SimpleAnchorExtractorSpark {
  @transient private lazy val log = LogManager.getLogger(getClass)
  private val featureToDefs = features
  private val columnNameToFeatureDefs = featureToDefs.map(f => (f._1, f._2.featureExpr))
  private val userFacingAvroTensorToFDSTensorUDFName = "avroTensorToFDSTensor"

  def getProvidedFeatureNames: Seq[String] = features.keys.toIndexedSeq

  /**
   * Apply the user defined SQL transformation to the dataframe and produce the (feature name, feature column) pairs,
   * one pair for each provided feature.
   * @param inputDF input dataframe
   * @return
   */
  override def transformAsColumns(inputDF: DataFrame): Seq[(String, Column)] = {
    // apply sql transformation for the features
    val featureNames = featureToDefs.map(_._1).toSet
    columnNameToFeatureDefs
      .map(featureNameAndDef => {
        // if there's another feature that is conflicting,
        // e.g, the two features will result in exception, because Bar definition 'Foo' is ambiguous, could be feature Foo
        // or a field named 'Foo' in the dataframe
        // features {
        //  Foo.sqlExpr: Foo
        //  Bar.sqlExpr: Foo
        // }
        if (featureNames.contains(featureNameAndDef._2) && (featureNameAndDef._1 != featureNameAndDef._2)) {
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_ERROR,
            s"Feature ${featureNameAndDef._1} should not be defined as ${featureNameAndDef._2}, " +
              s"as there's another feature named ${featureNameAndDef._2} already, thus ambiguous.")
        }
        (featureNameAndDef._1, expr(featureNameAndDef._2))
      })
      .toSeq
  }

  /**
   * This is used in experimental-tensor-mode.
   * We cannot use the getFeatures() API in SimpleAnchorExtractorSpark, because the parameter 'expectedColSchemas'
   * in that API is Map[String, StructType], while in FDS format, the type is Map[String, DataType].

   * @param dataFrameWithKeyColumns source feature data
   * @param expectedColSchemas expected schemas for each requested features
   * @return A map from (feature name, feature column) pair to its FeatureColumnFormat.
   */
  def getTensorFeatures(dataFrameWithKeyColumns: DataFrame, expectedColSchemas: Map[String, DataType]): Map[(String, Column), FeatureColumnFormat] = {
    import org.apache.spark.sql.functions._
    columnNameToFeatureDefs.collect {
      case (featureName, featureDef) if (expectedColSchemas.keySet.contains(featureName)) =>
        val schema = expectedColSchemas(featureName)
        val (rewrittenDef, featureColumnFormat) = if (featureDef.contains(userFacingAvroTensorToFDSTensorUDFName)) {
          // If the feature definition contains avroTensorToFDSTensor UDF then the feature column is already in tensor format.
          // So we set the column format for this feature as FDS_TENSOR.
          (handleAvroTensorFDSConversionUDF(schema, featureDef), FDS_TENSOR)
        } else if (featureDef.contains(FeatureTransformation.USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME)) {
          // If the feature definition contains USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME then the feature column is already in tensor format.
          // So we strip the udf name and return only the feature name.
          (FeatureTransformation.parseMultiDimTensorExpr(featureDef), FDS_TENSOR)
        } else {
          // Else, the spark sql transformation is expected to work on RAW format.
          (featureDef, RAW)
        }
        ((featureName, expr(rewrittenDef)), featureColumnFormat)
    }
  }

  /**
   * Handle the 'avroTensorToFDSTensor' UDF in the feature definition in Spark SQL expression. The UDF 'avroTensorToFDSTensor'
   * we provide to end users is a simple way to convert their existing Avro Tensor to FDS tensor that Frame uses internally.
   * The implementation here will rewrite the feature definition by renaming 'avroTensorToFDSTensor' to appropriate SQL UDFs
   * according the FDS column type.
   * The rewrite/rename is needed because the return type of Spark SQL UDF is not allowed to be 'Any', while our FDS column
   * has to be 'Any' due to different feature types.
   * By renaming it internally, users will be able to use a single user-facing conversion UDF 'avroTensorToFDSTensor', instead
   * of calling different UDFs which is error-prone.
   *
   * @param schema            the FDS feature column schema for the current feature
   * @param featureDefinition the Spark SQL definition of the feature
   * @return the rewritten feature definition in Spark SQL expression
   */
  private def handleAvroTensorFDSConversionUDF(schema: DataType, featureDefinition: String): String = {
    lazy val ss = SparkSession.builder().getOrCreate()
    val (internalAvroTensorToFDSTensorUDFName, avroTensorToFDSTensorUDF) = schema match {
      case _: IntegerType =>
        ("avroTensorToFDSTensorAsInt", getAvroTensorToFDSTensorUDF[Int](schema))
      case _: LongType =>
        ("avroTensorToFDSTensorAsLong", getAvroTensorToFDSTensorUDF[Long](schema))
      case _: FloatType =>
        ("avroTensorToFDSTensorAsFloat", getAvroTensorToFDSTensorUDF[Float](schema))
      case _: DoubleType =>
        ("avroTensorToFDSTensorAsDouble", getAvroTensorToFDSTensorUDF[Double](schema))
      case _: BooleanType =>
        ("avroTensorToFDSTensorAsBoolean", getAvroTensorToFDSTensorUDF[Boolean](schema))
      case _: StringType =>
        ("avroTensorToFDSTensorAsString", getAvroTensorToFDSTensorUDF[String](schema))
      case _: StructType =>
        ("avroTensorToFDSTensor", getAvroTensorToFDSTensorUDF[Row](schema))
      case dataType =>
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Cannot handle ${dataType} in handleAvroTensorFDSConversionUDF")
    }
    ss.udf.register(internalAvroTensorToFDSTensorUDFName, avroTensorToFDSTensorUDF)
    // rewrite the feature definition by replacing the user-facing udf 'avroTensorToFDSTensor' with the proper internal UDF name
    featureDefinition.replaceAll(userFacingAvroTensorToFDSTensorUDFName, internalAvroTensorToFDSTensorUDFName)
  }

  /**
   * Return a UDF that convert a row to the expected FDS column type
   *
   * @param schema the expected FDS column schema, could be primitive datatype or a struct, e.g. IntType, StructType
   * @tparam T the scala datatype of the output, e.g. Int, Row.
   * @return the conversion UDF
   */
  private def getAvroTensorToFDSTensorUDF[T](schema: DataType): UserDefinedFunction = {
    def avroTensorRowToFDSTensorRow[T](inputRow: GenericRowWithSchema): T = {
      if (inputRow != null) {
        val row = inputRow.asInstanceOf[Row]
        val tensorData = FeaturizedDatasetUtils.parseAvroTensorToTensorData(row).asInstanceOf[LOLTensorData]
        FeaturizedDatasetUtils.tensorToDataFrameRow(tensorData).asInstanceOf[T]
      } else null.asInstanceOf[T]
    }

    udf((x: GenericRowWithSchema) => avroTensorRowToFDSTensorRow[T](x), schema)
  }
}
